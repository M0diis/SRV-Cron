package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.job.CronJob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CronJobDispatchEvent extends Event
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final CronJob cronJob;
    
    public CronJobDispatchEvent(CronJob cronJob)
    {
        this.cronJob = cronJob;
    }

    public CronJob getCronJob()
    {
        return cronJob;
    }
    
    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }
    
    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
    
    public String getJobName()
    {
        return cronJob.getName();
    }
    
    public String getJobTime()
    {
        return cronJob.getTime();
    }
    
    public List<String> getJobCommands()
    {
        return cronJob.getCommands();
    }
}