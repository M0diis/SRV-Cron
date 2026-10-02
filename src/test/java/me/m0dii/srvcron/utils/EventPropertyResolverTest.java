package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.job.EventJobContext;
import me.m0dii.srvcron.testsupport.MockBukkitTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EventPropertyResolverTest extends MockBukkitTest {
    @Test
    void resolvesNestedGetterPathsAndIndexedListsAndArrays() {
        SampleEvent event = new SampleEvent(null, null, 5, Component.text("message"));

        assertEquals("payload", EventPropertyResolver.resolvePath(event, "payload.name"));
        assertEquals("second", EventPropertyResolver.resolvePath(event, "values[1].name"));
        assertEquals("first", EventPropertyResolver.resolvePath(event, "labels[0]"));
        assertEquals("payload", EventPropertyResolver.resolvePath(event, "event.payload.name"));
    }

    @Test
    void capturesAliasesDirectValuesAndImmutableSnapshots() {
        SampleEvent event = new SampleEvent(null, null, 5, Component.text("message"));

        EventJobContext context = EventPropertyResolver.capture(
                event,
                null,
                null,
                Map.of("quantity", "payload.count"),
                List.of("count={event.payload.count}")
        );

        event.getPayload().setCount(20);

        assertEquals("5", context.getPlaceholders().get("quantity"));
        assertEquals("5", context.getPlaceholders().get("event.payload.count"));
        assertThrows(UnsupportedOperationException.class,
                () -> context.getPlaceholders().put("other", "value"));
    }

    @Test
    void capturesComponentPropertiesForStylePreservingSubstitution() {
        Component styled = Component.text("styled", NamedTextColor.RED);
        SampleEvent event = new SampleEvent(null, null, 5, styled);

        EventJobContext context = EventPropertyResolver.capture(
                event, null, null, Map.of("alias", "component"), List.of("{event.component}"));

        assertSame(styled, context.getComponentPlaceholders().get("alias"));
        assertSame(styled, context.getComponentPlaceholders().get("event.component"));
        assertEquals("§cstyled", context.getPlaceholders().get("alias"));
        assertThrows(UnsupportedOperationException.class,
                () -> context.getComponentPlaceholders().put("other", Component.empty()));
    }

    @Test
    void infersPlayerAndWorldAndHonorsConfiguredPlayerPath() {
        Player first = server.addPlayer();
        Player second = server.addPlayer();
        SampleEvent event = new SampleEvent(first, first.getWorld(), 1, Component.empty());

        EventJobContext inferred = EventPropertyResolver.capture(event, null, null, Map.of(), List.of());
        assertSame(first, inferred.getPlayer());
        assertSame(first.getWorld(), inferred.getWorld());
        assertEquals(first.getName(), inferred.getPlaceholders().get("player_name"));
        assertEquals(first.getWorld().getName(), inferred.getPlaceholders().get("world_name"));

        AmbiguousPlayerEvent ambiguous = new AmbiguousPlayerEvent(first, second);
        EventJobContext selected = EventPropertyResolver.capture(
                ambiguous, "secondary", null, Map.of(), List.of());
        assertSame(second, selected.getPlayer());
    }

    @Test
    void returnsNullForMissingMalformedAndOutOfRangePaths() {
        SampleEvent event = new SampleEvent(null, null, 1, Component.empty());

        assertNull(EventPropertyResolver.resolvePath(event, "payload.unknown"));
        assertNull(EventPropertyResolver.resolvePath(event, "values[9].name"));
        assertNull(EventPropertyResolver.resolvePath(event, "values[0]junk"));
        assertNull(EventPropertyResolver.resolvePath(event, "payload."));
        assertNull(EventPropertyResolver.resolvePath(null, "payload.name"));
    }

    @Test
    void stringifiesCommonBukkitValuesPredictably() {
        World world = server.addPlayer().getWorld();
        ItemStack stack = new ItemStack(Material.DIAMOND, 3);

        assertEquals("DIAMOND x3", EventPropertyResolver.stringify(stack));
        assertEquals("minecraft:some/path", EventPropertyResolver.stringify(NamespacedKey.minecraft("some/path")));
        assertEquals("world,1,2,3", EventPropertyResolver.stringify(new Location(world, 1.8, 2.2, 3.9)));
        assertEquals("one, two", EventPropertyResolver.stringify(List.of("one", "two")));
        assertEquals("one, two", EventPropertyResolver.stringify(new String[]{"one", "two"}));
        assertEquals("sample text", EventPropertyResolver.stringify(Component.text("sample text"))
                .replace('\u00a7', '&'));
    }

    @Test
    void eventTextValuesRemainLiteralAndComponentValuesKeepTheirStyle() {
        Component literal = TextTransformer.kyorify(
                "{event.value}", null, Map.of("event.value", "<red>not markup"), Map.of());
        Component styled = TextTransformer.kyorify(
                "{event.value}", null, Map.of(), Map.of("event.value", Component.text("red text", NamedTextColor.RED)));

        assertEquals("<red>not markup", PlainTextComponentSerializer.plainText().serialize(literal));
        assertNull(literal.style().color());
        assertEquals("red text", PlainTextComponentSerializer.plainText().serialize(styled));
        assertEquals(NamedTextColor.RED, styled.style().color());
    }

    public static class SampleEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;
        private final World world;
        private final Payload payload;
        private final List<Payload> values = List.of(new Payload("first", 1), new Payload("second", 2));
        private final String[] labels = {"first", "second"};
        private final Component component;

        public SampleEvent(Player player, World world, int count, Component component) {
            this.player = player;
            this.world = world;
            this.payload = new Payload("payload", count);
            this.component = component;
        }

        public Player getPlayer() {
            return player;
        }

        public World getWorld() {
            return world;
        }

        public Payload getPayload() {
            return payload;
        }

        public List<Payload> getValues() {
            return values;
        }

        public String[] getLabels() {
            return labels;
        }

        public Component getComponent() {
            return component;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class AmbiguousPlayerEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player primary;
        private final Player secondary;

        public AmbiguousPlayerEvent(Player primary, Player secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public Player getPrimary() {
            return primary;
        }

        public Player getSecondary() {
            return secondary;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static final class Payload {
        private final String name;
        private int count;

        public Payload(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
