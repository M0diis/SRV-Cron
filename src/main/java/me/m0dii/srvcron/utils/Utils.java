package me.m0dii.srvcron.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.m0dii.srvcron.SRVCron;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Calendar;

public class Utils
{
    private static SRVCron plugin = SRVCron.getInstance();
    
    public static String parsePlaceholder(String str, Player p)
    {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
        {
            str = PlaceholderAPI.setPlaceholders(p, str);
        }
    
        if(p != null)
        {
            str = str.replace("%player_name%", p.getName());
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
    
    public static void sendCommand(Player sender, String cmd)
    {
        cmd = parsePlaceholder(cmd, sender);
        
        if(cmd.startsWith("["))
        {
            String sendAs = cmd.substring(cmd.indexOf("["), cmd.indexOf("]") + 1).toUpperCase();
            
            cmd = cmd.substring(cmd.indexOf("]") + 2);
            
            if(sendAs.equalsIgnoreCase("[MESSAGE]") || sendAs.equalsIgnoreCase("[TEXT]"))
            {
                sender.sendMessage(cmd);
            }
            
            if(sendAs.equalsIgnoreCase("[TITLE]"))
            {
                String[] split = cmd.split(", ");
                
                int fadeIn = 20;
                int stay = 60;
                int fadeOut = 20;
                
                switch(split.length)
                {
                    case 1:
                        sender.sendTitle(split[0], "", fadeIn, stay, fadeOut);
                    break;
                    case 2:
                        sender.sendTitle(split[0], split[1], fadeIn, stay, fadeOut);
                    break;
                    case 4:
                        try
                        {
                            fadeIn = Integer.parseInt(split[1]);
                            stay = Integer.parseInt(split[2]);
                            fadeOut = Integer.parseInt(split[3]);
                        }
                        catch(NumberFormatException ex)
                        {
                            plugin.log("Invalid fadeIn, stay, or fadeOut time for title action.");
                        }
                        
                        sender.sendTitle(split[0], "", fadeIn, stay, fadeOut);
                    break;
                    case 5:
                        String subtitle = split[1];
                        try
                        {
                            fadeIn = Integer.parseInt(split[2]);
                            stay = Integer.parseInt(split[3]);
                            fadeOut = Integer.parseInt(split[4]);
                        }
                        catch(NumberFormatException ex)
                        {
                            plugin.log("Invalid fadeIn, stay, or fadeOut time for title action.");
                        }
                        sender.sendTitle(split[0], subtitle, fadeIn, stay, fadeOut);
                    break;
                }
            }
            
            if(sendAs.equalsIgnoreCase("[CHAT]"))
            {
                sender.chat(cmd);
            }
            
            if(sendAs.equalsIgnoreCase("[SOUND]"))
            {
                String[] split = cmd.split(", ");
                
                if(split.length == 2)
                {
                    try
                    {
                        sender.playSound(sender.getLocation(), Sound.valueOf(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[1]));
                    }
                    catch (Exception ex)
                    {
                        plugin.log("Invalid sound format: " + cmd);
                    }
                }
            }
            if(sendAs.startsWith("[PLAYER]"))
                Bukkit.dispatchCommand(sender, cmd);
            if(sendAs.startsWith("[CONSOLE]"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        else Bukkit.dispatchCommand(sender, cmd);
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
