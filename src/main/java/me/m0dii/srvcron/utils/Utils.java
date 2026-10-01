package me.m0dii.srvcron.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.m0dii.srvcron.SRVCron;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {
    private static SRVCron plugin() {
        return SRVCron.getInstance();
    }

    private static boolean isPlaceholderApiEnabled() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private static String applyPlaceholderApi(Player player, String text) {
        if (!isPlaceholderApiEnabled()) {
            return text;
        }

        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static String handleDispatcherPlaceholders(String str, Player p) {
        StringBuilder result = new StringBuilder();

        String[] split = str.replaceAll("\\{player_name}", p.getName()).split(" ");

        for (String s : split) {
            if (s.startsWith("{") && s.endsWith("}")) {
                String placeholder = s.replaceAll("[{}]", "%");
                result.append(applyPlaceholderApi(p, placeholder));
            } else {
                result.append(s);
            }

            result.append(" ");
        }

        return result.toString().trim();
    }

    public static String setPlaceholders(String str, Player p) {
        debug("Setting placeholders in: " + str + " for player: " + (p == null ? "null" : p.getName()));

        str = str.trim();

        if (p != null) {
            str = str.replace("%player_name%", p.getName());
        }

        if (isPlaceholderApiEnabled()) {
            str = applyPlaceholderApi(p, str);
        }

        debug("Text after setting placeholders: " + str);

        return str;
    }

    public static String setPlaceholders(String str) {
        return setPlaceholders(str, null);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("(?<!&)#([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])([A-Fa-f0-9])");

    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String formattedHex = HEX_PATTERN.matcher(text).replaceAll("&#$1$2$3$4$5$6");
        return LegacyComponentSerializer.legacySection().serialize(
                TextTransformer.kyorifyWithoutPlaceholderExpansion(formattedHex)
        );
    }

    public static final Map<String, List<String>> messagesByFile = new HashMap<>();

    public static void logToFile(String file, String text) {
//        List<String> messages = messagesByFile.getOrDefault(file, new ArrayList<>());
//
//        if(messages.size() <= 10)
//        {
//            messages.add(text);
//
//            messagesByFile.put(file, messages);
//
//            return;
//        }

        try {
            SRVCron plugin = plugin();

            if (plugin == null) {
                return;
            }

            File logFolder = plugin.getDataFolder();

            if (!logFolder.exists()) {
                logFolder.mkdir();
            }

            File saveTo = new File(plugin.getDataFolder(), file);

            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

//            for(String s : messages)
//            {
//                pw.println("[" + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()) + "] " + s);
//            }

            pw.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + text.trim());

            pw.flush();
            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void sendCommand(Player onlinePlayer, String cmd) {
        debug("Dispatching command: " + cmd);
        CommandActionParser.parse(onlinePlayer, cmd);
    }

    public static void debug(String message) {
        SRVCron plugin = plugin();

        if (plugin != null && plugin.getConfig().getBoolean("debug")) {
            logToFile("debug.log", message);

            String prefix = "&3[&bSRV-Cron - DEBUG&3]&r ";

            Bukkit.getConsoleSender().sendMessage(format(prefix + message));
        }
    }

    public static boolean matchesFilter(Player p, String cond) {
        if (cond.toUpperCase().startsWith("PERM") && cond.contains(":")) {
            if (p == null) {
                return true;
            }

            debug("Checking permission condition '" + cond + "' for player '" + p.getName() + "'");

            String permission = cond.split(":")[1];

            return p.hasPermission(permission);
        }

        List<String> condSplit = Arrays.asList(cond.split(" "));

        if (condSplit.size() == 3) {
            String op = condSplit.get(1);

            try {
                String leftStr = condSplit.get(0);
                String rightStr = condSplit.get(2);

                debug("Checking condition pre-parse: " + leftStr + " " + op + " " + rightStr);

                if (Objects.equals(op, "=") || Objects.equals(op, "==")) {
                    if (!Utils.isDigit(leftStr) && !Utils.isDigit(rightStr)) {
                        if (leftStr.equalsIgnoreCase(rightStr)) {
                            return true;
                        }
                    }
                }

                double left = Double.parseDouble(applyPlaceholderApi(p, leftStr)
                        .replaceAll("[a-zA-Z!@#$&*()/\\\\\\[\\]{}:\"?]", ""));

                double right = Double.parseDouble(applyPlaceholderApi(p, rightStr)
                        .replaceAll("[a-zA-Z!@#$&*()/\\\\\\[\\]{}:\"?]", ""));

                debug("Checking condition after parse: " + leftStr + " " + op + " " + rightStr);

                return switch (op) {
                    case ">", "greater_than" -> left > right;
                    case "<", "less_than" -> left < right;
                    case "<=", "less_than_or_equals", "less_or_equals" -> left <= right;
                    case ">=", "greater_or_equals", "greater_than_or_equals" -> left >= right;
                    case "!=", "not_equals" -> left != right;
                    case "==", "=", "equals" -> left == right;
                    default -> false;
                };
            } catch (NumberFormatException ex) {
                SRVCron plugin = plugin();

                if (plugin != null) {
                    plugin.log("Failed to parse the condition: " + cond);
                }
            }

            return false;
        } else {
            String result = applyPlaceholderApi(p, cond).toLowerCase();

            return result.equals("yes") || result.equals("true");
        }
    }

    public static boolean isDigit(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }
}
