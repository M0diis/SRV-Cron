package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class EventManager implements Listener
{
    private final SRVCron SRVCron;

    public EventManager(SRVCron SRVCron) {
        this.SRVCron = SRVCron;

        Bukkit.getPluginManager().registerEvents(this, SRVCron);
    }

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if(!SRVCron.getEventJobs().containsKey(EventType.JOIN_EVENT))
            return;

        for(EventJob job : SRVCron.getEventJobs().get(EventType.JOIN_EVENT))
            job.performJob(player);
    }

    @EventHandler
    public void onQuitEvent(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();

        if (!SRVCron.getEventJobs().containsKey(EventType.QUIT_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.QUIT_EVENT))
            job.performJob(player);
    }

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event)
    {
        if (!SRVCron.getEventJobs().containsKey(EventType.WEATHER_CHANGE_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.WEATHER_CHANGE_EVENT))
            job.performJob(null);
    }
    
    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event)
    {
        if (!SRVCron.getEventJobs().containsKey(EventType.WORLD_LOAD_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.WORLD_LOAD_EVENT))
            job.performJob(null);
    }
    
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event)
    {
        Player player = event.getPlayer();
        
        if (!SRVCron.getEventJobs().containsKey(EventType.PLAYER_BED_ENTER_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.PLAYER_BED_ENTER_EVENT))
            job.performJob(player);
    }
    
    @EventHandler
    public void onPlayerBedEnter(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        
        if (!SRVCron.getEventJobs().containsKey(EventType.PLAYER_CHANGE_WORLD_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.PLAYER_CHANGE_WORLD_EVENT))
            job.performJob(player);
    }
}
