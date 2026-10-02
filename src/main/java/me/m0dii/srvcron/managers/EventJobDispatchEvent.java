package me.m0dii.srvcron.managers;

import lombok.Getter;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.job.EventJobContext;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class EventJobDispatchEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    private final EventJob eventJob;
    @Getter
    private final Event event;
    @Getter
    private final Player player;
    @Getter
    private final World world;
    @Getter
    private final EventJobContext context;

    private List<String> jobCommands;

    private boolean isCancelled;

    public EventJobDispatchEvent(EventJob eventJob, Event event, Player player, World world) {
        this(eventJob, new EventJobContext(event, player, world, Map.of()), eventJob.getCommands());
    }

    public EventJobDispatchEvent(EventJob eventJob, Event event, Player player, World world, List<String> commands) {
        this(eventJob, new EventJobContext(event, player, world, Map.of()), commands);
    }

    public EventJobDispatchEvent(EventJob eventJob, EventJobContext context, List<String> commands) {
        this.eventJob = eventJob;
        this.context = context;
        this.event = context.getEvent();
        this.player = context.getPlayer();
        this.world = context.getWorld();
        this.jobCommands = commands;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public String getJobName() {
        return eventJob.getName();
    }

    /** Returns the legacy event enum, or {@code null} for a generic event job. */
    @Nullable
    public EventType getEventType() {
        return eventJob.getEventType();
    }

    public String getJobConfigName() {
        return eventJob.getEventIdentifier();
    }

    public String getEventIdentifier() {
        return eventJob.getEventIdentifier();
    }

    public Class<? extends Event> getEventClass() {
        return eventJob.getEventClass();
    }

    public Map<String, String> getEventPlaceholders() {
        return context.getPlaceholders();
    }

    public List<String> getEventJobCommands() {
        return eventJob.getCommands();
    }

    public List<String> getFinalCommands() {
        return jobCommands;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}
