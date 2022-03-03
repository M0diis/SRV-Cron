package me.m0dii.srvcron.job;

import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.managers.CronJobDispatchEvent;
import me.m0dii.srvcron.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class CronJob
{
    private final SRVCron SRVCron;
    private final List<String> cmds;
    private final String time;
    private final String name;
    private Calendar cal;
    private int t = 0;
    private int calDayMonth = 0;
    private int calDayWeek = 0;
    private String clockTime = "";
    private BukkitTask task;
    
    private boolean suspended = false;
    private int runCount = 0;
    
    public CronJob(SRVCron SRVCron, List<String> cmds, String time, String name)
    {
        this.SRVCron = SRVCron;
        this.cmds = cmds;
        this.time = time;
        this.name = name;
    }
    
    public void startJob() throws IllegalArgumentException
    {
        getTimer();
        
        task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                t--;
                if(cal != null)
                {
                    if(t <= 0)
                    {
                        getTimer();
                        
                        if(!Objects.equals(clockTime, "") && !Utils.isTime(clockTime))
                        {
                            return;
                        }
                        
                        if(calDayMonth != 0 && cal.get(Calendar.DAY_OF_MONTH) == calDayMonth)
                        {
                            runCommands();
                        }
                        else if(calDayWeek != 0 && cal.get(Calendar.DAY_OF_WEEK) == calDayWeek)
                        {
                            runCommands();
                        }
                    }
                    
                    return;
                }
                
                if(!Objects.equals(clockTime, "") && !Utils.isTime(clockTime))
                {
                    return;
                }
                
                if(t <= 0)
                {
                    runCommands();
                    getTimer();
                }
            }
        }.runTaskTimer(SRVCron, 0, 20);
    }
    
    public void stopJob()
    {
        if(task != null) task.cancel();
    }
    
    private void getTimer() throws IllegalArgumentException
    {
        String[] args = time.split(" ");
        
        if(args.length >= 5)
        {
            if(args.length == 7)
            {
                if(args[5].contains("at"))
                {
                    clockTime = args[6];
                }
            }
            
            cal = Calendar.getInstance();
            
            if(args[2].contains("day") && args[4].contains("month"))
            {
                calDayMonth = Integer.parseInt(args[1]);
                t = Objects.equals(clockTime, "") ? ((20 * 60) * 60 ) : 61;
            }
            else if(args[2].contains("day") && args[4].contains("week"))
            {
                calDayWeek = Integer.parseInt(args[1]) + 1;
                t = Objects.equals(clockTime, "") ? ((20 * 60) * 60 ) : 61;
            }
            else
            {
                throw new IllegalArgumentException("Invalid Time format: '" + time + "'");
            }
        }
        else if(args.length == 3)
        {
            if(args[2].contains("second"))
            {
                t = Integer.parseInt(args[1]);
            }
            else if(args[2].contains("minute"))
            {
                t = Integer.parseInt(args[1]) * 60;
            }
            else if(args[2].contains("hour"))
            {
                t = (Integer.parseInt(args[1]) * 60) * 60;
            }
            else if(args[2].contains("day") || args[2].contains("days"))
            {
                t = ((Integer.parseInt(args[1]) * 60) * 60) * 24;
            }
            else{
                
                throw new IllegalArgumentException("Invalid Time format: '" + time + "'");
            }
        }
        else if(args.length == 2 || time.startsWith("every day at"))
        {
            String[] smArgs = time.replace("every day at", "at").split(" ");
            
            if(smArgs[0].contains("at"))
            {
                clockTime = smArgs[1];
                t = 61;
                
                return;
            }
            
            if(smArgs[1].contains("second"))
            {
                t = 1;
            }
            else if(smArgs[1].contains("minute"))
            {
                t = 60;
            }
            else if(smArgs[1].contains("hour"))
            {
                t = 60 * 60;
            }
            else if(smArgs[1].contains("day") || smArgs[1].contains("days"))
            {
                t = (60 * 60) * 24;
            }
            else
            {
                throw new IllegalArgumentException("Invalid Time format: '" + time + "'");
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid Time format: '" + time + "'");
        }
    }
    
    public void runCommands()
    {
        Bukkit.getPluginManager().callEvent(new CronJobDispatchEvent(this));
    }
    
    public String getName()
    {
        return name;
    }

    public String getTime()
    {
        return time;
    }

    public List<String> getCommands()
    {
        return cmds;
    }
    
    public int getRunCount()
    {
        return runCount;
    }

    public void increaseRunCount()
    {
        runCount++;
    }
    
    public void suspend()
    {
        suspended = true;
    }
    
    public void resume()
    {
        suspended = false;
    }
    
    public boolean isSuspended()
    {
        return suspended;
    }
}
