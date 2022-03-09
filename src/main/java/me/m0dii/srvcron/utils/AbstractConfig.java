package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.SRVCron;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class AbstractConfig
{
    private final SRVCron plugin;
    private final File file;
    
    FileConfiguration config;
    
    protected AbstractConfig(File parentFile, String configName, SRVCron plugin)
    {
        this.plugin = plugin;
        
        file = new File(parentFile, configName);
        
        loadConfig();
    }
    
    protected FileConfiguration loadConfig()
    {
        config = createConfig();
        
        saveConfig();
        
        return config;
    }
    
    protected void saveConfig()
    {
        try
        {
            config.save(file);
        }
        catch (IOException ex)
        {
            plugin.log("Error while saving " + file.getName() + ".");
        }
    }
    
    public void reloadConfig()
    {
        config = loadConfig();
    }
    
    protected void deleteConfig()
    {
        if(file.exists())
        {
            file.delete();
        }
    }
    
    protected FileConfiguration createConfig()
    {
        if(!file.exists())
        {
            file.getParentFile().mkdirs();
            
            plugin.saveResource("lang" + File.separator + file.getName(), false);
        }
        
        FileConfiguration config = new YamlConfiguration();
        
        try
        {
            config.load(file);
    
            plugin.log("Successfully loaded " + file.getName() + ".");
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            plugin.log("Error while loading " + file.getName() + ".");
        }
        
        return config;
    }
    
    protected String getString(String path)
    {
        return Utils.format(config.getString(path));
    }
}