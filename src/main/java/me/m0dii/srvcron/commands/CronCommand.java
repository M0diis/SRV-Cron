package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.CronJobDispatchEvent;
import me.m0dii.srvcron.utils.LangConfig;
import me.m0dii.srvcron.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CronCommand implements CommandExecutor, TabCompleter
{
    private final SRVCron srvCron;
    private final LangConfig langCfg;

    public CronCommand(SRVCron srvCron)
    {
        this.srvCron = srvCron;
        this.langCfg = srvCron.getLangCfg();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args)
    {
        if(args.length == 0)
        {
            sendf(sender, "&aSRV-Cron by &2M0dii");
            sendf(sender, "&aVersion: &2" + srvCron.getDescription().getVersion());
            sendf(sender, "&7/srvcron reload &8- &7reloads the config and jobs.");
            sendf(sender, "&7/srvcron suspend &8- &7suspends job execution.");
        
            return true;
        }
        
        execute(sender, args);
    
        return true;
    }
    
    private void execute(CommandSender sender, String[] args)
    {
        if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("srvcron.command.reload"))
            {
                sendf(sender, langCfg.getNoPermissionCmd());
            
                return;
            }
            
            for(CronJob j : srvCron.getJobs().values())
            {
                j.stopJob();
            }
        
            srvCron.getJobs().clear();
        
            srvCron.reloadConfig();
            srvCron.saveConfig();
        
            srvCron.loadJobs();
            
            langCfg.reloadConfig();
    
            sendf(sender, langCfg.getConfigReloaded());
        }
    
        if(args[0].equalsIgnoreCase("suspend"))
        {
            suspend(sender, args);
        }
        else if(args[0].equalsIgnoreCase("resume"))
        {
            resume(sender, args);
        }
        else if(args[0].equalsIgnoreCase("jobinfo"))
        {
            jobInfo(sender, args);
        }
        else if(args[0].equalsIgnoreCase("list"))
        {
            list(sender, args);
        }
        else if(args[0].equalsIgnoreCase("run"))
        {
            run(sender, args);
        }
    }
    
    private void suspend(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.suspend"))
        {
            sendf(sender, langCfg.getNoPermissionCmd());
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, langCfg.getUsage("suspend"));
    
            return;
        }
        
        if(args[1].equalsIgnoreCase("all"))
        {
            for(CronJob j : srvCron.getJobs().values())
            {
                j.suspend();
            }
            
            sendf(sender, langCfg.getSuspendedAll());
            
            return;
        }
        
        if(!srvCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, langCfg.getJobDoesNotExist().replace("{job}", args[1]));
    
            return;
        }
        
        CronJob j = srvCron.getJobs().get(args[1]);
        
        if(j.isSuspended())
        {
            sendf(sender, langCfg.getJobIsSuspended().replace("{job}", args[1]));
        }
        else
        {
            j.suspend();
        
            sendf(sender, langCfg.getJobSuspended().replace("{job}", args[1]));
        }
    }
    
    private void run(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.run"))
        {
            sendf(sender, langCfg.getNoPermissionCmd());
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, langCfg.getUsage("run"));
    
            return;
        }
        
        if(args[1].equalsIgnoreCase("event"))
        {
            if(args.length == 2)
            {
                sendf(sender, langCfg.getUsage("run"));
                
                return;
            }
            
            EventJob job = null;
            
            for(List<EventJob> l : srvCron.getEventJobs().values())
            {
                for(EventJob ej : l)
                {
                    if(ej.getName().equalsIgnoreCase(args[2]))
                    {
                        job = ej;
                        
                        break;
                    }
                }
            }
            
            if(job == null)
            {
                sendf(sender, langCfg.getJobDoesNotExist().replace("{job}", args[2]));
                
                return;
            }
    
            for(String cmd : job.getCommands())
            {
                if(cmd.toUpperCase().startsWith("<ALL>"))
                {
                    cmd = cmd.replace("<ALL>", "");
                    
                    for(Player p : Bukkit.getOnlinePlayers())
                    {
                        Utils.sendCommand(p, cmd);
                    }
                }
                else
                {
                    if(sender instanceof Player)
                        Utils.sendCommand((Player)sender, cmd);
                    else Utils.sendCommand(null, cmd);
                }

            }
    
            srvCron.log("Manually running Event Job " + job.getName() + " by " + sender.getName());
    
            sendf(sender, langCfg.getEventJobExecuted().replace("{job}", job.getName()));
            
            return;
        }
        
        if(!srvCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, langCfg.getJobDoesNotExist().replace("{job}", args[1]));
    
            return;
        }
        
        CronJob j = srvCron.getJobs().get(args[1]);
        
        srvCron.log("Manually running job " + j.getName() + " by " + sender.getName());
    
        sendf(sender, langCfg.getJobDispatched().replace("{job}", j.getName()));
    
        Bukkit.getPluginManager().callEvent(new CronJobDispatchEvent(j));
    }
    
    private void list(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.list"))
        {
            sendf(sender, langCfg.getNoPermissionCmd());
    
            return;
        }
        
        if(args.length == 2)
        {
            if(args[1].equalsIgnoreCase("events"))
            {
                sendf(sender, "&8&m----------------------------------");
                sendf(sender, "&7EVENT JOBS &8(&7" + srvCron.getEventJobs().size() + "&8)");
                sendf(sender, "&8&m----------------------------------");
            
                int id = 1;
            
                for(List<EventJob> eventJobs : srvCron.getEventJobs().values())
                {
                    for(EventJob job : eventJobs)
                    {
                        sendf(sender, String.format("&8#&7%d. &a%s &8(&7%s&8) &2%d commands;", id,
                                job.getName().toLowerCase(), job.getEventType().getConfigName(), job.getCommands().size()));
                    
                        id++;
                    }
                }
    
                return;
            }
        }
        
        sendf(sender, "&8&m----------------------------------");
        sendf(sender, "&7CRON JOBS &8(&7" + srvCron.getJobs().size() + "&8)");
        sendf(sender, "&8&m----------------------------------");
        
        int id = 1;
        
        for(CronJob j : srvCron.getJobs().values())
        {
            if(j.isSuspended())
            {
                sendf(sender, String.format("&8#&7%d. &a%s &8(&7%s&8) &4[&cS&4] &2%d commands;", id, j.getName().toLowerCase(),
                        j.getTime(), j.getCommands().size()));

            }
            else
            {
                sendf(sender, String.format("&8#&7%d. &a%s &8(&7%s&8) &2%d commands;", id, j.getName().toLowerCase(), j.getTime(),
                        j.getCommands().size()));
            }

            id++;
        }
        
        sendf(sender, "&8&m----------------------------------");
    }
    
    private void jobInfo(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.jobinfo"))
        {
            sendf(sender, langCfg.getNoPermissionCmd());
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, langCfg.getUsage("jobinfo"));
    
            return;
        }
        
        if(!srvCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, langCfg.getJobDoesNotExist().replace("{job}", args[1]));
        }
        
        CronJob j = srvCron.getJobs().get(args[1]);
        
        sendf(sender, "&8&m----------------------------------");
        sendf(sender, "&7JOB INFORMATION");
        sendf(sender, "&8&m----------------------------------");
        
        sendf(sender, "&8Job name: &7" + j.getName());
        sendf(sender, "&8Time: &7" + j.getTime());
        sendf(sender, "&8Run count: &7" + j.getRunCount());
        sendf(sender, "&8Suspended: &7" + j.isSuspended());
        sendf(sender, "&8Commands: ");
        
        for(String s : j.getCommands())
        {
            sendf(sender, "&8- &7" + s);
        }
        
        sendf(sender, "&8&m----------------------------------");
    }
    
    private void resume(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.resume"))
        {
            sendf(sender, langCfg.getNoPermissionCmd());
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, langCfg.getUsage("resume"));
    
            return;
        }
    
        if(args[1].equalsIgnoreCase("all"))
        {
            for(CronJob j : srvCron.getJobs().values())
            {
                j.resume();
            }
        
            sendf(sender, langCfg.getResumedAll());
        
            return;
        }
        
        if(!srvCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, langCfg.getJobDoesNotExist().replace("{job}", args[1]));
    
            return;
        }
        
        CronJob j = srvCron.getJobs().get(args[1]);
        
        if(!j.isSuspended())
        {
            sendf(sender, langCfg.getJobNotSuspended().replace("{job}", args[1]));
        }
        else
        {
            j.resume();
        
            sendf(sender, langCfg.getJobResumed().replace("{job}", args[1]));
        }
    }
    
    private void sendf(CommandSender sender, String msg)
    {
        String m = ChatColor.translateAlternateColorCodes('&', msg);
        
        if(sender instanceof Player)
        {
            m = Utils.setPlaceholders(m, (Player)sender);
        }
        
        sender.sendMessage(m);
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                                @NotNull String[] args)
    {
        List<String> completes = new ArrayList<>();
        
        if(args.length == 1)
        {
            completes.add("reload");
            completes.add("list");
            completes.add("jobinfo");
            completes.add("run");
            completes.add("resume");
            completes.add("suspend");
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("run"))
        {
            completes.add("event");
            completes.addAll(srvCron.getJobs().keySet());
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("list"))
        {
            completes.add("events");
            completes.add("jobs");
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("jobinfo"))
        {
            completes.addAll(srvCron.getJobs().keySet());
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("suspend"))
        {
            for(CronJob j : srvCron.getJobs().values())
            {
                if(!j.isSuspended())
                {
                    completes.add(j.getName());
                }
            }
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("resume"))
        {
            for(CronJob j : srvCron.getJobs().values())
            {
                if(j.isSuspended())
                {
                    completes.add(j.getName());
                }
            }
        }
    
        if(args.length == 3 && args[0].equalsIgnoreCase("run")
        && args[1].equalsIgnoreCase("event"))
        {
            for(List<EventJob> list : srvCron.getEventJobs().values())
            {
                for(EventJob job : list)
                {
                    completes.add(job.getName());
                }
            }
        }
        
        return completes;
    }
}
