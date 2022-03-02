package me.m0dii.srvcron.bungee.commands;

import me.m0dii.srvcron.bungee.BungeeSRVCron;
import me.m0dii.srvcron.bungee.job.BungeeCronJob;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCronCommand extends Command
{
    private final BungeeSRVCron cron;

    public BungeeCronCommand(BungeeSRVCron cron)
    {
        super("srvcron");
        this.cron = cron;
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage("§aSRV-Cron system by §eM0dii");
            sender.sendMessage("§aSRV-Cron version: §e" + cron.getDescription().getVersion());
        }
        else if(args[0].equalsIgnoreCase("reload"))
        {
            if(!sender.hasPermission("mccron.reload"))
            {
                sender.sendMessage("§cNo permission!");
                return;
            }
            
            sender.sendMessage("§aReloading jobs..");
            
            for(BungeeCronJob j : cron.getJobs().values())
                j.stopJob();

            cron.getJobs().clear();

            cron.reloadConfig();
            cron.saveConfig();

            cron.loadJobs();
            
            sender.sendMessage("§aJobs reloaded!");
        }
        else if(args[0].equalsIgnoreCase("list"))
        {
            if(!sender.hasPermission("mccron.list")){
                sender.sendMessage("§cNo permission!");
                return;
            }
            
            sender.sendMessage("§aAll Cron jobs:");
            int id = 1;
            
            for(BungeeCronJob j : cron.getJobs().values())
            {
                sender.sendMessage("§c" + id + "# §a" + j.getName() + " §e(" + j.getTime() + ") §c" + j.getCommands().size() + " commands");
                id++;
            }
        }
    }
}
