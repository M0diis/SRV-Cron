package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.managers.CronJobDispatchEvent;
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
    private final SRVCron SRVCron;

    public CronCommand(SRVCron SRVCron)
    {
        this.SRVCron = SRVCron;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args)
    {
        if(args.length == 0)
        {
            sendf(sender, "&aSRV-Cron by &2M0dii");
            sendf(sender, "&aVersion: &2" + SRVCron.getDescription().getVersion());
        
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
                sendf(sender, "&cYou have no permission to execute this command.");
            
                return;
            }
        
            sendf(sender, "&aReloading jobs..");
        
            for(CronJob j : SRVCron.getJobs().values())
            {
                j.stopJob();
            }
        
            SRVCron.getJobs().clear();
        
            SRVCron.reloadConfig();
            SRVCron.saveConfig();
        
            SRVCron.loadJobs();
        
            sendf(sender, "&aJobs have been reloaded.");
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
    }
    
    private void suspend(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.suspend"))
        {
            sendf(sender, "&cYou have no permission to execute this command.");
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, "&cUsage: /srvcron suspend <job-name>");
    
            return;
        }
        
        if(!SRVCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
    
            return;
        }
        
        CronJob j = SRVCron.getJobs().get(args[1]);
        
        if(j.isSuspended())
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cis already suspended.");
        }
        else
        {
            j.suspend();
        
            sendf(sender, "&aJob &2\"" + args[1] + "\" &ahas been successfully suspended.");
        }
    }
    
    private void run(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.run"))
        {
            sendf(sender, "&cYou have no permission to execute this command.");
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, "&cUsage: /srvcron run <job-name>");
    
            return;
        }
        
        if(args[1].equalsIgnoreCase("event"))
        {
            if(args.length == 2)
            {
                sendf(sender, "&cUsage: /srvcron run event <event-name>");
                
                return;
            }
            
            EventJob job = null;
            
            for(List<EventJob> l : SRVCron.getEventJobs().values())
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
                sendf(sender, "&cEvent Job &4\"" + args[2] + "\" &cdoes not exist.");
                
                return;
            }
    
            for(String cmd : job.getCommands())
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
            
            sendf(sender, "&aEvent Job &2\"" + args[2] + "\" &a commands have been executed.");
            
            return;
        }
        
        if(!SRVCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
    
            return;
        }
        
        CronJob j = SRVCron.getJobs().get(args[1]);
    
        Bukkit.getPluginManager().callEvent(new CronJobDispatchEvent(j));
    }
    
    private void list(CommandSender sender, String[] args)
    {
        if(!sender.hasPermission("srvcron.command.list"))
        {
            sendf(sender, "&cYou have no permission to execute this command.");
    
            return;
        }
        
        if(args.length == 2)
        {
            if(args[1].equalsIgnoreCase("events"))
            {
                sendf(sender, "&8&m----------------------------------");
                sendf(sender, "&7EVENT JOBS &8(&7" + SRVCron.getEventJobs().size() + "&8)");
                sendf(sender, "&8&m----------------------------------");
            
                int id = 1;
            
                for(List<EventJob> eventJobs : SRVCron.getEventJobs().values())
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
        sendf(sender, "&7CRON JOBS &8(&7" + SRVCron.getJobs().size() + "&8)");
        sendf(sender, "&8&m----------------------------------");
        
        int id = 1;
        
        for(CronJob j : SRVCron.getJobs().values())
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
            sendf(sender, "&cYou have no permission to execute this command.");
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, "&cUsage: /cron jobinfo <job-name>");
    
            return;
        }
        
        if(!SRVCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
        }
        
        CronJob j = SRVCron.getJobs().get(args[1]);
        
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
            sendf(sender, "&cYou have no permission to execute this command.");
    
            return;
        }
        
        if(args.length == 1)
        {
            sendf(sender, "&cUsage: /srvcron resume <job-name>");
    
            return;
        }
        
        if(!SRVCron.getJobs().containsKey(args[1]))
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
    
            return;
        }
        
        CronJob j = SRVCron.getJobs().get(args[1]);
        
        if(!j.isSuspended())
        {
            sendf(sender, "&cJob &4\"" + args[1] + "\" &cis not suspended.");
        }
        else
        {
            j.resume();
        
            sendf(sender, "&aJob &4\"" + args[1] + "\" &ahas been successfully resumed.");
        }
    }
    
    private void sendf(CommandSender sender, String msg)
    {
        String m = ChatColor.translateAlternateColorCodes('&', msg);
        
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
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("run"))
        {
            completes.add("event");
            completes.addAll(SRVCron.getJobs().keySet());
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("list"))
        {
            completes.add("events");
            completes.add("jobs");
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("jobinfo"))
        {
            completes.addAll(SRVCron.getJobs().keySet());
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("suspend"))
        {
            for(CronJob j : SRVCron.getJobs().values())
            {
                if(!j.isSuspended())
                {
                    completes.add(j.getName());
                }
            }
        }
        
        if(args.length == 2 && args[0].equalsIgnoreCase("resume"))
        {
            for(CronJob j : SRVCron.getJobs().values())
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
            for(List<EventJob> list : SRVCron.getEventJobs().values())
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
