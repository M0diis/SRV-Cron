package me.m0dii.srvcron;

import lombok.Getter;
import me.m0dii.srvcron.commands.CronCommand;
import me.m0dii.srvcron.commands.TimerCommand;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.EventManager;
import me.m0dii.srvcron.managers.GenericEventDefinition;
import me.m0dii.srvcron.managers.StartupCommandDispatchEvent;
import me.m0dii.srvcron.utils.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class SRVCron extends JavaPlugin {
    @Getter
    private final Map<String, CronJob> jobs = new HashMap<>();
    @Getter
    private final Map<EventType, List<EventJob>> eventJobs = new HashMap<>();
    @Getter
    private final Map<String, List<EventJob>> genericEventJobs = new LinkedHashMap<>();
    @Getter
    private final Map<String, GenericEventDefinition> genericEventDefinitions = new LinkedHashMap<>();
    @Getter
    private final List<String> startUpCommands = new ArrayList<>();

    @Getter
    private static SRVCron instance;

    @Getter
    private final SRVCronAPI api = new SRVCronAPI(this);

    @Getter
    private LangConfig langCfg;

    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        log("Loading SRV-Cron...");

        log("Loading configuration...");

        prepareConfig();
        saveConfig();

        this.langCfg = new LangConfig(this, getConfig().getString("locale"));
        applyScheduleSettings();

        log("Finished loading configuration.");

        logStartup("Loading commands...");

        Optional.ofNullable(getCommand("timer"))
                .ifPresent(command -> command.setExecutor(new TimerCommand(this)));

        Optional.ofNullable(getCommand("srvcron"))
                .ifPresent(command -> command.setExecutor(new CronCommand(this)));

        logStartup("Finished loading commands.");

        logStartup("Loading jobs...");
        loadJobs();
        logStartup("Finished loading jobs.");

        logStartup("Creating Event Managers...");
        eventManager = new EventManager(this);
        logStartup("Finished loading Event Managers.");

        logStartup("Loading metrics...");
        setupMetrics();
        logStartup("Finished loading metrics.");

        logStartup("SRV-Cron has been loaded successfully.");

        logStartup("Running startup commands...");
        Bukkit.getPluginManager().callEvent(new StartupCommandDispatchEvent(this));
        logStartup("Startup commands dispatched.");

        checkForUpdates();
    }

    private void checkForUpdates() {
        logStartup("Checking for updates...");

        if (!getConfig().getBoolean("notify-update")) {
            logStartup("Update checking disabled, skipping.");

            return;
        }

        new UpdateChecker(this, 100382).getVersion(ver ->
        {
            if (!this.getPluginMeta().getVersion().equalsIgnoreCase(ver)) {
                log("You are running an outdated version of SRV-Cron.");
                log("You are using: " + getPluginMeta().getVersion() + ".");
                log("Latest version: " + ver + ".");
                log("You can download the latest version on Spigot:");
                log("https://www.spigotmc.org/resources/100382/");
            }
        });

        logStartup("Finished checking for updates.");
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 14503);

        logStartup("Loading custom charts for metrics...");

        metrics.addCustomChart(new SingleLineChart("running_jobs", jobs::size));

        metrics.addCustomChart(new SingleLineChart("running_event_jobs", () ->
                getAllEventJobs().size()
        ));

        metrics.addCustomChart(new SingleLineChart("running_startup_commands", startUpCommands::size));

        logStartup("Custom charts have been loaded.");
    }

    public void loadJobs() {
        applyScheduleSettings();
        logStartup("Loading cron jobs....");

        for (CronJob job : jobs.values()) {
            job.stopJob();
        }

        jobs.clear();
        eventJobs.clear();
        genericEventJobs.clear();
        genericEventDefinitions.clear();
        startUpCommands.clear();

        ConfigurationSection jobsSection = getConfig().getConfigurationSection("jobs");

        if (jobsSection != null) {
            for (String s : jobsSection.getKeys(false)) {
                List<String> cmds = getConfig().getStringList("jobs." + s + ".commands");
                String time = getConfig().getString("jobs." + s + ".time");

                jobs.put(s, new CronJob(this, cmds, time, s));

                logStartup("Created new job: " + s);
            }

            logStartup("Successfully loaded jobs " + (jobs.size() == 1 ? "job" : "jobs") + ".");
        } else {
            logStartup("Configuration section with jobs was not found.");
        }

        logStartup("Starting cron jobs...");

        for (CronJob j : new ArrayList<>(jobs.values())) {
            try {
                logStartup("Starting job: " + j.getName());
                j.startJob();
            } catch (IllegalArgumentException ex) {
                log("Failed to start job " + j.getName() + ": " + ex.getMessage());
            }
        }

        logStartup("Jobs have been started.");

        ConfigurationSection eventJobSection = getConfig().getConfigurationSection("event-jobs");

        if (eventJobSection != null) {
            for (String s : eventJobSection.getKeys(false)) {
                EventType type = EventType.isEventJob(s);

                if (type != null) {
                    List<EventJob> jobs = new ArrayList<>();

                    ConfigurationSection eventJobsSection = getConfig().getConfigurationSection("event-jobs." + s);

                    if (eventJobsSection != null) {
                        for (String name : eventJobsSection.getKeys(false)) {
                            int time = getConfig().getInt("event-jobs." + s + "." + name + ".time");
                            List<String> cmds = getConfig().getStringList("event-jobs." + s + "." + name + ".commands");
                            jobs.add(new EventJob(this, name, time, cmds, type));

                            logStartup("Created new event job: " + name + " (" + type.getConfigName() + ")");
                        }

                        eventJobs.put(type, jobs);
                    } else {
                        log("Configuration section for jobs in event job '" + s + "' was not found.");
                    }
                }
            }

            logStartup("Event jobs have been registered.");
        } else {
            log("Configuration section with event jobs was not found.");
        }

        loadGenericEventJobs();

        List<String> cmds = getConfig().getStringList("startup.commands");

        if (cmds == null || cmds.isEmpty()) {
            log("Configuration section with startup commands was not found.");
        } else {
            for (String command : cmds) {
                startUpCommands.add(command);

                logStartup("Created new startup command: " + command);
            }

            logStartup("Startup commands have been registered.");
        }

        if (eventManager != null) {
            eventManager.refreshEventRegistrations();
        }
    }

    private void loadGenericEventJobs() {
        ConfigurationSection definitions = getConfig().getConfigurationSection("generic-event-jobs");
        if (definitions == null) {
            return;
        }

        for (String id : definitions.getKeys(false)) {
            String path = "generic-event-jobs." + id;
            String className = getConfig().getString(path + ".event", "").trim();
            Class<? extends Event> eventClass = resolveEventClass(className);
            if (eventClass == null) {
                log("Skipping generic event '" + id + "': event class '" + className + "' could not be resolved or is not a supported Bukkit event.");
                continue;
            }

            EventPriority priority;
            try {
                priority = EventPriority.valueOf(getConfig().getString(path + ".priority", "NORMAL").trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log("Skipping generic event '" + id + "': invalid priority '" + getConfig().getString(path + ".priority") + "'.");
                continue;
            }

            Map<String, String> placeholderPaths = new LinkedHashMap<>();
            ConfigurationSection placeholders = getConfig().getConfigurationSection(path + ".placeholders");
            if (placeholders != null) {
                for (String alias : placeholders.getKeys(false)) {
                    String propertyPath = placeholders.getString(alias, "").trim();
                    if (!alias.matches("[A-Za-z0-9_-]+") || alias.equalsIgnoreCase("player_name")
                            || alias.equalsIgnoreCase("world_name") || alias.equalsIgnoreCase("event")) {
                        log("Ignoring invalid or reserved event placeholder alias '" + alias + "' in '" + id + "'.");
                        continue;
                    }
                    if (!propertyPath.isBlank()) {
                        placeholderPaths.put(alias, propertyPath);
                    }
                }
            }

            ConfigurationSection configuredJobs = getConfig().getConfigurationSection(path + ".jobs");
            if (configuredJobs == null) {
                log("Skipping generic event '" + id + "': missing jobs section.");
                continue;
            }

            List<EventJob> loadedJobs = new ArrayList<>();
            for (String name : configuredJobs.getKeys(false)) {
                String jobPath = path + ".jobs." + name;
                int time = getConfig().getInt(jobPath + ".time", 0);
                List<String> commands = getConfig().getStringList(jobPath + ".commands");
                EventJob job = new EventJob(this, name, time, commands, id, eventClass);
                loadedJobs.add(job);
                logStartup("Created generic event job: " + name + " (" + id + ": " + className + ")");
            }

            String playerPath = getConfig().getString(path + ".context.player", "");
            String worldPath = getConfig().getString(path + ".context.world", "");
            GenericEventDefinition definition = new GenericEventDefinition(
                    id,
                    eventClass,
                    priority,
                    getConfig().getBoolean(path + ".ignore-cancelled", false),
                    playerPath,
                    worldPath,
                    placeholderPaths,
                    loadedJobs
            );
            genericEventDefinitions.put(id, definition);
            genericEventJobs.put(id, loadedJobs);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> resolveEventClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }

        Set<ClassLoader> classLoaders = new LinkedHashSet<>();
        classLoaders.add(getClass().getClassLoader());
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            classLoaders.add(plugin.getClass().getClassLoader());
        }

        for (ClassLoader classLoader : classLoaders) {
            try {
                Class<?> candidate = Class.forName(className, false, classLoader);
                if (!Event.class.isAssignableFrom(candidate)) {
                    continue;
                }
                var handlerList = candidate.getDeclaredMethod("getHandlerList");
                if (!java.lang.reflect.Modifier.isStatic(handlerList.getModifiers())
                        || !HandlerList.class.isAssignableFrom(handlerList.getReturnType())) {
                    continue;
                }
                var handlers = candidate.getMethod("getHandlers");
                if (java.lang.reflect.Modifier.isStatic(handlers.getModifiers())
                        || !HandlerList.class.isAssignableFrom(handlers.getReturnType())) {
                    continue;
                }
                return (Class<? extends Event>) candidate;
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError | SecurityException ignored) {
                // Try the classloader belonging to another enabled plugin.
            }
        }
        return null;
    }

    public List<EventJob> getAllEventJobs() {
        List<EventJob> allJobs = new ArrayList<>();
        eventJobs.values().forEach(allJobs::addAll);
        genericEventJobs.values().forEach(allJobs::addAll);
        return allJobs;
    }

    public List<EventJob> getEventJobsByIdentifier(String identifier) {
        List<EventJob> genericJobs = genericEventJobs.get(identifier);
        if (genericJobs != null) {
            return genericJobs;
        }
        EventType legacyType = EventType.isEventJob(identifier);
        if (legacyType != null) {
            return eventJobs.getOrDefault(legacyType, List.of());
        }
        return List.of();
    }

    private void applyScheduleSettings() {
        String configured = getConfig().getString("schedule.weekday-numbering", "monday-first");
        boolean valid = TimeExpression.configureWeekdayNumbering(configured);
        if (!valid) {
            log("Invalid value for schedule.weekday-numbering: '" + configured + "'. Falling back to 'monday-first'.");
        }
    }

    @Override
    public void onDisable() {
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

    public void log(String msg) {
        getLogger().info(msg);

        if (getConfig().getBoolean("log-to-file")) {
            Utils.logToFile("log.txt", msg);
        }
    }

    public void logStartup(String msg) {
        if (!getConfig().getBoolean("silent-start")) {
            log(msg);
        }
    }

    private void prepareConfig() {
        File configFile = new File(this.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);

            configFile.getParentFile().mkdirs();

            this.copy(this.getResource("config.yml"), configFile);
        }

        try {
            this.getConfig().save(configFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        YamlConfiguration.loadConfiguration(configFile);

        copy(getResource("config.yml_backup"), new File(this.getDataFolder(), "config.yml_backup"));

        logStartup("Finished loading config.yml");
    }

    private void copy(InputStream in, File file) {
        if (in != null) {
            try {
                OutputStream out = new FileOutputStream(file);

                byte[] buf = new byte[1024];

                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                out.close();
                in.close();
            } catch (Exception ex) {
                log("Error copying resource: " + file.getAbsolutePath());

                ex.printStackTrace();
            }
        }
    }
}
