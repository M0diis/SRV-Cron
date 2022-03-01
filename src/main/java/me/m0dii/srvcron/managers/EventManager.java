package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class EventManager implements Listener
{
    private final SRVCron SRVCron;

    public EventManager(SRVCron SRVCron)
    {
        this.SRVCron = SRVCron;

        Bukkit.getPluginManager().registerEvents(this, SRVCron);
    }

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if(ignore(EventType.JOIN_EVENT))
            return;

        for(EventJob job : SRVCron.getEventJobs().get(EventType.JOIN_EVENT))
            job.performJob(player, player.getWorld());
    }

    @EventHandler
    public void onQuitEvent(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();

        if (ignore(EventType.QUIT_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.QUIT_EVENT))
            job.performJob(player, player.getWorld());
    }

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event)
    {
        if (ignore(EventType.WEATHER_CHANGE_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.WEATHER_CHANGE_EVENT))
            job.performJob(event.getWorld());
    }
    
    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event)
    {
        if (ignore(EventType.WORLD_LOAD_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.WORLD_LOAD_EVENT))
            job.performJob(event.getWorld());
    }
    
    @EventHandler
    public void onPlayerGamemodeChangeEvent(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();

        if (ignore(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    @EventHandler
    public void onPlayerBedEnterEvent(PlayerBedEnterEvent event)
    {
        Player player = event.getPlayer();
        
        if (ignore(EventType.PLAYER_BED_ENTER_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.PLAYER_BED_ENTER_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        
        if (ignore(EventType.PLAYER_CHANGE_WORLD_EVENT))
            return;

        for (EventJob job : SRVCron.getEventJobs().get(EventType.PLAYER_CHANGE_WORLD_EVENT))
            job.performJob(player, player.getWorld());
    }
    
    private boolean ignore(EventType type)
    {
        return !SRVCron.getEventJobs().containsKey(type);
    }
}
