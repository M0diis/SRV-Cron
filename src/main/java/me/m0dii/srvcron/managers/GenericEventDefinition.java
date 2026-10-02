package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.job.EventJob;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.List;
import java.util.Map;

/** Configuration for one dynamically registered Bukkit/Paper event class. */
public final class GenericEventDefinition {
    private final String id;
    private final Class<? extends Event> eventClass;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final String playerPath;
    private final String worldPath;
    private final Map<String, String> placeholderPaths;
    private final List<EventJob> jobs;

    public GenericEventDefinition(
            String id,
            Class<? extends Event> eventClass,
            EventPriority priority,
            boolean ignoreCancelled,
            String playerPath,
            String worldPath,
            Map<String, String> placeholderPaths,
            List<EventJob> jobs
    ) {
        this.id = id;
        this.eventClass = eventClass;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
        this.playerPath = playerPath;
        this.worldPath = worldPath;
        this.placeholderPaths = Map.copyOf(placeholderPaths);
        this.jobs = List.copyOf(jobs);
    }

    public String getId() {
        return id;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public boolean isIgnoreCancelled() {
        return ignoreCancelled;
    }

    public String getPlayerPath() {
        return playerPath;
    }

    public String getWorldPath() {
        return worldPath;
    }

    public Map<String, String> getPlaceholderPaths() {
        return placeholderPaths;
    }

    public List<EventJob> getJobs() {
        return jobs;
    }
}
