package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.job.EventJobContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves read-only event property paths and snapshots their values as strings. */
public final class EventPropertyResolver {
    private static final Pattern PATH_PARTS = Pattern.compile("([^\\.\\[\\]]+)|\\[(\\d+)\\]");
    private static final Pattern EVENT_PLACEHOLDER = Pattern.compile("\\{event\\.([^{}]+)}");
    private static final Map<Class<?>, Map<String, Method>> ACCESSORS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private EventPropertyResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static EventJobContext capture(
            @Nullable Event event,
            @Nullable String playerPath,
            @Nullable String worldPath,
            Map<String, String> placeholderPaths,
            List<String> commands
    ) {
        Player player = resolvePlayer(event, playerPath);
        World world = resolveWorld(event, worldPath);
        if (world == null && player != null) {
            world = player.getWorld();
        }

        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Component> componentValues = new LinkedHashMap<>();
        if (player != null) {
            values.put("player_name", player.getName());
        }
        if (world != null) {
            values.put("world_name", world.getName());
        }

        for (Map.Entry<String, String> alias : placeholderPaths.entrySet()) {
            Object value = resolvePath(event, alias.getValue());
            values.put(alias.getKey(), stringify(value));
            if (value instanceof Component component) {
                componentValues.put(alias.getKey(), component);
            }
        }

        for (String command : commands) {
            Matcher matcher = EVENT_PLACEHOLDER.matcher(command);
            while (matcher.find()) {
                String path = matcher.group(1).trim();
                String key = "event." + path;
                Object value = resolvePath(event, path);
                values.putIfAbsent(key, stringify(value));
                if (value instanceof Component component) {
                    componentValues.putIfAbsent(key, component);
                }
            }
        }

        return new EventJobContext(event, player, world, values, componentValues);
    }

    public static EventJobContext captureLegacy(
            @Nullable Event event,
            @Nullable Player player,
            @Nullable World world,
            Map<String, String> placeholders
    ) {
        return captureLegacy(event, player, world, placeholders, Map.of());
    }

    public static EventJobContext captureLegacy(
            @Nullable Event event,
            @Nullable Player player,
            @Nullable World world,
            Map<String, String> placeholders,
            Map<String, Component> componentPlaceholders
    ) {
        Map<String, String> values = new LinkedHashMap<>(placeholders);
        if (player != null) {
            values.put("player_name", player.getName());
        }
        if (world != null) {
            values.put("world_name", world.getName());
        }
        return new EventJobContext(event, player, world, values, componentPlaceholders);
    }

    @Nullable
    public static Object resolvePath(@Nullable Object root, @Nullable String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }

        String normalizedPath = path.trim();
        if (normalizedPath.startsWith("event.")) {
            normalizedPath = normalizedPath.substring("event.".length());
        }

