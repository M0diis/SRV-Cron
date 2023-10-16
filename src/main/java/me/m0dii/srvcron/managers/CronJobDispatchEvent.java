package me.m0dii.srvcron.managers;

import lombok.Getter;
import me.m0dii.srvcron.job.CronJob;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class CronJobDispatchEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    private final CronJob cronJob;

    private boolean isCancelled;

    public CronJobDispatchEvent(CronJob cronJob) {
        this.cronJob = cronJob;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public String getJobName() {
        return cronJob.getName();
    }

    public String getJobTime() {
        return cronJob.getTime();
    }

    public List<String> getJobCommands() {
        return cronJob.getCommands();
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