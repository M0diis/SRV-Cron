package me.m0dii.srvcron;

import me.m0dii.srvcron.commands.CronCommand;
import me.m0dii.srvcron.commands.TimerCommand;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.EventManager;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    
    private File configFile;
                                            
    @Override
    public void onEnable()
    {
        instance = this;
        
        log("Loading plugin...");
        log("Loading config...");
        
        prepareConfig();
        saveConfig();
        
        log("Loading commands...");
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("srvcron").setExecutor(new CronCommand(this));
        
        loadJobs();

        log("Loading managers...");
        new EventManager(this);
        
        //log("Loading metrics...");
        //setupMetrics();
        
        log("Everything loaded!");
        
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                log("Dispatching startup commands..");
                
                for(String cmd : getStartUpCommands())
                {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.parsePlaceholder(cmd));
                }
                
                log("Commands dispatched!");
            }
        }.runTaskLater(this, 20);
    }
    
    private void setupMetrics()
    {
        Metrics metrics = new Metrics(this, 10924);
        
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
    
        metrics.addCustomChart(new SingleLineChart("running_startup_commands", () -> getStartUpCommands().size()));
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
        
        log("Total loaded jobs: " + jobs.size());
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
                log("Can't start job " + j.getName() + "! " + ex.getMessage());
            }
        }
        
        log("All jobs started!");

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
        
        log("All event jobs registered!");

        List<String> cmds = getConfig().getStringList("startup.commands");
        
        if(cmds != null)
        {
            for (String command : cmds)
            {
                startUpCommands.add(command);
                
                log("Created new startup command: " + command);
            }
        }
        log("All startup commands registered!");
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
            //noinspection ResultOfMethodCallIgnored
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

    public List<String> getStartUpCommands()
    {
        return startUpCommands;
    }
}
