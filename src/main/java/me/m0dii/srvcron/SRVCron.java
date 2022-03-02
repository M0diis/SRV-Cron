package me.m0dii.srvcron;

import me.m0dii.srvcron.commands.CronCommand;
import me.m0dii.srvcron.commands.TimerCommand;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.EventManager;
import me.m0dii.srvcron.managers.StartupCommandDispatchEvent;
import me.m0dii.srvcron.utils.EventType;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
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
        
        log("Loading config...");
        
        prepareConfig();
        saveConfig();
    
        log("Finished loading configuration.");
        
        log("Loading commands...");
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("srvcron").setExecutor(new CronCommand(this));
    
        log("Finished loading server commands.");
        
        loadJobs();

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
    }
    
    private void setupMetrics()
    {
        Metrics metrics = new Metrics(this, 14498);
        
        CustomChart c = new MultiLineChart("players_and_servers", () ->
        {
            Map<String, Integer> valueMap = new HashMap<>();
            
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            
            return valueMap;
        });
        
        metrics.addCustomChart(c);
    
        log("Loading custom charts for metrics...");
    
        metrics.addCustomChart(new SingleLineChart("running_jobs", () -> getJobs().size()));

        metrics.addCustomChart(new SingleLineChart("running_event_jobs", () ->
            Arrays.stream(EventType.values()).filter(type -> getEventJobs().containsKey(type)).mapToInt(type -> getEventJobs().get(type).size()).sum()
        ));
    
        metrics.addCustomChart(new SingleLineChart("running_startup_commands", () -> getStartupCommands().size()));
    
        log("Custom charts have been loaded.");
    }
    
    public void loadJobs()
    {
        log("Loading cron jobs....");
        
        for(String s : getConfig().getConfigurationSection("jobs").getKeys(false))
        {
            List<String> cmds = getConfig().getStringList("jobs." + s + ".commands");
            String time = getConfig().getString("jobs." + s + ".time");
            
            jobs.put(s, new CronJob(this, cmds, time, s));
            log("Created new job: " + s);
        }
        
        log("Successfully loaded jobs " + (jobs.size() == 1 ? "job" : "jobs") + ".");
        log("Starting cron jobs...");
        
        for(CronJob j : new ArrayList<>(jobs.values()))
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

        for(String s : getConfig().getConfigurationSection("event-jobs").getKeys(false))
        {
            EventType type = EventType.isEventJob(s);
            
            if(type != null)
            {
                List<EventJob> jobs = new ArrayList<>();
                
                for(String name : getConfig().getConfigurationSection("event-jobs." + s).getKeys(false))
                {
                    int time = getConfig().getInt("event-jobs." + s + "." + name + ".time");
                    List<String> cmds = getConfig().getStringList("event-jobs." + s + "." + name + ".commands");
                    jobs.add(new EventJob(this, name, time, cmds, type));
                    
                    log("Created new event job: " + name + " (" + type.getConfigName() + ")");
                }

                eventJobs.put(type, jobs);
            }
        }
        
        log("Event jobs have been registered.");

        List<String> cmds = getConfig().getStringList("startup.commands");
        
        if(cmds != null && !cmds.isEmpty())
        {
            for (String command : cmds)
            {
                startUpCommands.add(command);
                
                log("Created new startup command: " + command);
            }
        }
        
        log("Startup commands have been registered.");
    }
    
    @Override
    public void onDisable()
    {
        //reloadConfig();
        //saveConfig();
    }
    
    public void log(String info)
    {
        getLogger().info(info);
        logToFile(info);
    }
    
    private void logToFile(String info)
    {
        try
        {
            File dataFolder = getDataFolder();
            
            if(!dataFolder.exists())
            {
                dataFolder.mkdir();
            }
            
            File saveTo = new File(getDataFolder(), "log.txt");
            
            if (!saveTo.exists())
            {
                saveTo.createNewFile();
            }
            
            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);
            
            pw.println("[" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + "] "+ info);
            pw.flush();
            pw.close();
            
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
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
            this.getConfig().options().copyDefaults(true);
            this.getConfig().save(this.configFile);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        
        YamlConfiguration.loadConfiguration(this.configFile);
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
