package me.m0dii.srvcron.utils;

import me.m0dii.srvcron.SRVCron;

public class LangConfig extends AbstractConfig
{
    private final SRVCron plugin;
    
    public LangConfig(SRVCron plugin)
    {
        super(plugin.getDataFolder(), "messages.yml", plugin);
        
        this.plugin = plugin;
    }
    
    public String getConfigReloaded()
    {
        return getString("config-reloaded");
    }
    
    public String getNoPermissionCmd()
    {
        return getString("no-permission-command");
    }
    
    public String getUsage(String type)
    {
        return getString("usage." + type);
    }
    
    public String getJobDoesNotExist()
    {
        return getString("job-does-not-exist");
    }
    
    public String getJobNotSuspended()
    {
        return getString("job-not-suspended");
    }
    
    public String getJobResumed()
    {
        return getString("job-resumed");
    }
    
    public String getJobSuspended()
    {
        return getString("job-suspended");
    }
    
    public String getJobIsSuspended()
    {
        return getString("job-is-suspended");
    }
    
    public String getEventJobExecuted()
    {
        return getString("event-job-executed");
    }
    
    public String getJobDispatched()
    {
        return getString("job-dispatched");
    }
}