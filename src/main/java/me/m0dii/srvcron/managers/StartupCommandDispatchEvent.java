package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StartupCommandDispatchEvent extends Event
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    
    private final SRVCron srvCron;
    
    public StartupCommandDispatchEvent(SRVCron srvCron)
    {
        this.srvCron = srvCron;
    }

    public List<String> getStartupCommands()
    {
        return srvCron.getStartUpCommands();
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
}