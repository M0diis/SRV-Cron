package me.m0dii.srvcron.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.m0dii.srvcron.SRVCron;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses scheduled command strings and dispatches the requested action. */
public final class CommandActionParser {
    private static final String IF_PREFIX = "{IF:";
    private static final Pattern LEGACY_SELECTOR_PATTERN = Pattern.compile("@([pares])\\[([^\\]]+)]");
    private static final Pattern EVENT_VALUE_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final String[] COMPARISON_OPERATORS = {">=", "<=", "==", "!=", "~~", "!~", "~=", ">", "<"};
    private static final String[] SCORE_OPERATORS = {">=", "<=", "==", "!=", ">", "<"};

    private CommandActionParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void parse(@NotNull Player player, @NotNull List<String> commands) {
        for (String command : commands) {
            parse(player, command);
        }
    }

    public static void parse(@NotNull OfflinePlayer player, @NotNull List<String> commands) {
        for (String command : commands) {
            parse(player, command);
        }
    }

    /**
     * Supported actions include {@code [MESSAGE]}, {@code [TITLE]}, {@code [ACTIONBAR]},
     * {@code [SOUND]}, {@code [PARTICLE]}, {@code [CHAT]}, {@code [PLAYER]}, {@code [OP]},
     * {@code [CONSOLE]}, {@code [BROADCAST]}, and {@code [LOG <file>]}.
     *
     * <p>Prefix conditions with {@code {IF:type:value}} and join them with {@code &&} or
     * {@code ||}. Consecutive conditions imply AND; AND binds more tightly than OR. The
     * former action-header filter form, such as {@code [TEXT (PERMISSION:staff)]}, is kept
     * for existing configurations.</p>
     */
    public static void parse(@Nullable OfflinePlayer player, @NotNull String rawCommand) {
        parse(player, rawCommand, Map.of());
    }

    public static void parse(@Nullable OfflinePlayer player, @NotNull String rawCommand,
                             @NotNull Map<String, String> eventPlaceholders) {
        parse(player, rawCommand, eventPlaceholders, Map.of());
    }

    public static void parse(@Nullable OfflinePlayer player, @NotNull String rawCommand,
                             @NotNull Map<String, String> eventPlaceholders,
                             @NotNull Map<String, Component> componentPlaceholders) {
        String command = rawCommand.trim();
        if (command.isEmpty()) {
            return;
        }

        List<String> conditions = new ArrayList<>();
        List<Boolean> operators = new ArrayList<>(); // true means AND, false means OR

        while (command.startsWith(IF_PREFIX)) {
            int conditionEnd = command.indexOf('}');
            if (conditionEnd < 0) {
                warn("Invalid conditional syntax: " + command);
                return;
            }

            conditions.add(command.substring(IF_PREFIX.length(), conditionEnd).trim());
            command = command.substring(conditionEnd + 1).trim();

            if (command.startsWith("&&") || command.startsWith("||")) {
                operators.add(command.startsWith("&&"));
                String operator = command.substring(0, 2);
                command = command.substring(2).trim();
                if (!command.startsWith(IF_PREFIX)) {
                    warn("Expected another condition after '" + operator + "'");
                    return;
                }
            } else if (command.startsWith(IF_PREFIX)) {
                operators.add(true);
            } else {
                break;
            }
        }

        if (!evaluateConditions(player, conditions, operators) || command.isEmpty()) {
            return;
        }

        if (!command.startsWith("[")) {
            dispatchConsoleCommand(resolvePlaceholders(player, command, eventPlaceholders));
            return;
        }

        int actionEnd = command.indexOf(']');
        if (actionEnd < 0) {
            warn("Invalid action syntax: " + command);
            return;
        }

        String actionHeader = command.substring(1, actionEnd).trim();
        String actionCommand = command.substring(actionEnd + 1).trim();

        int filterStart = actionHeader.indexOf('(');
        if (filterStart >= 0) {
            int filterEnd = actionHeader.lastIndexOf(')');
            if (filterEnd < filterStart) {
                warn("Invalid action filter syntax: [" + actionHeader + "]");
                return;
            }
            String filter = resolvePlaceholders(player, actionHeader.substring(filterStart + 1, filterEnd).trim(), eventPlaceholders);
            if (!matchesLegacyFilter(player, filter)) {
                return;
            }
            actionHeader = actionHeader.substring(0, filterStart).trim();
        }

        int firstSpace = actionHeader.indexOf(' ');
        String action = (firstSpace < 0 ? actionHeader : actionHeader.substring(0, firstSpace)).toUpperCase(Locale.ROOT);
        String actionArguments = firstSpace < 0 ? "" : actionHeader.substring(firstSpace + 1).trim();

        executeAction(player, action, actionArguments, actionCommand, eventPlaceholders, componentPlaceholders);
    }

