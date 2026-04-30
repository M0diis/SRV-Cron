package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class TimerCommand implements CommandExecutor {
    private final SRVCron SRVCron;

    public TimerCommand(SRVCron SRVCron) {
        this.SRVCron = SRVCron;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String label, String[] args) {
        if (!sender.hasPermission("srvcron.command.timer")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");

            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§aUsage: /timer <time> <command>");

            return true;
        }

        if (args.length == 1) {
            sender.sendMessage("§cMissing command argument.");
            sender.sendMessage("§aUsage: /timer <time> <command>");

            return true;
        }

        StringBuilder c = new StringBuilder();

        for (int i = 1; i < args.length; i++) {
            c.append(" ").append(args[i]);
        }

        c = new StringBuilder(c.substring(1));

        final int time;

        try {
            time = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cTime must be a valid number.");

            return true;
        }

        if (time < 0) {
            sender.sendMessage("§cTime cannot be negative.");

            return true;
        }

        if (time > 3600) {
            sender.sendMessage("§cMaximum amount is 60 minutes!");

            return true;
        }

        runCmd(c.toString(), time);

        return true;
    }

    public void runCmd(String cmd, int seconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.setPlaceholders(cmd));
            }
        }.runTaskLater(SRVCron, seconds * 20L);
    }
}
