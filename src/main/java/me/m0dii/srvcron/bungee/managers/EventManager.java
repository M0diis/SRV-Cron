package me.m0dii.srvcron.bungee.managers;

import me.m0dii.srvcron.bungee.BungeeSRVCron;
import me.m0dii.srvcron.bungee.job.BungeeEventJob;
import me.m0dii.srvcron.utils.EventType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class EventManager implements Listener {
    private final BungeeSRVCron cron;

    public EventManager(BungeeSRVCron cron) {
        this.cron = cron;

        ProxyServer.getInstance().getPluginManager().registerListener(this.cron, this);
    }

    @EventHandler
    public void onJoinEvent(final ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!cron.getEventJobs().containsKey(EventType.JOIN_EVENT))
            return;

        for (BungeeEventJob job : cron.getEventJobs().get(EventType.JOIN_EVENT))
            job.performJob(player);
    }

    @EventHandler
    public void onQuitEvent(final ServerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!cron.getEventJobs().containsKey(EventType.QUIT_EVENT)) {
            return;
        }

        for (BungeeEventJob job : cron.getEventJobs().get(EventType.QUIT_EVENT)) {
            job.performJob(player);
        }
    }
}
