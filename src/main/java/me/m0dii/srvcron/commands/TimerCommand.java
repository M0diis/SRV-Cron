package me.m0dii.srvcron.commands;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.utils.TextTransformer;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NonNull;

public class TimerCommand implements CommandExecutor {
    private final SRVCron srvCron;

    public TimerCommand(SRVCron srvCron) {
        this.srvCron = srvCron;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command cmd,
                             @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("srvcron.command.timer")) {
            sendf(sender, "§cYou do not have permission to execute this command.");

            return true;
        }

        if (args.length == 0) {
            sendf(sender, "§aUsage: /timer <time> <command>");

            return true;
        }

        if (args.length == 1) {
            sendf(sender, "§cMissing command argument.");
            sendf(sender, "§aUsage: /timer <time> <command>");

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
            sendf(sender, "§cTime must be a valid number.");

            return true;
        }

        if (time < 0) {
            sendf(sender, "§cTime cannot be negative.");

            return true;
        }

        if (time > 3600) {
            sendf(sender, "§cMaximum amount is 60 minutes!");

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
        }.runTaskLater(srvCron, seconds * 20L);
    }

    private void sendf(CommandSender sender, String message) {
        sender.sendMessage(TextTransformer.kyorify(message));
    }
}
