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

import java.util.List;

public class EventJobDispatchEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final EventJob eventJob;
    private final Player player;
    private final World world;
    
    private boolean isCancelled;
    
    public EventJobDispatchEvent(EventJob eventJob, Player player, World world)
    {
        this.eventJob = eventJob;
        
        this.player = player;
        this.world = world;
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

    public List<String> getJobCommands()
    {
        return eventJob.getCommands();
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