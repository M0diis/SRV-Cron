package me.m0dii.srvcron.managers;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.job.EventJobContext;
import me.m0dii.srvcron.utils.EventPropertyResolver;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.Utils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public class EventManager implements Listener {
    private final SRVCron srvCron;
    private final Set<Listener> eventListeners = Collections.newSetFromMap(new IdentityHashMap<>());

    public EventManager(SRVCron srvCron) {
        this.srvCron = srvCron;
        Bukkit.getPluginManager().registerEvents(this, srvCron);
        refreshEventRegistrations();
    }

    public void refreshEventRegistrations() {
        unregisterDynamicListeners();

        for (EventType type : EventType.values()) {
            List<EventJob> jobs = srvCron.getEventJobs().get(type);
            if (jobs == null || jobs.isEmpty()) {
                continue;
            }
            Class<? extends Event> eventClass = legacyEventClass(type);
            if (eventClass == null) {
                srvCron.log("No Bukkit event class is registered for legacy event '" + type.getConfigName() + "'.");
                continue;
            }

            boolean ignoreCancelled = type == EventType.ITEM_PICKUP_EVENT;
            register(eventClass, EventPriority.NORMAL, ignoreCancelled,
                    event -> dispatchLegacy(type, event));
        }

        for (GenericEventDefinition definition : srvCron.getGenericEventDefinitions().values()) {
            if (!definition.getJobs().isEmpty()) {
                register(definition.getEventClass(), definition.getPriority(), definition.isIgnoreCancelled(),
                        event -> dispatchGeneric(definition, event));
            }
        }
    }

    public void unregisterDynamicListeners() {
        for (Listener listener : eventListeners) {
            HandlerList.unregisterAll(listener);
        }
        eventListeners.clear();
    }

    private void register(Class<? extends Event> eventClass, EventPriority priority, boolean ignoreCancelled,
                          java.util.function.Consumer<Event> action) {
        Listener listener = new Listener() { };
        try {
            Bukkit.getPluginManager().registerEvent(eventClass, listener, priority,
                    (registeredListener, event) -> action.accept(event), srvCron, ignoreCancelled);
            eventListeners.add(listener);
        } catch (RuntimeException ex) {
            srvCron.log("Failed to register event " + eventClass.getName() + ": " + ex.getMessage());
        }
    }

    private void dispatchGeneric(GenericEventDefinition definition, Event event) {
        List<String> commands = definition.getJobs().stream()
                .flatMap(job -> job.getCommands().stream())
                .toList();
        EventJobContext context = EventPropertyResolver.capture(
                event,
                definition.getPlayerPath(),
                definition.getWorldPath(),
                definition.getPlaceholderPaths(),
                commands
        );
        for (EventJob job : definition.getJobs()) {
            job.performJob(context);
        }
    }

    private void dispatchLegacy(EventType type, Event event) {
        Player player = event instanceof PlayerEvent playerEvent ? playerEvent.getPlayer()
                : event instanceof AsyncChatEvent chatEvent ? chatEvent.getPlayer() : null;
        World world = eventWorld(event, player);
        Map<String, String> placeholders = legacyPlaceholders(type, event);
        Map<String, Component> componentPlaceholders = legacyComponentPlaceholders(type, event);
        EventJobContext context = EventPropertyResolver.captureLegacy(event, player, world, placeholders, componentPlaceholders);
        for (EventJob job : srvCron.getEventJobs().getOrDefault(type, List.of())) {
            job.performJob(context);
        }
    }

    private Class<? extends Event> legacyEventClass(EventType type) {
        return switch (type) {
            case JOIN_EVENT -> PlayerJoinEvent.class;
            case QUIT_EVENT -> PlayerQuitEvent.class;
            case WEATHER_CHANGE_EVENT -> WeatherChangeEvent.class;
            case WORLD_LOAD_EVENT -> WorldLoadEvent.class;
            case PLAYER_BED_ENTER_EVENT -> PlayerBedEnterEvent.class;
            case PLAYER_BED_LEAVE_EVENT -> PlayerBedLeaveEvent.class;
            case PLAYER_CHANGE_WORLD_EVENT -> PlayerChangedWorldEvent.class;
            case PLAYER_GAMEMODE_CHANGE_EVENT -> PlayerGameModeChangeEvent.class;
            case PLAYER_KICK_EVENT -> PlayerKickEvent.class;
            case CHAT_EVENT -> AsyncChatEvent.class;
            case COMMAND_EVENT -> PlayerCommandPreprocessEvent.class;
            case ITEM_PICKUP_EVENT -> PlayerAttemptPickupItemEvent.class;
            case PLAYER_ADVANCEMENT_DONE_EVENT -> PlayerAdvancementDoneEvent.class;
        };
    }

    private World eventWorld(Event event, Player player) {
        if (event instanceof WorldLoadEvent worldLoadEvent) {
            return worldLoadEvent.getWorld();
        }
        if (event instanceof WeatherChangeEvent weatherChangeEvent) {
            return weatherChangeEvent.getWorld();
        }
        if (event instanceof PlayerChangedWorldEvent changedWorldEvent) {
            return changedWorldEvent.getPlayer().getWorld();
        }
        return player == null ? null : player.getWorld();
    }

    private Map<String, String> legacyPlaceholders(EventType type, Event event) {
        Map<String, String> values = new LinkedHashMap<>();
        switch (type) {
            case ITEM_PICKUP_EVENT -> {
                PlayerAttemptPickupItemEvent pickup = (PlayerAttemptPickupItemEvent) event;
                values.put("item_type", pickup.getItem().getItemStack().getType().name());
                values.put("item_amount", String.valueOf(pickup.getItem().getItemStack().getAmount()));
            }
            case COMMAND_EVENT -> values.put("command", ((PlayerCommandPreprocessEvent) event).getMessage());
            case CHAT_EVENT -> values.put("message", LegacyComponentSerializer.legacySection()
                    .serialize(((AsyncChatEvent) event).message()));
            case QUIT_EVENT -> {
                var message = ((PlayerQuitEvent) event).quitMessage();
                if (message != null) {
                    values.put("quit_reason", LegacyComponentSerializer.legacySection().serialize(message));
                }
            }
            case PLAYER_GAMEMODE_CHANGE_EVENT -> {
                PlayerGameModeChangeEvent change = (PlayerGameModeChangeEvent) event;
                values.put("from_gamemode", change.getPlayer().getGameMode().name());
                values.put("to_gamemode", change.getNewGameMode().name());
            }
            case PLAYER_CHANGE_WORLD_EVENT -> {
                PlayerChangedWorldEvent change = (PlayerChangedWorldEvent) event;
                values.put("from_world", change.getFrom().getName());
                values.put("to_world", change.getPlayer().getWorld().getName());
            }
            case PLAYER_KICK_EVENT -> values.put("kick_reason", LegacyComponentSerializer.legacySection()
                    .serialize(((PlayerKickEvent) event).reason()));
            case PLAYER_ADVANCEMENT_DONE_EVENT -> values.put("advancement_name", ((PlayerAdvancementDoneEvent) event)
                    .getAdvancement().getKey().getKey().replace('/', ' '));
            default -> {
                // No additional legacy values for this event.
            }
        }
        return values;
    }

    private Map<String, Component> legacyComponentPlaceholders(EventType type, Event event) {
        Map<String, Component> values = new LinkedHashMap<>();
        switch (type) {
            case CHAT_EVENT -> values.put("message", ((AsyncChatEvent) event).message());
            case QUIT_EVENT -> {
                Component quitMessage = ((PlayerQuitEvent) event).quitMessage();
                if (quitMessage != null) {
                    values.put("quit_reason", quitMessage);
                }
            }
            case PLAYER_KICK_EVENT -> values.put("kick_reason", ((PlayerKickEvent) event).reason());
            default -> {
                // No component-valued legacy placeholders for this event.
            }
        }
        return values;
    }

    @EventHandler
    public void onStartupCommandDispatchEvent(StartupCommandDispatchEvent event) {
        if (event.isCancelled()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.isCancelled()) {
                    return;
                }

                for (String command : event.getStartupCommands()) {
                    if (command.toUpperCase(Locale.ROOT).startsWith("<ALL>")) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            Utils.sendCommand(player, command);
                        }
                    } else {
                        Utils.sendCommand(null, command);
                    }
                }
            }
        }.runTaskLater(srvCron, 20);
    }

    @EventHandler
    public void onCronJobDispatchEvent(CronJobDispatchEvent event) {
        if (event.isCancelled()) {
            return;
        }

        CronJob job = event.getCronJob();
        if (job.isSuspended()) {
            srvCron.log("Job " + job.getName() + " is suspended, skipping...");
            return;
        }

        job.increaseRunCount();
        for (String command : event.getJobCommands()) {
            if (command.toUpperCase(Locale.ROOT).startsWith("<ALL>")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Utils.sendCommand(player, command);
                }
            } else {
                Utils.sendCommand(null, command);
            }
        }
    }

    @EventHandler
    public void onEventJobDispatchEvent(EventJobDispatchEvent event) {
        if (event.isCancelled()) {
            return;
        }

        EventJob job = event.getEventJob();
        if (job.isSuspended()) {
            srvCron.log("Event Job " + job.getName() + " is suspended, skipping...");
            return;
        }

        EventJobContext context = event.getContext();
        Map<String, String> placeholders = context.getPlaceholders();
        Player player = context.getPlayer();
        for (String originalCommand : event.getFinalCommands()) {
            String command = originalCommand;
            if (player != null) {
                command = Utils.handleDispatcherPlaceholders(command, player, placeholders.keySet());
            }

            if (command.toUpperCase(Locale.ROOT).startsWith("<ALL>")) {
                command = command.substring("<ALL>".length());
                for (Player recipient : Bukkit.getOnlinePlayers()) {
                    if (player != null && recipient.getUniqueId().equals(player.getUniqueId())) {
                        continue;
                    }
                    Utils.sendCommand(recipient, command, context);
                }
            } else if (command.toUpperCase(Locale.ROOT).startsWith("<ALL+>")) {
                command = command.substring("<ALL+>".length());
                for (Player recipient : Bukkit.getOnlinePlayers()) {
                    Utils.sendCommand(recipient, command, context);
                }
            } else {
                Utils.sendCommand(player, command, context);
            }
        }
    }
}
