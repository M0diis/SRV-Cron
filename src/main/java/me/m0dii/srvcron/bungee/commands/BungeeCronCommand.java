package me.m0dii.srvcron.bungee.commands;

import me.m0dii.srvcron.bungee.BungeeSRVCron;
import me.m0dii.srvcron.bungee.job.BungeeCronJob;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCronCommand extends Command {
    private final BungeeSRVCron cron;

    public BungeeCronCommand(BungeeSRVCron cron) {
        super("srvcron");
        this.cron = cron;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§aSRV-Cron system by §eM0dii");
            sender.sendMessage("§aSRV-Cron version: §e" + cron.getDescription().getVersion());
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mccron.reload")) {
                sender.sendMessage("§cNo permission!");
                return;
            }

            sender.sendMessage("§aReloading jobs..");

            for (BungeeCronJob j : cron.getJobs().values())
                j.stopJob();

            cron.getJobs().clear();

            cron.reloadConfig();
            cron.saveConfig();

            cron.loadJobs();

            sender.sendMessage("§aJobs reloaded!");
        } else if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("mccron.list")) {
                sender.sendMessage("§cNo permission!");
                return;
            }

            sendf(sender, "&7----------------------------------");
            sendf(sender, "&aAll jobs (" + cron.getJobs().size() + ")");

            int id = 1;

            for (BungeeCronJob j : cron.getJobs().values()) {
                sendf(sender, String.format("&8#&7%d& &a%s &8(&7%s&8) &2%d commands;", id, j.getName().toLowerCase(), j.getTime(),
                        j.getCommands().size()));
                id++;
            }

            sendf(sender, "&7----------------------------------");
        }
    }

    private void sendf(CommandSender sender, String msg) {
        String m = ChatColor.translateAlternateColorCodes('&', msg);

        sender.sendMessage(m);
    }
}
