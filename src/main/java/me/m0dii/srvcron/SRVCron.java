package me.m0dii.srvcron;

import me.m0dii.srvcron.commands.CronCommand;
import me.m0dii.srvcron.commands.TimerCommand;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.EventManager;
import me.m0dii.srvcron.managers.StartupCommandDispatchEvent;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.UpdateChecker;
import me.m0dii.srvcron.utils.Utils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class SRVCron extends JavaPlugin
{
    private final HashMap<String, CronJob> jobs = new HashMap<>();
    private final HashMap<EventType, List<EventJob>> eventJobs = new HashMap<>();
    private final List<String> startUpCommands = new ArrayList<>();
    
    private static SRVCron instance;
    
    public static SRVCron getInstance()
    {
        return instance;
    }
    
    private final SRVCronAPI api = new SRVCronAPI(this);
    
    public SRVCronAPI getAPI()
    {
        return api;
    }
    
    private File configFile;
                                            
    @Override
    public void onEnable()
    {
        instance = this;
        
        log("Loading SRV-Cron...");
        
        log("Loading configuration...");
        prepareConfig();
        saveConfig();
        log("Finished loading configuration.");
        
        log("Loading commands...");
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("srvcron").setExecutor(new CronCommand(this));
        log("Finished loading commands.");
        
        log("Loading jobs...");
        loadJobs();
        log("Finished loading jobs.");

        log("Creating Event Managers...");
        new EventManager(this);
        log("Finished loading Event Managers.");
    
        log("Loading metrics...");
        setupMetrics();
        log("Finished loading metrics.");
        
        log("SRV-Cron has been loaded successfully.");
    
        log("Running startup commands...");
        Bukkit.getPluginManager().callEvent(new StartupCommandDispatchEvent(this));
        log("Startup commands dispatched.");
        
        log("Checking for updates...");
        checkForUpdates();
        log("Finished checking for updates.");
    }
    
    private void checkForUpdates()
    {
        new UpdateChecker(this, 100382).getVersion(ver ->
        {
            if (!this.getDescription().getVersion().equalsIgnoreCase(ver))
            {
                log("You are running an outdated version of SRV-Cron.");
                log("You can download the latest version on Spigot:");
                log("https://www.spigotmc.org/resources/100382/");
            }
        });
    }
    
    private void setupMetrics()
    {
        Metrics metrics = new Metrics(this, 14503);
        
        log("Loading custom charts for metrics...");
    
        metrics.addCustomChart(new SingleLineChart("running_jobs", jobs::size));

        metrics.addCustomChart(new SingleLineChart("running_event_jobs", () ->
            Arrays.stream(EventType.values()).filter(eventJobs::containsKey).mapToInt(type -> eventJobs.get(type).size()).sum()
        ));
    
        metrics.addCustomChart(new SingleLineChart("running_startup_commands", startUpCommands::size));
    
        log("Custom charts have been loaded.");
    }
    
    public void loadJobs()
    {
        log("Loading cron jobs....");
    
        ConfigurationSection jobsSection = getConfig().getConfigurationSection("jobs");
    
        if(jobsSection != null)
        {
            for(String s : jobsSection.getKeys(false))
            {
                List<String> cmds = getConfig().getStringList("jobs." + s + ".commands");
                String time = getConfig().getString("jobs." + s + ".time");
        
                this.jobs.put(s, new CronJob(this, cmds, time, s));
                log("Created new job: " + s);
            }
    
            log("Successfully loaded jobs " + (this.jobs.size() == 1 ? "job" : "jobs") + ".");
        }
        else
        {
            log("Configuration section with jobs was not found.");
        }
    
        log("Starting cron jobs...");
        
        for(CronJob j : new ArrayList<>(this.jobs.values()))
        {
            try
            {
                log("Starting job: " + j.getName());
                j.startJob();
            }
            catch(IllegalArgumentException ex)
            {
                log("Failed to start job " + j.getName() + ": " + ex.getMessage());
            }
        }
        
        log("Jobs have been started.");
    
        ConfigurationSection eventJobSection = getConfig().getConfigurationSection("event-jobs");
        
        if(eventJobSection != null)
        {
            for(String s : eventJobSection.getKeys(false))
            {
                EventType type = EventType.isEventJob(s);
        
                if(type != null)
                {
                    List<EventJob> jobs = new ArrayList<>();
    
                    ConfigurationSection eventJobsSection = getConfig().getConfigurationSection("event-jobs." + s);
                    
                    if(eventJobsSection != null)
                    {
                        for(String name : eventJobsSection.getKeys(false))
                        {
                            int time = getConfig().getInt("event-jobs." + s + "." + name + ".time");
                            List<String> cmds = getConfig().getStringList("event-jobs." + s + "." + name + ".commands");
                            jobs.add(new EventJob(this, name, time, cmds, type));
        
                            log("Created new event job: " + name + " (" + type.getConfigName() + ")");
                        }
    
                        eventJobs.put(type, jobs);
                    }
                    else
                    {
                        log("Configuration section for jobs in event job '" + s + "' was not found.");
                    }
                }
            }
    
            log("Event jobs have been registered.");
        }
        else
        {
            log("Configuration section with event jobs was not found.");
        }
        
        List<String> cmds = getConfig().getStringList("startup.commands");
    
        if(cmds == null || cmds.isEmpty())
        {
            log("Configuration section with startup commands was not found.");
        }
        else
        {
            for (String command : cmds)
            {
                startUpCommands.add(command);
                
                log("Created new startup command: " + command);
            }
    
            log("Startup commands have been registered.");
        }
    }
    
    @Override
    public void onDisable()
    {
//        Map<String, List<String>> messagesByFile = Utils.messagesByFile;
//
//        for(String file : messagesByFile.keySet())
//        {
//            List<String> messages = messagesByFile.get(file);
//
//            for(String m : messages)
//            {
//                Utils.logToFile(file, m);
//            }
//        }
    }
    
    public void log(String msg)
    {
        getLogger().info(msg);
        Utils.logToFile("log.txt", msg);
    }
    
    private void prepareConfig()
    {
        this.configFile = new File(this.getDataFolder(), "config.yml");
        
        if(!this.configFile.exists())
        {
            this.configFile.getParentFile().mkdirs();
            
            this.copy(this.getResource("config.yml"), this.configFile);
        }
        
        try
        {
            this.getConfig().save(this.configFile);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        
        YamlConfiguration.loadConfiguration(this.configFile);
    
        this.copy(getResource("config.yml_backup"), new File(this.getDataFolder(), "config.yml_backup"));
    }
    
    private void copy(InputStream in, File file)
    {
        if(in != null)
        {
            try
            {
                OutputStream out = new FileOutputStream(file);
                
                byte[] buf = new byte[1024];
                
                int len;
                
                while((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                
                out.close();
                in.close();
            }
            catch(Exception ex)
            {
                log("Error copying resource: " + file.getAbsolutePath());
                
                ex.printStackTrace();
            }
        }
    }

    public HashMap<String, CronJob> getJobs()
    {
        return jobs;
    }

    public HashMap<EventType, List<EventJob>> getEventJobs()
    {
        return eventJobs;
    }

    public List<String> getStartupCommands()
    {
        return startUpCommands;
    }
}
