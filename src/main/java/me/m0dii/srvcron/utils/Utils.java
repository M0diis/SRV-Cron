package me.m0dii.srvcron.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.m0dii.srvcron.SRVCron;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Calendar;

public class Utils
{
    public static String parsePlaceholder(String str, Player p)
    {
        if (SRVCron.getInstance().getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
        {
            str = PlaceholderAPI.setPlaceholders(p, str);
        }
    
        if(p != null)
        {
            str = str.replace("{player}", p.getName());
        }
        
        return str;
    }
    
    public static String parsePlaceholder(String str)
    {
        return parsePlaceholder(str, null);
    }

    public static String format(String text)
    {
        if(text == null || text.isEmpty())
        {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static boolean isTime(String clockTime)
    {
        String[] args = clockTime.split(":");
        Calendar c = Calendar.getInstance();
        String hour, minute;
    
        hour = c.get(Calendar.HOUR_OF_DAY) + "";
        minute = c.get(Calendar.MINUTE) + "";
    
        String cHour = args[0];
        String cMinute = args[1];
    
        if(args[0].startsWith("0") && args[0].length() == 2)
        {
            cHour = args[0].substring(1);
        }
    
        if(args[1].startsWith("0") && args[1].length() == 2)
        {
            cMinute = args[1].substring(1);
        }
    
        return cMinute.equalsIgnoreCase(minute) && cHour.equalsIgnoreCase(hour);
    }
}