    private static boolean evaluateConditions(OfflinePlayer player, List<String> conditions, List<Boolean> operators) {
        if (conditions.isEmpty()) {
            return true;
        }

        boolean andGroup = true;
        for (int i = 0; i < conditions.size(); i++) {
            if (andGroup) {
                andGroup = evaluateCondition(player, conditions.get(i));
            }

            if (i == conditions.size() - 1 || !operators.get(i)) {
                if (andGroup) {
                    return true;
                }
                andGroup = true;
            }
        }
        return false;
    }

    private static boolean evaluateCondition(@Nullable OfflinePlayer player, @NotNull String condition) {
        if (player == null) {
            return false;
        }

        int colon = condition.indexOf(':');
        if (colon < 0) {
            warn("Invalid conditional format: " + condition + " (colon not found)");
            return false;
        }

        String type = condition.substring(0, colon).trim();
        String value = condition.substring(colon + 1).trim();

        if (type.equalsIgnoreCase("hasPermission") || type.equalsIgnoreCase("permission") || type.equalsIgnoreCase("perm")) {
            return player instanceof Player onlinePlayer && onlinePlayer.isOnline() && onlinePlayer.hasPermission(value);
        }
        if (type.equalsIgnoreCase("placeholder") || type.equalsIgnoreCase("ph")) {
            return evaluatePlaceholderCondition(player, value);
        }
        if (type.equalsIgnoreCase("score")) {
            return player instanceof Player onlinePlayer && onlinePlayer.isOnline()
                    && evaluateScoreCondition(onlinePlayer, value);
        }

        warn("Unknown condition type: " + type);
        return false;
    }

    private static boolean evaluatePlaceholderCondition(OfflinePlayer player, String condition) {
        for (String operator : COMPARISON_OPERATORS) {
            int operatorIndex = condition.indexOf(operator);
            if (operatorIndex < 0) {
                continue;
            }

            String actual = resolvePlaceholders(player, condition.substring(0, operatorIndex).trim());
            String expected = resolvePlaceholders(player, condition.substring(operatorIndex + operator.length()).trim());

            if (operator.equals("~~") || operator.equals("~=")) {
                return actual.contains(expected);
            }
            if (operator.equals("!~")) {
                return !actual.contains(expected);
            }

            try {
                double actualNumber = Double.parseDouble(actual);
                double expectedNumber = Double.parseDouble(expected);
                return compare(actualNumber, expectedNumber, operator);
            } catch (NumberFormatException ignored) {
                return switch (operator) {
                    case "==" -> actual.equals(expected);
                    case "!=" -> !actual.equals(expected);
                    default -> false;
                };
            }
        }

        String result = resolvePlaceholders(player, condition);
        return !result.isEmpty()
                && !result.equalsIgnoreCase("false")
                && !Pattern.compile("%[^%\\s]+%").matcher(result).find();
    }

    private static boolean compare(double left, double right, String operator) {
        return switch (operator) {
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case ">" -> left > right;
            case "<" -> left < right;
            case "==" -> left == right;
            case "!=" -> left != right;
            default -> false;
        };
    }

