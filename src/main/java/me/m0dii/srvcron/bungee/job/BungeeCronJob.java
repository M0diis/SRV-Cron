package me.m0dii.srvcron.bungee.job;

import lombok.Getter;
import me.m0dii.srvcron.bungee.BungeeSRVCron;
import me.m0dii.srvcron.utils.TimeExpression;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BungeeCronJob {

    private final BungeeSRVCron cron;
    @Getter
    private final List<String> commands;
    @Getter
    private final String time;
    @Getter
    private final String name;
    private TimeExpression schedule;
    private long lastTickSecond = -1;
    private ScheduledTask task;

    public BungeeCronJob(BungeeSRVCron cron, List<String> commands, String time, String name) {
        this.cron = cron;
        this.commands = commands;
        this.time = time;
        this.name = name;
    }

    public void startJob() throws IllegalArgumentException {
        schedule = TimeExpression.parse(time);

        task = cron.getProxy().getScheduler().schedule(cron, () ->
        {
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
            cron.getProxy().getScheduler().schedule(cron, this::runCommands, delay, TimeUnit.SECONDS);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopJob() {
        if (task != null) {
            task.cancel();
        }
    }

    public void runCommands() {
        for (String s : new ArrayList<>(commands)) {
            cron.getProxy().getPluginManager().dispatchCommand(
                    cron.getProxy().getConsole(), s);
        }
    }

}
