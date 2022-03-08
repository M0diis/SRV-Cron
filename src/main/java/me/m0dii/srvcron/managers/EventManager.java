package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

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
            job.performJob(player, player.getWorld(), event);
    }
    
    @EventHandler
    public void onPlayerItemPickupEvent(PlayerPickupItemEvent event)
    {
        if(ignore(EventType.ITEM_PICKUP_EVENT))
            return;
    
        Player player = event.getPlayer();
        Item item = event.getItem();

        for(EventJob job : srvCron.getEventJobs().get(EventType.ITEM_PICKUP_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
                
                command = command.replaceAll("\\{item_type}", item.getItemStack().getType().name());
                command = command.replaceAll("\\{item_amount}", String.valueOf(item.getItemStack().getAmount()));
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }
    
    @EventHandler
    public void onCommandEvent(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();

        if (ignore(EventType.COMMAND_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.COMMAND_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{command}", event.getMessage());
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }
    
    @EventHandler
    public void onPlayerChatEvent(AsyncPlayerChatEvent event)
    {
        if(ignore(EventType.CHAT_EVENT))
            return;

        Player player = event.getPlayer();
        
        for(EventJob job : srvCron.getEventJobs().get(EventType.CHAT_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{message}", event.getMessage());

                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }

    @EventHandler
    public void onQuitEvent(PlayerQuitEvent event)
    {
        if (ignore(EventType.QUIT_EVENT))
            return;
    
        Player player = event.getPlayer();
        
        for (EventJob job : srvCron.getEventJobs().get(EventType.QUIT_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{quit_reason}", event.getReason().name());
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event)
    {
        if (ignore(EventType.WEATHER_CHANGE_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.WEATHER_CHANGE_EVENT))
            job.performJob(null, event.getWorld(), event);
    }
    
    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event)
    {
        if (ignore(EventType.WORLD_LOAD_EVENT))
            return;

        for (EventJob job : srvCron.getEventJobs().get(EventType.WORLD_LOAD_EVENT))
            job.performJob(null, event.getWorld(), event);
    }
    
    @EventHandler
    public void onPlayerGamemodeChangeEvent(PlayerGameModeChangeEvent event)
    {
        if (ignore(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
            return;
    
        Player player = event.getPlayer();
        
        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_GAMEMODE_CHANGE_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{from_gamemode}", player.getGameMode().name());
                command = command.replaceAll("\\{to_gamemode}", event.getNewGameMode().name());
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }
    
    @EventHandler
    public void onPlayerBedEnterEvent(PlayerBedEnterEvent event)
    {
        if (ignore(EventType.PLAYER_BED_ENTER_EVENT))
            return;
    
        Player player = event.getPlayer();

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_BED_ENTER_EVENT))
            job.performJob(player, player.getWorld(), event);
    }
    
    @EventHandler
    public void onPlayerAdvancementDoneEvent(PlayerAdvancementDoneEvent event)
    {
        if (ignore(EventType.PLAYER_ADVANCEMENT_DONE_EVENT))
            return;
    
        Player player = event.getPlayer();

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_ADVANCEMENT_DONE_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{advancement_name}",
                        event.getAdvancement().getKey().getKey().replace("/", " "));
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }
    
    @EventHandler
    public void onPlayerBedLeaveEvent(PlayerBedLeaveEvent event)
    {
        if (ignore(EventType.PLAYER_BED_LEAVE_EVENT))
            return;
        
        Player player = event.getPlayer();
        
        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_BED_LEAVE_EVENT))
            job.performJob(player, player.getWorld(), event);
    }
    
    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event)
    {
        if (ignore(EventType.PLAYER_CHANGE_WORLD_EVENT))
            return;
        
        Player player = event.getPlayer();

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_CHANGE_WORLD_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{to_world}", player.getWorld().getName());
                command = command.replaceAll("\\{from_world}", event.getFrom().getName());
        
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
    }
    
    @EventHandler
    public void onPlayerKickEvent(PlayerKickEvent event)
    {
        if (ignore(EventType.PLAYER_KICK_EVENT))
            return;
        
        Player player = event.getPlayer();

        for (EventJob job : srvCron.getEventJobs().get(EventType.PLAYER_KICK_EVENT))
        {
            List<String> commands = new ArrayList<>(job.getCommands());
    
            for(int i = 0; i < commands.size(); i++)
            {
                String command = commands.get(i);
        
                command = command.replaceAll("\\{kick_reason}", event.getReason());
                
                commands.set(i, command);
            }
    
            job.performJob(player, player.getWorld(), event, commands);
        }
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
                    if(cmd.toUpperCase().startsWith("<ALL>"))
                    {
                        for(Player p : Bukkit.getOnlinePlayers())
                        {
                            Utils.sendCommand(p, cmd);
                        }
                    }
                    else
                    {
                        Utils.sendCommand(null, cmd);
                    }

                }
            }
        }.runTaskLater(srvCron, 20);
    }
    
    @EventHandler
    public void onCronJobDispatchEvent(CronJobDispatchEvent event)
    {
        if(event.isCancelled())
        {
            return;
        }
        
        CronJob job = event.getCronJob();
    
        if(job.isSuspended())
        {
            srvCron.log("Job " + job.getName() + " is suspended, skipping...");
        
            return;
        }
    
        job.increaseRunCount();
    
        for(String cmd : event.getJobCommands())
        {
            if(cmd.toUpperCase().startsWith("<ALL>"))
            {
                for(Player p : Bukkit.getOnlinePlayers())
                {
                    Utils.sendCommand(p, cmd);
                }
            }
            else
            {
                Utils.sendCommand(null, cmd);
            }
        }
    }
    
    @EventHandler
    public void onEventJobDispatchEvent(EventJobDispatchEvent event)
    {
        if(event.isCancelled())
        {
            return;
        }
        
        EventJob job = event.getEventJob();
        
        if(job.isSuspended())
        {
            srvCron.log("Event Job " + job.getName() + " is suspended, skipping...");
        
            return;
        }
    
        for(String cmd : event.getFinalCommands())
        {
            World world = event.getWorld();
            
            if(world != null)
            {
                cmd = cmd.replace("{world_name}", world.getName());
            }
            
            Player player = event.getPlayer();
            
            if(player != null)
            {
                cmd = Utils.handleDispatcherPlaceholders(cmd, player);
            }
    
            if(cmd.toUpperCase().startsWith("<ALL>"))
            {
                cmd = cmd.replace("<ALL>", "");
                
                for(Player p : Bukkit.getOnlinePlayers())
                {
                    if(player != null && p.getUniqueId().equals(player.getUniqueId()))
                        continue;
                    
                    Utils.sendCommand(p, cmd);
                }
            }
            else if(cmd.toUpperCase().startsWith("<ALL+>"))
            {
                cmd = cmd.replace("<ALL+>", "");
    
                for(Player p : Bukkit.getOnlinePlayers())
                {
                    Utils.sendCommand(p, cmd);
                }
            }
            else
            {
                Utils.sendCommand(player, cmd);
            }
        }
    }
    
    private boolean ignore(EventType type)
    {
        return !srvCron.getEventJobs().containsKey(type);
    }
}
