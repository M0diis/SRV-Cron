package me.m0dii.srvcron.managers;

import me.m0dii.srvcron.SRVCronAPI;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.testsupport.MockBukkitTest;
import me.m0dii.srvcron.utils.EventType;
import me.m0dii.srvcron.utils.EventPropertyResolverTest;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenericEventManagerTest extends MockBukkitTest {
    @Test
    void loadsGenericDefinitionsAndFiltersInvalidAliasesAndEventClasses() {
        configureGenericEvent("sample", TestEvent.class, 0, "HIGH", true,
                Map.of("amount_alias", "amount", "player_name", "amount", "bad alias", "amount"));
        configureRawGenericEvent("not-an-event", String.class.getName(), "NORMAL");
        configureRawGenericEvent("no-handler-list", MissingHandlerListEvent.class.getName(), "NORMAL");
        configureGenericEvent("bad-priority", TestEvent.class, 0, "urgent", false, Map.of());
        plugin.loadJobs();

        GenericEventDefinition definition = plugin.getGenericEventDefinitions().get("sample");
        assertNotNull(definition);
        assertEquals(TestEvent.class, definition.getEventClass());
        assertEquals(EventPriority.HIGH, definition.getPriority());
        assertTrue(definition.isIgnoreCancelled());
        assertEquals(Map.of("amount_alias", "amount"), definition.getPlaceholderPaths());
        assertEquals(List.of("test-job"), definition.getJobs().stream().map(EventJob::getName).toList());
        assertFalse(plugin.getGenericEventDefinitions().containsKey("not-an-event"));
        assertFalse(plugin.getGenericEventDefinitions().containsKey("no-handler-list"));
        assertFalse(plugin.getGenericEventDefinitions().containsKey("bad-priority"));
    }

    @Test
    void registersWithConfiguredPriorityAndHonorsIgnoreCancelled() {
        configureGenericEvent("sample", TestEvent.class, 0, "HIGH", true,
                Map.of("amount_alias", "amount"));
        plugin.loadJobs();
        new EventManager(plugin);
        List<EventJobDispatchEvent> dispatched = captureDispatches();

        RegisteredListener registration = List.of(TestEvent.getHandlerList().getRegisteredListeners()).stream()
                .filter(listener -> listener.getPlugin() == plugin)
                .findFirst()
                .orElseThrow();
        assertEquals(EventPriority.HIGH, registration.getPriority());

        Bukkit.getPluginManager().callEvent(new TestEvent(null, 1, true));
        assertTrue(dispatched.isEmpty());

        Bukkit.getPluginManager().callEvent(new TestEvent(null, 2, false));
        assertEquals(1, dispatched.size());
        assertEquals("2", dispatched.getFirst().getEventPlaceholders().get("amount_alias"));
        assertEquals("sample", dispatched.getFirst().getEventIdentifier());
        assertEquals(TestEvent.class, dispatched.getFirst().getEventClass());
        assertNull(dispatched.getFirst().getEventType());
    }

    @Test
    void refreshUnregistersOldListenersInsteadOfDuplicatingThem() {
        configureGenericEvent("sample", TestEvent.class, 0, "NORMAL", false,
                Map.of("amount_alias", "amount"));
        plugin.loadJobs();
        EventManager manager = new EventManager(plugin);
        List<EventJobDispatchEvent> dispatched = captureDispatches();

        Bukkit.getPluginManager().callEvent(new TestEvent(null, 1, false));
        plugin.loadJobs();
        Bukkit.getPluginManager().callEvent(new TestEvent(null, 2, false));

        assertEquals(2, dispatched.size());
        assertEquals("2", dispatched.getLast().getEventPlaceholders().get("amount_alias"));
        assertEquals(1, List.of(TestEvent.getHandlerList().getRegisteredListeners()).stream()
                .filter(listener -> listener.getPlugin() == plugin).count());
    }

    @Test
    void delayedDispatchUsesValuesCapturedWhenTheSourceEventFired() {
        configureGenericEvent("sample", TestEvent.class, 1, "NORMAL", false,
                Map.of("amount_alias", "amount"));
        plugin.loadJobs();
        new EventManager(plugin);
        List<EventJobDispatchEvent> dispatched = captureDispatches();
        TestEvent event = new TestEvent(null, 7, false);

        Bukkit.getPluginManager().callEvent(event);
        event.setAmount(99);
        assertTrue(dispatched.isEmpty());

        server.getScheduler().performTicks(20L);

        assertEquals(1, dispatched.size());
        assertEquals("7", dispatched.getFirst().getEventPlaceholders().get("amount_alias"));
    }

    @Test
    void exposesGenericJobsThroughApiLookupsAndValidatesSourceEvents() {
        configureGenericEvent("sample", TestEvent.class, 0, "NORMAL", false,
                Map.of("amount_alias", "amount"));
        plugin.loadJobs();
        EventJob job = plugin.getGenericEventJobs().get("sample").getFirst();
        SRVCronAPI api = plugin.getApi();

        assertEquals(List.of(job), api.getEventJobsByClass(TestEvent.class));
        assertEquals(List.of(job), api.getEventJobsByIdentifier("sample"));
        assertSame(job, api.getEventJobByName("test-job"));
        assertThrows(IllegalArgumentException.class,
                () -> api.runEventJob(job, new EventPropertyResolverTest.AmbiguousPlayerEvent(null, null), null, null));

        EventManager manager = new EventManager(plugin);
        List<EventJobDispatchEvent> dispatched = captureDispatches();
        TestEvent source = new TestEvent(null, 4, false);
        api.runEventJob(job, source, null, null);

        assertEquals(1, dispatched.size());
        assertSame(source, dispatched.getFirst().getEvent());
        assertEquals("4", dispatched.getFirst().getEventPlaceholders().get("amount_alias"));
        assertEquals(EventType.JOIN_EVENT, EventType.isEventJob("join-event"));
        manager.unregisterDynamicListeners();
    }

    @Test
    @SuppressWarnings("removal")
    void retainsLegacyCommandAndAdvancementDispatchWithTheirPlaceholders() {
        var config = plugin.getConfig();
        config.set("event-jobs.command-event.command-job.time", 0);
        config.set("event-jobs.command-event.command-job.commands", List.of());
        config.set("event-jobs.player-advancement-done-event.advancement-job.time", 0);
        config.set("event-jobs.player-advancement-done-event.advancement-job.commands", List.of());
        plugin.loadJobs();
        new EventManager(plugin);
        List<EventJobDispatchEvent> dispatched = captureDispatches();
        Player player = server.addPlayer();

        Bukkit.getPluginManager().callEvent(new PlayerCommandPreprocessEvent(player, "/example argument"));
        Advancement advancement = (Advancement) Proxy.newProxyInstance(
                Advancement.class.getClassLoader(), new Class<?>[]{Advancement.class},
                (proxy, method, args) -> method.getName().equals("getKey")
                        ? NamespacedKey.minecraft("story/root") : null);
        Bukkit.getPluginManager().callEvent(new PlayerAdvancementDoneEvent(player, advancement));

        assertEquals(2, dispatched.size());
        assertEquals(EventType.COMMAND_EVENT, dispatched.get(0).getEventType());
        assertEquals("/example argument", dispatched.get(0).getEventPlaceholders().get("command"));
        assertSame(player, dispatched.get(0).getPlayer());
        assertEquals(EventType.PLAYER_ADVANCEMENT_DONE_EVENT, dispatched.get(1).getEventType());
        assertEquals("story root", dispatched.get(1).getEventPlaceholders().get("advancement_name"));
    }

    private void configureGenericEvent(String id, Class<? extends Event> eventClass, int delay,
                                       String priority, boolean ignoreCancelled,
                                       Map<String, String> placeholders) {
        configureRawGenericEvent(id, eventClass.getName(), priority);
        var config = plugin.getConfig();
        config.set("generic-event-jobs." + id + ".ignore-cancelled", ignoreCancelled);
        placeholders.forEach((alias, path) ->
                config.set("generic-event-jobs." + id + ".placeholders." + alias, path));
        config.set("generic-event-jobs." + id + ".jobs.test-job.time", delay);
        config.set("generic-event-jobs." + id + ".jobs.test-job.commands", List.of());
    }

    private void configureRawGenericEvent(String id, String className, String priority) {
        var config = plugin.getConfig();
        config.set("generic-event-jobs." + id + ".event", className);
        config.set("generic-event-jobs." + id + ".priority", priority);
        config.set("generic-event-jobs." + id + ".jobs.test-job.time", 0);
        config.set("generic-event-jobs." + id + ".jobs.test-job.commands", List.of());
    }

    private List<EventJobDispatchEvent> captureDispatches() {
        List<EventJobDispatchEvent> dispatched = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDispatch(EventJobDispatchEvent event) {
                dispatched.add(event);
            }
        }, plugin);
        return dispatched;
    }

    public static class TestEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;
        private int amount;
        private boolean cancelled;

        public TestEvent(Player player, int amount, boolean cancelled) {
            this.player = player;
            this.amount = amount;
            this.cancelled = cancelled;
        }

        public Player getPlayer() {
            return player;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class MissingHandlerListEvent extends Event {
        private final HandlerList handlers = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }
    }
}
