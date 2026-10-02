package me.m0dii.srvcron.job;

import lombok.Getter;
import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.managers.EventJobDispatchEvent;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class EventJob {
    private final SRVCron srvCron;
    @Getter
    private final String name;
    private final int time;
    @Getter
    private final List<String> commands;
    @Nullable
    private final EventType eventType;
    @Getter
    private final String eventIdentifier;
    @Getter
    private final Class<? extends Event> eventClass;

    @Getter
    private boolean suspended = false;
    @Getter
    private int runCount;

    public EventJob(SRVCron srvCron, String name, int time, List<String> commands, EventType eventType) {
        this.srvCron = srvCron;
        this.name = name;
        this.time = time;
        this.commands = commands;
        this.eventType = eventType;
        this.eventIdentifier = eventType.getConfigName();
        this.eventClass = null;
    }

    public EventJob(SRVCron srvCron, String name, int time, List<String> commands, String eventIdentifier,
                    Class<? extends Event> eventClass) {
        this.srvCron = srvCron;
        this.name = name;
        this.time = time;
        this.commands = commands;
        this.eventType = null;
        this.eventIdentifier = eventIdentifier;
        this.eventClass = eventClass;
    }

    public void performJob(Player player) {
        performJob(player, null, null);
    }

    public void performJob(World world) {
        performJob(null, world, null);
    }

    public void performJob(Player player, World world, Event event, List<String> commands) {
        performJob(new EventJobContext(event, player, world, Map.of()), commands);
    }

    public void performJob(EventJobContext context) {
        performJob(context, commands);
    }

    private void performJob(EventJobContext context, List<String> commands) {
        if (eventType == EventType.JOIN_EVENT && context.getPlayer() != null && !context.getPlayer().isOnline()) {
            return;
        }

        if (this.suspended) {
            this.srvCron.log("Job " + EventJob.this.name + " is suspended, skipping...");

            return;
        }

        if (time == 0) {
            Runnable eventDispatchTask = () -> Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(EventJob.this, context, commands));

            if (Bukkit.isPrimaryThread()) {
                eventDispatchTask.run();
            } else {
                Bukkit.getScheduler().runTask(srvCron, eventDispatchTask);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(EventJob.this, context, commands));
                }
            }.runTaskLater(srvCron, time * 20L);
        }
    }

    public void performJob(Player player, World world, Event event) {
        performJob(player, world, event, commands);
    }

    public void increaseRunCount() {
        runCount++;
    }

    /** Returns the legacy event enum, or {@code null} for a generic event job. */
    @Nullable
    public EventType getEventType() {
        return eventType;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        suspended = false;
    }

}
