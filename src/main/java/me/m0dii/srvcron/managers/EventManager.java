package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class EventManager implements Listener
{
    private final SRVCron srvCron;

    public EventManager(SRVCron srvCron)
    {
        this.srvCron = srvCron;

        Bukkit.getPluginManager().registerEvents(this, srvCron);
    }

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if(ignore(EventType.JOIN_EVENT))
            return;

        for(EventJob job : srvCron.getEventJobs().get(EventType.JOIN_EVENT))
            job.performJob(player, player.getWorld());
    }

    @EventHandler
    public void onQuitEvent(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();

        if (ignore(EventType.QUIT_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.QUIT_EVENT))
            job.performJob(player, player.getWorld());
    }

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event)
    {
        if (ignore(EventType.WEATHER_CHANGE_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.WEATHER_CHANGE_EVENT))
            job.performJob(event.getWorld());
    }
    
    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event)
    {
        if (ignore(EventType.WORLD_LOAD_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.WORLD_LOAD_EVENT))
            job.performJob(event.getWorld());
    }
    
    @EventHandler
    public void onPlayerGamemodeChangeEvent(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();

        if (ignore(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    @EventHandler
    public void onPlayerBedEnterEvent(PlayerBedEnterEvent event)
    {
        Player player = event.getPlayer();
        
        if (ignore(EventType.PLAYER_BED_ENTER_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_BED_ENTER_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        
        if (ignore(EventType.PLAYER_CHANGE_WORLD_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_CHANGE_WORLD_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    @EventHandler
    public void onStartupCommandDispatchEvent(StartupCommandDispatchEvent event)
    {
        if(event.isCancelled())
        {
            return;
        }
        
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if(event.isCancelled())
                {
                    return;
                }
                
                for(String cmd : event.getStartupCommands())
                {
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
            }
        }.runTaskLater(srvCron, 20);
    }
    
    private boolean ignore(EventType type)
    {
        return !srvCron.getEventJobs().containsKey(type);
    }
}
