package me.m0dii.srvcron.job;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.managers.EventJobDispatchEvent;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
        performJob(player, null);
    }
    
    public void performJob(World world)
    {
        performJob(null, world);
    }

    public void performJob(Player player, World world)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if(eventType == EventType.JOIN_EVENT && !player.isOnline())
                    return;

                for(String cmd : commands)
                {
                    if(world != null)
                    {
                        cmd = cmd.replace("%world_name%", world.getName());
                    }
    
                    if(cmd.startsWith("["))
                    {
                        for(Player p : Bukkit.getOnlinePlayers())
                        {
                            Utils.sendCommand(p, cmd);
                        }
                    }
                    else
                    {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.setPlaceholders(cmd));
                    }
                }
                
                Bukkit.getPluginManager().callEvent(new EventJobDispatchEvent(EventJob.this, player, world));
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
