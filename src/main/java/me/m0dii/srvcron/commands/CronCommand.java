package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CronCommand implements CommandExecutor
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
        if(cmd.getName().equalsIgnoreCase("mccron"))
        {
            if(args.length == 0)
            {
                sender.sendMessage("§aMC-Cron system by §eThe_TadeSK");
                sender.sendMessage("§aMC-Cron version: §e" + SRVCron.getDescription().getVersion());
                
                return true;
            }
            else if(args[0].equalsIgnoreCase("reload"))
            {
                if(!sender.hasPermission("mccron.reload"))
                {
                    sender.sendMessage("§cNo permission!");
                    
                    return true;
                }
                
                sender.sendMessage("§aReloading jobs..");
                
                for(CronJob j : SRVCron.getJobs().values())
                    j.stopJob();

                SRVCron.getJobs().clear();

                SRVCron.reloadConfig();
                SRVCron.saveConfig();

                SRVCron.loadJobs();
                sender.sendMessage("§aJobs reloaded!");
            }
            else if(args[0].equalsIgnoreCase("list"))
            {
                if(!sender.hasPermission("mccron.list"))
                {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                
                sender.sendMessage("§aAll Cron jobs:");
                
                int id = 1;
                
                for(CronJob j : SRVCron.getJobs().values())
                {
                    sender.sendMessage("§c" + id + "# §a" + j.getName() + " §e(" + j.getTime() + ") §c" + j.getCommands().size() + " commands");
                    
                    id++;
                }
            }
        }
        
        return true;
    }

}
