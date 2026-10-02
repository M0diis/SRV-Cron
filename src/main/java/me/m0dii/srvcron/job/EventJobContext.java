package me.m0dii.srvcron.job;

import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Map;

/** Snapshot of the event and values used while dispatching an event job. */
public final class EventJobContext {
    private final Event event;
    private final Player player;
    private final World world;
    private final Map<String, String> placeholders;
    private final Map<String, Component> componentPlaceholders;

    public EventJobContext(Event event, Player player, World world, Map<String, String> placeholders) {
        this(event, player, world, placeholders, Map.of());
    }

    public EventJobContext(Event event, Player player, World world, Map<String, String> placeholders,
                           Map<String, Component> componentPlaceholders) {
        this.event = event;
        this.player = player;
        this.world = world;
        this.placeholders = Map.copyOf(placeholders);
        this.componentPlaceholders = Map.copyOf(componentPlaceholders);
    }

    public Event getEvent() {
        return event;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public Map<String, Component> getComponentPlaceholders() {
        return componentPlaceholders;
    }
}