    private static boolean evaluateScoreCondition(Player player, String condition) {
        for (String operator : SCORE_OPERATORS) {
            int operatorIndex = condition.indexOf(operator);
            if (operatorIndex < 0) {
                continue;
            }

            String objectiveName = condition.substring(0, operatorIndex).trim();
            int expected;
            try {
                expected = Integer.parseInt(resolvePlaceholders(player, condition.substring(operatorIndex + operator.length()).trim()));
            } catch (NumberFormatException ex) {
                warn("Invalid score comparison value: " + condition.substring(operatorIndex + operator.length()).trim());
                return false;
            }

            return compareScore(player, objectiveName, expected, operator);
        }

        String[] scoreParts = condition.split(":", 2);
        if (scoreParts.length != 2) {
            warn("Invalid score condition format: " + condition);
            return false;
        }

        try {
            int required = Integer.parseInt(resolvePlaceholders(player, scoreParts[1].trim()));
            return compareScore(player, scoreParts[0].trim(), required, ">=");
        } catch (NumberFormatException ex) {
            warn("Invalid score value: " + condition);
            return false;
        }
    }

    private static boolean compareScore(Player player, String objectiveName, int expected, String operator) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return false;
        }

        Score score = objective.getScore(player.getName());
        if (!score.isScoreSet()) {
            return false;
        }
        return compare(score.getScore(), expected, operator);
    }

    private static void executeAction(OfflinePlayer player, String action, String arguments, String rawCommand,
                                      Map<String, String> eventPlaceholders,
                                      Map<String, Component> componentPlaceholders) {
        Player onlinePlayer = player instanceof Player target && target.isOnline() ? target : null;
        String command = resolvePlaceholders(player, rawCommand, eventPlaceholders);

        switch (action) {
            case "MESSAGE", "TEXT" -> {
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(TextTransformer.kyorify(rawCommand, player, eventPlaceholders, componentPlaceholders));
                }
            }
            case "TITLE" -> {
                if (onlinePlayer != null) {
                    parseTitle(onlinePlayer, rawCommand, player, eventPlaceholders, componentPlaceholders);
                }
            }
            case "ACTIONBAR" -> {
                if (onlinePlayer != null) {
                    onlinePlayer.sendActionBar(TextTransformer.kyorify(rawCommand, player, eventPlaceholders, componentPlaceholders));
                }
            }
            case "CHAT" -> {
                if (onlinePlayer != null) {
                    onlinePlayer.chat(command);
                }
            }
            case "SOUND" -> {
                if (onlinePlayer != null) {
                    parseSound(onlinePlayer, command);
                }
            }
            case "PARTICLE" -> {
                if (onlinePlayer != null) {
                    parseParticle(onlinePlayer, command);
                }
            }
            case "PLAYER" -> {
                if (onlinePlayer != null) {
                    dispatchPlayerCommand(onlinePlayer, command);
                }
            }
            case "OP" -> {
                if (onlinePlayer != null) {
                    runAsOperator(onlinePlayer, command);
                }
            }
            case "CONSOLE" -> dispatchConsoleCommand(command);
            case "BROADCAST" -> Bukkit.broadcast(TextTransformer.kyorify(rawCommand, player, eventPlaceholders, componentPlaceholders));
            case "LOG" -> logAction(arguments, command);
            default -> dispatchConsoleCommand(command);
        }
    }

    private static void runAsOperator(Player player, String command) {
        String normalized = normalizeCommandForDispatch(command);
        if (normalized.isBlank()) {
            return;
        }
        runOnMainThread(() -> {
            boolean wasOperator = player.isOp();
            try {
                if (!wasOperator) {
                    player.setOp(true);
                }
                dispatchPlayerCommandNow(player, normalized);
            } finally {
                if (!wasOperator) {
                    player.setOp(false);
                }
            }
        });
    }

    private static void logAction(String arguments, String command) {
        Matcher fileMatcher = Pattern.compile("<([^>]+)>").matcher(arguments);
        if (fileMatcher.find()) {
            Utils.logToFile(fileMatcher.group(1).toLowerCase(Locale.ROOT), command);
        } else {
            Bukkit.getConsoleSender().sendMessage(Utils.format("&8[&7SRV-Cron - CUSTOM-LOG&8]&r " + command));
        }
    }

    private static boolean matchesLegacyFilter(OfflinePlayer player, String filter) {
        if (player == null) {
            return Utils.matchesFilter(null, filter);
        }
        if (player instanceof Player onlinePlayer && onlinePlayer.isOnline()) {
            return Utils.matchesFilter(onlinePlayer, filter);
        }
        return false;
    }

    private static String resolvePlaceholders(@Nullable OfflinePlayer player, String input,
                                              Map<String, String> eventPlaceholders) {
        if (player != null && player.getName() != null) {
            input = input.replaceAll("(?i)%player_name%", Matcher.quoteReplacement(player.getName()));
        }
        if (player != null) {
            input = input.replaceAll("(?i)%player_uuid%", Matcher.quoteReplacement(player.getUniqueId().toString()));
        }
        if (Bukkit.getPluginManager() != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            input = PlaceholderAPI.setPlaceholders(player, input);
        }

        Matcher matcher = EVENT_VALUE_PATTERN.matcher(input);
        StringBuilder resolved = new StringBuilder(input.length());
        int lastEnd = 0;
        while (matcher.find()) {
            resolved.append(input, lastEnd, matcher.start());
            String key = matcher.group(1).trim();
            if (eventPlaceholders.containsKey(key)) {
                resolved.append(eventPlaceholders.get(key));
            } else {
                resolved.append(matcher.group());
            }
            lastEnd = matcher.end();
        }
        resolved.append(input, lastEnd, input.length());
        return resolved.toString();
    }

    private static String resolvePlaceholders(@Nullable OfflinePlayer player, String input) {
        return resolvePlaceholders(player, input, Map.of());
    }

    private static String normalizeCommandForDispatch(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        return normalizeLegacySelectorDistance(command);
    }

    private static void dispatchConsoleCommand(String rawCommand) {
        String command = normalizeCommandForDispatch(rawCommand);
        if (command.isBlank()) {
            return;
        }
        runOnMainThread(() -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (CommandException ex) {
                warn("Failed to execute console command: " + command + " (" + ex.getMessage() + ")");
            }
        });
    }

    private static void dispatchPlayerCommand(Player player, String rawCommand) {
        String command = normalizeCommandForDispatch(rawCommand);
        if (command.isBlank()) {
            return;
        }
        runOnMainThread(() -> dispatchPlayerCommandNow(player, command));
    }

    private static void dispatchPlayerCommandNow(Player player, String command) {
        try {
            Bukkit.dispatchCommand(player, command);
        } catch (CommandException ex) {
            warn("Failed to execute player command: " + command + " (" + ex.getMessage() + ")");
        }
    }

    private static void runOnMainThread(Runnable task) {
        SRVCron plugin = SRVCron.getInstance();
        if (plugin == null || Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private static String normalizeLegacySelectorDistance(String command) {
        Matcher matcher = LEGACY_SELECTOR_PATTERN.matcher(command);
        StringBuilder normalized = new StringBuilder(command.length());
        while (matcher.find()) {
            String selector = matcher.group(1);
            String arguments = matcher.group(2);
            if (!arguments.contains("r=") && !arguments.contains("rm=")) {
                continue;
            }
            matcher.appendReplacement(normalized, Matcher.quoteReplacement("@" + selector + "[" + rewriteLegacyRadiusArgs(arguments) + "]"));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static String rewriteLegacyRadiusArgs(String selectorArgs) {
        String[] parts = selectorArgs.split(",");
        Map<String, String> arguments = new LinkedHashMap<>();
        for (String part : parts) {
            String trimmed = part.trim();
            int equals = trimmed.indexOf('=');
            if (equals >= 0) {
                arguments.put(trimmed.substring(0, equals).trim().toLowerCase(Locale.ROOT), trimmed.substring(equals + 1).trim());
            }
        }

        if (arguments.containsKey("distance")) {
            return selectorArgs;
        }
        String minimum = arguments.remove("rm");
        String maximum = arguments.remove("r");
        if (minimum != null || maximum != null) {
            arguments.put("distance", (minimum == null ? "" : minimum) + ".." + (maximum == null ? "" : maximum));
        }
        return arguments.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse(selectorArgs);
    }

    private static void parseParticle(Player player, String command) {
        String[] arguments = command.split("\\s*,\\s*");
        if (arguments.length != 5) {
            warn("Invalid particle format: " + command);
            return;
        }
        try {
            Particle particle = Particle.valueOf(arguments[0].toUpperCase(Locale.ROOT));
            int count = Integer.parseInt(arguments[1]);
            double offsetX = Double.parseDouble(arguments[2]);
            double offsetY = Double.parseDouble(arguments[3]);
            double offsetZ = Double.parseDouble(arguments[4]);
            player.spawnParticle(particle, player.getLocation(), count, offsetX, offsetY, offsetZ);
        } catch (IllegalArgumentException ex) {
            warn("Invalid particle format: " + command);
        }
    }

    private static void parseTitle(Player player, String command, OfflinePlayer placeholderPlayer,
                                   Map<String, String> eventPlaceholders,
                                   Map<String, Component> componentPlaceholders) {
        String[] arguments = command.split("\\s*,\\s*");
        String titleText;
        String subtitleText = "";
        int fadeIn = 20;
        int stay = 60;
        int fadeOut = 20;

        try {
            if (arguments.length == 1) {
                titleText = arguments[0];
            } else if (arguments.length == 2) {
                titleText = arguments[0];
                subtitleText = arguments[1];
            } else if (arguments.length == 4) {
                titleText = arguments[0];
                fadeIn = Integer.parseInt(arguments[1]);
                stay = Integer.parseInt(arguments[2]);
                fadeOut = Integer.parseInt(arguments[3]);
            } else if (arguments.length == 5) {
                titleText = arguments[0];
                subtitleText = arguments[1];
                fadeIn = Integer.parseInt(arguments[2]);
                stay = Integer.parseInt(arguments[3]);
                fadeOut = Integer.parseInt(arguments[4]);
            } else {
                warn("Invalid title format: " + command);
                return;
            }
        } catch (NumberFormatException ex) {
            warn("Invalid fade-in, stay, or fade-out time for title action.");
            return;
        }

        Title.Times times = Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut));
        Component title = TextTransformer.kyorify(titleText, placeholderPlayer, eventPlaceholders, componentPlaceholders);
        Component subtitle = subtitleText.isEmpty() ? Component.empty()
                : TextTransformer.kyorify(subtitleText, placeholderPlayer, eventPlaceholders, componentPlaceholders);
        player.showTitle(Title.title(title, subtitle, times));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private static void parseSound(Player player, String command) {
        String[] arguments = command.split("\\s*,\\s*");
        if (arguments.length != 3) {
            warn("Invalid sound format: " + command);
            return;
        }
        try {
            Sound sound = resolveSound(arguments[0]);
            if (sound == null) {
                warn("Unknown sound: " + arguments[0]);
                return;
            }
            float volume = Float.parseFloat(arguments[1]);
            float pitch = Float.parseFloat(arguments[2]);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            warn("Invalid sound format: " + command);
        }
    }

    @Nullable
    private static Sound resolveSound(String input) {
        String name = input.trim();
        if (name.contains(":") || name.contains(".")) {
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
            if (key != null) {
                Sound registered = Registry.SOUND_EVENT.get(key);
                if (registered != null) {
                    return registered;
                }
            }
        }

        try {
            Field legacyConstant = Sound.class.getField(name.toUpperCase(Locale.ROOT));
            return (Sound) legacyConstant.get(null);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private static void warn(String message) {
        SRVCron plugin = SRVCron.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(message);
        } else {
            Bukkit.getLogger().warning(message);
        }
    }
}
