package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class StartupCommandDispatchEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final SRVCron srvCron;
    private boolean isCancelled;
    
    public StartupCommandDispatchEvent(SRVCron srvCron)
    {
        this.srvCron = srvCron;
    }

    public List<String> getStartupCommands()
    {
        return srvCron.getStartupCommands();
    }
    
    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }
    
    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
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