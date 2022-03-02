package me.m0dii.srvcron.job;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.managers.EventJobDispatchEvent;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class EventJob
{
    private final SRVCron SRVCron;
    private final String name;
    private final int time;
    private final List<String> commands;
    private final EventType eventType;

    public EventJob(SRVCron SRVCron, String name, int time, List<String> commands, EventType eventType)
    {
        this.SRVCron = SRVCron;
        this.name = name;
        this.time = time;
        this.commands = commands;
        this.eventType = eventType;
    }
    
    public void performJob(Player player)
    {
        performJob(player, null, null);
    }
    
    public void performJob(World world)
    {
        performJob(null, world, null);
    }

    public void performJob(Player player, World world, Event event)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if(eventType == EventType.JOIN_EVENT && !player.isOnline())
                    return;
                
                Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(EventJob.this, event, player, world));
            }
        }.runTaskLater(SRVCron, time * 20L);
    }

    public String getName()
    {
        return name;
    }

    public EventType getEventType()
    {
        return eventType;
    }
    
    public List<String> getCommands()
    {
        return commands;
    }
}
