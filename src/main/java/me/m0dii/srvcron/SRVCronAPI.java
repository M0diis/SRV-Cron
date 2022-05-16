package me.m0dii.srvcron;

import me.m0dii.srvcron.job.CronJob;
import me.m0dii.srvcron.job.EventJob;
import me.m0dii.srvcron.utils.EventType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class SRVCronAPI
{
    private final SRVCron plugin;
    
    public SRVCronAPI(SRVCron plugin)
    {
        this.plugin = plugin;
    }
    
    public CronJob getCronJob(String name)
    {
        return plugin.getJobs().getOrDefault(name, null);
    }
    
    public void runCronJob(CronJob job)
    {
        job.startJob();
    }
    
    public void stopCronJob(CronJob job)
    {
        job.stopJob();
    }
    
    public void runCronJobCommands(CronJob job)
    {
        job.runCommands();
    }
    
    public List<EventJob> getEventJobsByType(EventType type)
    {
        return plugin.getEventJobs().getOrDefault(type, null);
    }
    
    public EventJob getEventJobByName(String jobName)
    {
        return plugin.getEventJobs().values().stream()
                .flatMap(List::stream)
                .filter(job -> job.getName().equals(jobName))
                .findFirst().orElse(null);
    }
    
    public void runEventJob(EventJob job, Player player, World world)
    {
        job.performJob(player, world, null);
    }
}
