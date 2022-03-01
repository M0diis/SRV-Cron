package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
        if(cmd.getName().equalsIgnoreCase("timer"))
        {
            if(sender instanceof Player)
            {
                sender.sendMessage("§cOnly console can perform this command!");
                
                return true;
            }
            
            if(args.length == 0)
            {
                sender.sendMessage("§aUse /timer <time> <command>");
                
                return true;
            
            }
            else if(args.length >= 2)
            {
                String c = "";
                
                for(int i = 1;i < args.length;i++){
                    c = c + " " + args[i];
                }
                
                c = c.substring(1);
                
                int time = Integer.parseInt(args[0]);
                
                if(time > 1800)
                {
                    sender.sendMessage("§cMaximum amount is 30 minutes!");
                    
                    return true;
                }
                
                runCmd(c, time);
            }
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.parsePlaceholder(cmd));
            }
        }.runTaskLater(SRVCron, seconds * 20L);
    }
}
