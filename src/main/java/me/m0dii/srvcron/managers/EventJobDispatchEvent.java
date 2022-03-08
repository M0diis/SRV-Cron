package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EventJobDispatchEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final EventJob eventJob;
    private final Event event;
    private final Player player;
    private final World world;
    
    private List<String> jobCommands = new ArrayList<>();
    
    private boolean isCancelled;
    
    public EventJobDispatchEvent(EventJob eventJob, Event event, Player player, World world)
    {
        this.eventJob = eventJob;
        this.event = event;
        
        this.player = player;
        this.world = world;
        
        this.jobCommands = eventJob.getCommands();
    }
    
    public EventJobDispatchEvent(EventJob eventJob, Event event, Player player, World world, List<String> commands)
    {
        this.eventJob = eventJob;
        this.event = event;
        
        this.player = player;
        this.world = world;
        
        this.jobCommands = commands;
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
    
    public EventType getEventType()
    {
        return eventJob.getEventType();
    }
    
    public String getJobConfigName()
    {
        return eventJob.getEventType().getConfigName();
    }

    public List<String> getEventJobCommands()
    {
        return eventJob.getCommands();
    }

    public List<String> getFinalCommands()
    {
        return jobCommands;
    }
    
    @Nullable
    public Player getPlayer()
    {
        return player;
    }
    
    @Nullable
    public World getWorld()
    {
        return world;
    }
    
    @Nullable
    public Event getEvent()
    {
        return event;
    }
    
    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel)
    {
        this.isCancelled = cancel;
    }
}