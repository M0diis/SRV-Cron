package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            sender.sendMessage("§aSRV-Cron by §M0dii");
            sender.sendMessage("§aSRV-Cron version: §e" + SRVCron.getDescription().getVersion());
            
            return true;
        }
        
        if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("srvcron.reload"))
            {
                sendf(sender, "&cYou have no permission to execute this command.");
                
                return true;
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
            if(!sender.hasPermission("srvcron.job.suspend"))
            {
                sendf(sender, "&cYou have no permission to execute this command.");
                
                return true;
            }
            
            if(args.length == 1)
            {
                sendf(sender, "&cUsage: /cron suspend <job-name>");
                
                return true;
            }
            
            if(!SRVCron.getJobs().containsKey(args[1]))
            {
                sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
                
                return true;
            }
            
            CronJob j = SRVCron.getJobs().get(args[1]);
            
            if(j.isSuspended())
            {
                sendf(sender, "&cJob &4\"" + args[1] + "\" &cis already suspended.");
            }
            else
            {
                j.suspend();
                
                sendf(sender, "&aJob &4\"" + args[1] + "\" &ahas been successfully suspended.");
            }
        }
    
        if(args[0].equalsIgnoreCase("resume"))
        {
            if(!sender.hasPermission("srvcron.job.resume"))
            {
                sendf(sender, "&cYou have no permission to execute this command.");
            
                return true;
            }
        
            if(args.length == 1)
            {
                sendf(sender, "&cUsage: /cron resume <job-name>");
            
                return true;
            }
        
            if(!SRVCron.getJobs().containsKey(args[1]))
            {
                sendf(sender, "&cJob &4\"" + args[1] + "\" &cdoes not exist.");
            
                return true;
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
        
        if(args[0].equalsIgnoreCase("jobinfo"))
        {
            if(!sender.hasPermission("srvcron.jobinfo"))
            {
                sender.sendMessage("§cYou have no permission to execute this command.");
                
                return true;
            }
            
            if(args.length == 1)
            {
                sendf(sender, "&cUsage: /cron jobinfo <job-name>");
                
                return true;
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
        
        if(args[0].equalsIgnoreCase("list"))
        {
            if(!sender.hasPermission("srvcron.list"))
            {
                sender.sendMessage("§cYou have no permission to execute this command.");
                
                return true;
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
                    
                    return true;
                }
            }

            sendf(sender, "&8&m----------------------------------");
            sendf(sender, "&7CRON JOBS &8(&7" + SRVCron.getJobs().size() + "&8)");
            sendf(sender, "&8&m----------------------------------");
            
            int id = 1;
    
            for(CronJob j : SRVCron.getJobs().values())
            {
                sendf(sender, String.format("&8#&7%d. &a%s &8(&7%s&8) &2%d commands;", id, j.getName().toLowerCase(), j.getTime(),
                        j.getCommands().size()));
                id++;
            }
    
            sendf(sender, "&8&m----------------------------------");
        }
    
        return true;
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
        
        return completes;
    }
}
