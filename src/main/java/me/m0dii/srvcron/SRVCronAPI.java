package me.m0dii.srvcron;

import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.job.EventJobContext;
import me.m0dii.srvcron.managers.GenericEventDefinition;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.EventPropertyResolver;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class SRVCronAPI {
    private final SRVCron plugin;

    public SRVCronAPI(SRVCron plugin) {
        this.plugin = plugin;
    }

    public CronJob getCronJob(String name) {
        return plugin.getJobs().getOrDefault(name, null);
    }

    public void runCronJob(CronJob job) {
        job.startJob();
    }

    public void stopCronJob(CronJob job) {
        job.stopJob();
    }

    public void runCronJobCommands(CronJob job) {
        job.runCommands();
    }

    public List<EventJob> getEventJobsByType(EventType type) {
        return plugin.getEventJobs().getOrDefault(type, null);
    }

    public List<EventJob> getEventJobsByClass(Class<? extends Event> eventClass) {
        return plugin.getGenericEventJobs().values().stream()
                .flatMap(List::stream)
                .filter(job -> eventClass.equals(job.getEventClass()))
                .toList();
    }

    public List<EventJob> getEventJobsByIdentifier(String eventIdentifier) {
        return plugin.getEventJobsByIdentifier(eventIdentifier);
    }

    public EventJob getEventJobByName(String jobName) {
        return plugin.getAllEventJobs().stream()
                .filter(job -> job.getName().equals(jobName))
                .findFirst().orElse(null);
    }

    public void runEventJob(EventJob job, Player player, World world) {
        GenericEventDefinition definition = plugin.getGenericEventDefinitions().get(job.getEventIdentifier());
        EventJobContext context;
        if (definition != null) {
            context = EventPropertyResolver.capture(null, definition.getPlayerPath(), definition.getWorldPath(),
                    definition.getPlaceholderPaths(), job.getCommands());
            context = new EventJobContext(null,
                    player == null ? context.getPlayer() : player,
                    world == null ? context.getWorld() : world,
                    mergeManualContextValues(context, player, world),
                    context.getComponentPlaceholders());
        } else {
            context = EventPropertyResolver.captureLegacy(null, player, world, java.util.Map.of());
        }
        job.performJob(context);
    }

    public void runEventJob(EventJob job, Event event, Player player, World world) {
        GenericEventDefinition definition = plugin.getGenericEventDefinitions().get(job.getEventIdentifier());
        if (definition == null) {
            runEventJob(job, player, world);
            return;
        }
        if (event != null && !definition.getEventClass().isInstance(event)) {
            throw new IllegalArgumentException("Event must be an instance of " + definition.getEventClass().getName());
        }

        EventJobContext context = EventPropertyResolver.capture(event, definition.getPlayerPath(), definition.getWorldPath(),
                definition.getPlaceholderPaths(), job.getCommands());
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>(context.getPlaceholders());
        if (player != null) {
            values.put("player_name", player.getName());
        }
        if (world != null) {
            values.put("world_name", world.getName());
        }
        job.performJob(new EventJobContext(event,
                player == null ? context.getPlayer() : player,
                world == null ? context.getWorld() : world,
                values,
                context.getComponentPlaceholders()));
    }

    private java.util.Map<String, String> mergeManualContextValues(EventJobContext context, Player player, World world) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>(context.getPlaceholders());
        if (player != null) {
            values.put("player_name", player.getName());
        }
        if (world != null) {
            values.put("world_name", world.getName());
        }
        return values;
    }
}
