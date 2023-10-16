package me.m0dii.srvcron.bungee.commands;

import me.m0dii.srvcron.bungee.BungeeSRVCron;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.concurrent.TimeUnit;

public class BungeeTimerCommand extends Command {
    private final BungeeSRVCron cron;

    public BungeeTimerCommand(BungeeSRVCron cron) {
        super("timer");
        this.cron = cron;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            sender.sendMessage("§cOnly console can perform this command!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§aUse /timer <time> <command>");
        } else if (args.length >= 2) {
            StringBuilder c = new StringBuilder();

            for (int i = 1; i < args.length; i++) {
                c.append(" ").append(args[i]);
            }

            c = new StringBuilder(c.substring(1));

            int time = Integer.parseInt(args[0]);

            if (time > 6000) {
                sender.sendMessage("§cMaximum timer delay is 60 minutes!");

                return;
            }

            runCmd(c.toString(), time);
        }
    }

    public void runCmd(String cmd, int seconds) {
        ProxyServer.getInstance().getScheduler().schedule(cron, () ->
        {
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), cmd);
        }, seconds, TimeUnit.SECONDS);
    }
}