        Matcher matcher = PATH_PARTS.matcher(normalizedPath);
        Object current = root;
        int lastEnd = 0;
        boolean foundPart = false;
        while (matcher.find()) {
            String separator = normalizedPath.substring(lastEnd, matcher.start());
            if (!separator.isEmpty() && !separator.equals(".")) {
                return null;
            }
            foundPart = true;
            String property = matcher.group(1);
            String indexText = matcher.group(2);
            if (property != null) {
                current = invokeAccessor(current, property, normalizedPath);
            } else {
                current = resolveIndex(current, indexText, normalizedPath);
            }
            if (current == null) {
                return null;
            }
            lastEnd = matcher.end();
        }
        return foundPart && lastEnd == normalizedPath.length() ? current : null;
    }

    public static String stringify(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Component component) {
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        if (value instanceof Player player) {
            return player.getName();
        }
        if (value instanceof World world) {
            return world.getName();
        }
        if (value instanceof Entity entity) {
            return entity.getType().name();
        }
        if (value instanceof ItemStack itemStack) {
            return itemStack.getType().name() + " x" + itemStack.getAmount();
        }
        if (value instanceof Location location) {
            String world = location.getWorld() == null ? "" : location.getWorld().getName() + ",";
            return world + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        }
        if (value instanceof NamespacedKey key) {
            return key.asString();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(EventPropertyResolver::stringify).reduce((a, b) -> a + ", " + b).orElse("");
        }
        if (value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) {
                values.add(stringify(Array.get(value, index)));
            }
            return String.join(", ", values);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof UUID || value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return value.toString();
    }

    @Nullable
    private static Player resolvePlayer(@Nullable Event event, @Nullable String configuredPath) {
        if (event == null) {
            return null;
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            Object value = resolvePath(event, configuredPath);
            if (value instanceof Player player) {
                return player;
            }
            warnOnce(event.getClass(), "context.player:" + configuredPath,
                    "Configured player context path '" + configuredPath + "' did not resolve to a Player.");
            return null;
        }
        List<Player> candidates = directValues(event, Player.class);
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        if (candidates.size() > 1) {
            warnOnce(event.getClass(), "context.player:ambiguous",
                    "Multiple Player values found in " + event.getClass().getName() + "; configure context.player to select one.");
        }
        return null;
    }

    @Nullable
    private static World resolveWorld(@Nullable Event event, @Nullable String configuredPath) {
        if (event == null) {
            return null;
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            Object value = resolvePath(event, configuredPath);
            if (value instanceof World world) {
                return world;
            }
            warnOnce(event.getClass(), "context.world:" + configuredPath,
                    "Configured world context path '" + configuredPath + "' did not resolve to a World.");
            return null;
        }
        List<World> candidates = directValues(event, World.class);
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        if (candidates.size() > 1) {
            warnOnce(event.getClass(), "context.world:ambiguous",
                    "Multiple World values found in " + event.getClass().getName() + "; configure context.world to select one.");
        }
        return null;
    }

    private static <T> List<T> directValues(Event event, Class<T> expectedType) {
        List<T> values = new ArrayList<>();
        IdentityHashMap<T, Boolean> unique = new IdentityHashMap<>();
        for (Method method : event.getClass().getMethods()) {
            if (!isAccessor(method) || method.getDeclaringClass() == Object.class
                    || !mayReturnContext(method.getReturnType(), expectedType)) {
                continue;
            }
            try {
                Object value = method.invoke(event);
                if (expectedType.isInstance(value)) {
                    T candidate = expectedType.cast(value);
                    if (unique.put(candidate, Boolean.TRUE) == null) {
                        values.add(candidate);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
                // A getter unrelated to the requested context is allowed to fail.
            }
        }
        return values;
    }

    private static boolean mayReturnContext(Class<?> returnType, Class<?> expectedType) {
        if (expectedType == Player.class) {
            return Player.class.isAssignableFrom(returnType)
                    || Entity.class.isAssignableFrom(returnType)
                    || HumanEntity.class.isAssignableFrom(returnType);
        }
        return expectedType.isAssignableFrom(returnType);
    }

    @Nullable
    private static Object invokeAccessor(@Nullable Object target, String property, String path) {
        if (target == null) {
            return null;
        }
        Method accessor = ACCESSORS.computeIfAbsent(target.getClass(), EventPropertyResolver::findAccessors).get(property);
        if (accessor == null) {
            warnOnce(target.getClass(), "path:" + path,
                    "No public zero-argument getter for event property path '" + path + "' on " + target.getClass().getName() + ".");
            return null;
        }
        try {
            return accessor.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            warnOnce(target.getClass(), "path:" + path,
                    "Could not read event property path '" + path + "' on " + target.getClass().getName() + ".");
            return null;
        }
    }

    @Nullable
    private static Object resolveIndex(@Nullable Object target, String indexText, String path) {
        if (target == null) {
            return null;
        }
        try {
            int index = Integer.parseInt(indexText);
            if (target instanceof List<?> list) {
                return index >= 0 && index < list.size() ? list.get(index) : null;
            }
            if (target.getClass().isArray()) {
                return index >= 0 && index < Array.getLength(target) ? Array.get(target, index) : null;
            }
        } catch (NumberFormatException ignored) {
            // The path parser only accepts numeric indexes.
        }
        warnOnce(target.getClass(), "index:" + path,
                "Event property path '" + path + "' uses an index on a value that is not an array or List.");
        return null;
    }

    private static Map<String, Method> findAccessors(Class<?> type) {
        Map<String, Method> methods = new LinkedHashMap<>();
        List<Method> publicMethods = new ArrayList<>(List.of(type.getMethods()));
        publicMethods.sort(Comparator.comparing(Method::getName));
        for (Method method : publicMethods) {
            if (!isAccessor(method)) {
                continue;
            }
            String name = method.getName();
            String suffix = name.startsWith("get") ? name.substring(3) : name.substring(2);
            if (!suffix.isEmpty()) {
                methods.putIfAbsent(Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1), method);
            }
        }
        return methods;
    }

    private static boolean isAccessor(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0 || method.getReturnType() == void.class) {
            return false;
        }
        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3 && !name.equals("getClass"))
                || (name.startsWith("is") && name.length() > 2);
    }

    private static void warnOnce(Class<?> type, String key, String message) {
        if (WARNED.add(type.getName() + ":" + key)) {
            org.bukkit.Bukkit.getLogger().warning("[SRV-Cron] " + message);
        }
    }
}
