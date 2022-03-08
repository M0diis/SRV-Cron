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
    
    private boolean suspended = false;
    private int runCount;

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
    
    public void performJob(Player player, World world, Event event, List<String> commands)
    {
        if(eventType == EventType.JOIN_EVENT && !player.isOnline())
            return;
    
        if(this.suspended)
        {
            this.SRVCron.log("Job " + EventJob.this.name + " is suspended, skipping...");
        
            return;
        }
    
        if(time == 0)
        {
            Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(this, event, player, world, commands));
        }
        else
        {
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(EventJob.this, event, player, world, commands));
                }
            }.runTaskLater(SRVCron, time * 20L);
        }
    }

    public void performJob(Player player, World world, Event event)
    {
        performJob(player, world, event, commands);
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
    
    public int getRunCount()
    {
        return runCount;
    }
    
    public void increaseRunCount()
    {
        runCount++;
    }
    
    public void suspend()
    {
        suspended = true;
    }
    
    public void resume()
    {
        suspended = false;
    }
    
    public boolean isSuspended()
    {
        return suspended;
    }
}
