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
        if(args.length == 0)
        {
            sender.sendMessage("§aSRV-Cron system by §M0dii");
            sender.sendMessage("§aSRV-Cron version: §e" + SRVCron.getDescription().getVersion());
            
            return true;
        }
        
        if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("srvcron.reload"))
            {
                sender.sendMessage("§cYou have no permission to execute this command.");
                
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
        
        if(args[0].equalsIgnoreCase("list"))
        {
            if(!sender.hasPermission("srvcron.list"))
            {
                sender.sendMessage("§cYou have no permission to execute this command.");
                
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
    
        return true;
    }

}
