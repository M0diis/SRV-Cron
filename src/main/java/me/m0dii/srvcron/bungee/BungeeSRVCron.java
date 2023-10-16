package me.m0dii.srvcron.bungee;

import lombok.Getter;
import me.m0dii.srvcron.bungee.commands.BungeeCronCommand;
import me.m0dii.srvcron.bungee.commands.BungeeTimerCommand;
import me.m0dii.srvcron.bungee.job.BungeeCronJob;
import me.m0dii.srvcron.bungee.job.BungeeEventJob;
import me.m0dii.srvcron.bungee.managers.EventManager;
import me.m0dii.srvcron.utils.EventType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BungeeSRVCron extends Plugin {
    @Getter
    private final Map<String, BungeeCronJob> jobs = new HashMap<>();
    @Getter
    private final Map<EventType, List<BungeeEventJob>> eventJobs = new HashMap<>();
    @Getter
    private final List<String> startUpCommands = new ArrayList<>();

    private Configuration config;
    private File file;

    @Override
    public void onEnable() {
        log("Loading plugin...");
        log("Loading config...");

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        try {
            this.file = new File(getDataFolder(), "config.yml");

            if (!this.file.exists()) {
                log("Error: config.yml Not Found! Creating a new");
                copy(this.file, "config.yml");
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);

            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log("Loading commands...");
        getProxy().getPluginManager().registerCommand(this, new BungeeTimerCommand(this));
        getProxy().getPluginManager().registerCommand(this, new BungeeCronCommand(this));

        loadJobs();

        log("Loading managers...");
        new EventManager(this);

        log("Loading metrics...");
        BungeeMetrics metrics = new BungeeMetrics(this);

        log("Loading custom chart for metrics...");

        metrics.addCustomChart(new BungeeMetrics.SingleLineChart("running_jobs", jobs::size));

        metrics.addCustomChart(new BungeeMetrics.SingleLineChart("running_event_jobs", () ->
        {
            int size = 0;

            for (EventType type : EventType.values()) {
                if (getEventJobs().containsKey(type)) {
                    size += getEventJobs().get(type).size();
                }
            }
            return size;
        }));

        metrics.addCustomChart(new BungeeMetrics.SingleLineChart("running_startup_commands", () -> getStartUpCommands().size()));

        log("Everything loaded!");

        ProxyServer.getInstance().getScheduler().schedule(this, () ->
        {
            log("Dispatching startup commands..");

            for (String c : getStartUpCommands()) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(
                        ProxyServer.getInstance().getConsole(), c);
            }

            log("Commands dispatched!");
        }, 2, TimeUnit.SECONDS);
    }

    public void loadJobs() {
        log("Loading cron jobs....");

        for (String s : config.getSection("jobs").getKeys()) {
            List<String> cmds = config.getStringList("jobs." + s + ".commands");
            String time = config.getString("jobs." + s + ".time");

            jobs.put(s, new BungeeCronJob(this, cmds, time, s));
            log("Created new job: " + s);
        }

        log("Total loaded jobs: " + jobs.size());
        log("Starting cron jobs...");

        for (BungeeCronJob j : new ArrayList<>(jobs.values())) {
            try {
                log("Starting job: " + j.getName());
                j.startJob();
            } catch (IllegalArgumentException ex) {
                log("Can't start job " + j.getName() + "! " + ex.getMessage());
            }
        }

        log("All jobs started!");

        for (String s : config.getSection("event-jobs").getKeys()) {
            EventType type = EventType.isEventJob(s);

            if (type != null) {
                List<BungeeEventJob> jobs = new ArrayList<>();

                for (String name : config.getSection("event-jobs." + s).getKeys()) {
                    int time = config.getInt("event-jobs." + s + "." + name + ".time");
                    List<String> cmds = config.getStringList("event-jobs." + s + "." + name + ".commands");
                    jobs.add(new BungeeEventJob(this, name, time, cmds, type));

                    log("Created new event job: " + name + " (" + type.getConfigName() + ")");
                }

                eventJobs.put(type, jobs);
            }
        }
        log("All event jobs registered!");

        List<String> cmds = config.getStringList("startup.commands");

        if (cmds != null) {
            for (String command : cmds) {
                startUpCommands.add(command);
                log("Created new startup command: " + command);
            }
        }

        log("All startup commands registered!");
    }

    @Override
    public void onDisable() {
        //reloadConfig();
        //saveConfig();
    }

    public void log(String info) {
        getLogger().info(info);
        logCustom(info);
    }

    private void logCustom(String info) {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File saveTo = new File(getDataFolder(), "log.txt");
            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println("[" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + "] " + info);
            pw.flush();
            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void copy(File file, String resource) {
        try {
            Files.copy(getResourceAsStream(resource), file.toPath());
        } catch (IOException ex) {
            ex.printStackTrace();

            getLogger().warning("Could not copy " + resource + " file");
        }
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
