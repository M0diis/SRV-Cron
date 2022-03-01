package me.m0dii.srvcron.job;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
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
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if(eventType == EventType.JOIN_EVENT && !player.isOnline())
                    return;

                for(String c : commands)
                {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.parsePlaceholder(c));
                }
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
}
