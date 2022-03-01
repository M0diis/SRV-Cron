package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class TimerCommand implements CommandExecutor
{
    private final SRVCron SRVCron;

    public TimerCommand(SRVCron SRVCron)
    {
        this.SRVCron = SRVCron;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args)
    {
        if(!sender.hasPermission("srvcron.timer"))
        {
            sender.sendMessage("§cYou do not have permission to execute this command.");
        
            return true;
        }
    
        if(args.length == 0)
        {
            sender.sendMessage("§aUsage: /timer <time> <command>");
            
            return true;
        }
        
        if(args.length >= 2)
        {
            StringBuilder c = new StringBuilder();
            
            for(int i = 1; i < args.length; i++)
            {
                c.append(" ").append(args[i]);
            }
            
            c = new StringBuilder(c.substring(1));
            
            int time = Integer.parseInt(args[0]);
            
            if(time > 1800)
            {
                sender.sendMessage("§cMaximum amount is 30 minutes!");
                
                return true;
            }
            
            runCmd(c.toString(), time);
        }
    
        return true;
    }
    
    public void runCmd(String cmd, int seconds)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.setPlaceholders(cmd));
            }
        }.runTaskLater(SRVCron, seconds * 20L);
    }
}
