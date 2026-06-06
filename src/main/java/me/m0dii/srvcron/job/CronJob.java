package me.m0dii.srvcron.job;

import lombok.Getter;
import me.m0dii.srvcron.SRVCron;
import me.m0dii.srvcron.managers.CronJobDispatchEvent;
import me.m0dii.srvcron.utils.TimeExpression;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CronJob {
    private final SRVCron srvCron;
    @Getter
    private final List<String> commands;
    @Getter
    private final String time;
    @Getter
    private final String name;
    private TimeExpression schedule;
    private long lastTickSecond = -1;
    private BukkitTask task;

    @Getter
    private boolean suspended = false;
    @Getter
    private int runCount = 0;

    public CronJob(SRVCron srvCronPlugin, List<String> commands, String time, String name) {
        this.srvCron = srvCronPlugin;
        this.commands = commands;
        this.time = time;
        this.name = name;
    }

    public void startJob() throws IllegalArgumentException {
        schedule = TimeExpression.parse(time);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                long epochSecond = Instant.now().getEpochSecond();
                if (epochSecond == lastTickSecond) {
                    return;
                }
                lastTickSecond = epochSecond;

                if (!schedule.shouldRunAt(Instant.ofEpochSecond(epochSecond))) {
                    return;
                }

                int jitterSeconds = schedule.jitterSeconds();
                if (jitterSeconds <= 0) {
                    runCommands();
                    return;
                }

                int delay = ThreadLocalRandom.current().nextInt(jitterSeconds + 1);
                Bukkit.getScheduler().runTaskLater(srvCron, this::safeRunCommands, delay * 20L);
            }

            private void safeRunCommands() {
                if (!suspended) {
                    runCommands();
                }
            }
        }.runTaskTimer(srvCron, 0, 20);
    }

    public void stopJob() {
        if (task != null) {
            task.cancel();
        }
    }

    public void runCommands() {
        Bukkit.getPluginManager().callEvent(new CronJobDispatchEvent(this));
    }

    public void increaseRunCount() {
        runCount++;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        suspended = false;
    }

    public List<String> getNextRuns(int count) {
        TimeExpression expr = schedule != null ? schedule : TimeExpression.parse(time);
        List<String> out = new ArrayList<>();
        for (var next : expr.nextRuns(count, Instant.now())) {
            out.add(next.toLocalDateTime().toString().replace('T', ' '));
        }
        return out;
    }
}
