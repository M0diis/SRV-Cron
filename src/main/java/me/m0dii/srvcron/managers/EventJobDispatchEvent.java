package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.job.EventJob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EventJobDispatchEvent extends Event
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final EventJob eventJob;
    
    public EventJobDispatchEvent(EventJob eventJob)
    {
        this.eventJob = eventJob;
    }

    public EventJob getEventJob()
    {
        return eventJob;
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
        return eventJob.getName();
    }
    
    public String getJobConfigName()
    {
        return eventJob.getEventType().getConfigName();
    }
}