package me.m0dii.srvcron.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTransformer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%\\s]+%");

    private static final Map<String, Character> SMALL_CAPS_MAP = Map.ofEntries(
            Map.entry("a", 'ᴀ'), Map.entry("b", 'ʙ'), Map.entry("c", 'ᴄ'),
            Map.entry("d", 'ᴅ'), Map.entry("e", 'ᴇ'), Map.entry("f", 'ғ'),
            Map.entry("g", 'ɢ'), Map.entry("h", 'ʜ'), Map.entry("i", 'ɪ'),
            Map.entry("j", 'ᴊ'), Map.entry("k", 'ᴋ'), Map.entry("l", 'ʟ'),
            Map.entry("m", 'ᴍ'), Map.entry("n", 'ɴ'), Map.entry("o", 'ᴏ'),
            Map.entry("p", 'ᴘ'), Map.entry("q", 'ǫ'), Map.entry("r", 'ʀ'),
            Map.entry("s", 's'), Map.entry("t", 'ᴛ'), Map.entry("u", 'ᴜ'),
            Map.entry("v", 'ᴠ'), Map.entry("w", 'ᴡ'), Map.entry("x", 'x'),
            Map.entry("y", 'ʏ'), Map.entry("z", 'ᴢ'), Map.entry("Ą", 'ą'),
            Map.entry("Č", 'č'), Map.entry("Ę", 'ę'), Map.entry("Ė", 'ė'),
            Map.entry("Į", 'į'), Map.entry("Š", 'š'), Map.entry("Ų", 'ų'),
            Map.entry("Ū", 'ū'), Map.entry("Ž", 'ž'), Map.entry("ą", 'ą'),
            Map.entry("č", 'č'), Map.entry("ę", 'ę'), Map.entry("ė", 'ė'),
            Map.entry("į", 'į'), Map.entry("š", 'š'), Map.entry("ų", 'ų'),
            Map.entry("ū", 'ū'), Map.entry("ž", 'ž')
    );

    private String text;

    public TextTransformer(@NotNull String text) {
        this.text = text;
    }

    public static TextTransformer of(@NotNull String text) {
        return new TextTransformer(text);
    }

    @NotNull
    public TextTransformer upperCase() {
        text = text.toUpperCase();
        return this;
    }

    @NotNull
    public TextTransformer lowerCase() {
        text = text.toLowerCase();
        return this;
    }

    @NotNull
    public TextTransformer trim() {
        text = text.trim();
        return this;
    }

    @NotNull
    public TextTransformer applyColor() {
        if (text == null || text.isEmpty()) {
            return this;
        }
        text = Utils.format(text);
        return this;
    }

    @NotNull
    public TextTransformer stripColor() {
        text = PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacySection().deserialize(text)
        );
        return this;
    }

    @NotNull
    public Component asComponent() {
        return Component.text(text);
    }

    @NotNull
    @SuppressWarnings("java:S127")
    public TextTransformer smallCaps() {
        if (text != null && !text.isEmpty()) {
            text = applySmallCaps(text);
        }
        return this;
    }

    @NotNull
    @SuppressWarnings("java:S127")
    public static TextTransformer smallCaps(@NotNull String text) {
        return new TextTransformer(applySmallCaps(text));
    }

    private static String applySmallCaps(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length;) {
            char current = chars[i];
            if ((current == '&' || current == '\u00a7') && i + 1 < chars.length) {
                result.append(current).append(chars[i + 1]);
                i += 2;
                continue;
            }
            if (current == '#') {
                int end = i + 7;
                if (end <= chars.length && isHex(chars, i + 1, end)) {
                    result.append(chars, i, 7);
                    i = end;
                    continue;
                }
            }
            if (current == '<') {
                int close = text.indexOf('>', i + 1);
                if (close >= 0) {
                    result.append(text, i, close + 1);
                    i = close + 1;
                    continue;
                }
            }

            String key = String.valueOf(current).toLowerCase();
            result.append(SMALL_CAPS_MAP.getOrDefault(key, current));
            i++;
        }
        return result.toString();
    }

    private static boolean isHex(char[] chars, int start, int end) {
        for (int i = start; i < end; i++) {
            char value = chars[i];
            if (!(value >= '0' && value <= '9' || value >= 'a' && value <= 'f' || value >= 'A' && value <= 'F')) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public TextTransformer replace(@NotNull String target, @NotNull String replacement) {
        text = text.replace(target, replacement);
        return this;
    }

    @NotNull
    public TextTransformer replace(@NotNull Map<String, Object> replacements) {
        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return this;
    }

    @NotNull
    public TextTransformer substring(int beginIndex, int endIndex) {
        text = text.substring(beginIndex, endIndex);
        return this;
    }

    @NotNull
    public TextTransformer substring(int beginIndex) {
        text = text.substring(beginIndex);
        return this;
    }

    @NotNull
    public TextTransformer append(@NotNull String suffix) {
        text = text + suffix;
        return this;
    }

    @NotNull
    public TextTransformer prepend(@NotNull String prefix) {
        text = prefix + text;
        return this;
    }

    @NotNull
    public TextTransformer repeat(int count) {
        text = String.valueOf(text).repeat(Math.max(0, count));
        return this;
    }

    @NotNull
    public TextTransformer reverse() {
        text = new StringBuilder(text).reverse().toString();
        return this;
    }

    @NotNull
    public TextTransformer clear() {
        text = "";
        return this;
    }

    @NotNull
    public TextTransformer set(@NotNull String newText) {
        text = newText;
        return this;
    }

    @NotNull
    public String get() {
        return text;
    }

    @NotNull
    public Component kyorify() {
        return kyorify(text, null);
    }

    @NotNull
    public Component colorize() {
        return kyorify(text, null);
    }

    @NotNull
    public static Component kyorify(@NotNull String text) {
        return kyorify(text, null);
    }

    @NotNull
    public static Component kyorify(@NotNull String text, @Nullable OfflinePlayer player) {
        return kyorify(text, player, true);
    }

    static Component kyorifyWithoutPlaceholderExpansion(@NotNull String text) {
        return kyorify(text, null, false);
    }

    private static Component kyorify(@NotNull String text, @Nullable OfflinePlayer player, boolean expandPlaceholders) {
        String namespace = "srvcron_" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder safeText = new StringBuilder(text.length());
        TagResolver.Builder placeholders = TagResolver.builder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        int lastEnd = 0;
        int index = 0;

        while (matcher.find()) {
            safeText.append(text, lastEnd, matcher.start());
            String placeholder = matcher.group();
            String tag = namespace + '_' + index++;
            safeText.append('<').append(tag).append('>');
            String value = expandPlaceholders ? resolvePlaceholder(player, placeholder) : placeholder;
            placeholders.resolver(Placeholder.unparsed(tag, value));
            lastEnd = matcher.end();
        }
        safeText.append(text, lastEnd, text.length());

        return MINI_MESSAGE.deserialize(Kyorifier.kyorify(safeText.toString()), placeholders.build())
                .decoration(TextDecoration.ITALIC, false);
    }

    @NotNull
    public static Component miniMessage(@NotNull String text) {
        return kyorify(text);
    }

    @NotNull
    public static Component miniMessage(@NotNull String text, @Nullable OfflinePlayer player) {
        return kyorify(text, player);
    }

    private static String resolvePlaceholder(@Nullable OfflinePlayer player, String placeholder) {
        if (placeholderApiEnabled()) {
            return PlaceholderAPI.setPlaceholders(player, placeholder);
        }
        if (player == null) {
            return placeholder;
        }

        if (placeholder.equalsIgnoreCase("%player_name%") && player.getName() != null) {
            return player.getName();
        }
        if (placeholder.equalsIgnoreCase("%player_uuid%")) {
            return player.getUniqueId().toString();
        }
        return placeholder;
    }

    private static boolean placeholderApiEnabled() {
        return Bukkit.getPluginManager() != null
                && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }
}
