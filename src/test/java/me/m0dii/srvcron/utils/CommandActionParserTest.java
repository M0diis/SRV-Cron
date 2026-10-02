package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.testsupport.MockBukkitTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommandActionParserTest extends MockBukkitTest {
    @Test
    void eventPlaceholderTextIsInsertedAsLiteralText() {
        Player player = server.addPlayer();

        CommandActionParser.parse(player, "[MESSAGE] {event.value}",
                Map.of("event.value", "<red>literal value"));

        Component sent = ((org.mockbukkit.mockbukkit.entity.PlayerMock) player).nextComponentMessage();
        assertEquals("<red>literal value", PlainTextComponentSerializer.plainText().serialize(sent));
        assertFalse(LegacyComponentSerializer.legacySection().serialize(sent).contains("§c"));
    }

    @Test
    void componentEventPlaceholderKeepsItsFormatting() {
        Player player = server.addPlayer();
        Component styled = Component.text("styled value", NamedTextColor.RED);

        CommandActionParser.parse(player, "[MESSAGE] {event.value}", Map.of(), Map.of("event.value", styled));

        Component sent = ((org.mockbukkit.mockbukkit.entity.PlayerMock) player).nextComponentMessage();
        assertEquals("styled value", PlainTextComponentSerializer.plainText().serialize(sent));
        assertEquals("§cstyled value", LegacyComponentSerializer.legacySection().serialize(sent));
    }
}
