package me.m0dii.srvcron.utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScheduleCalculator {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ScheduleCalculator() {
        // Utility class, no instantiation
    }

    /**
     * Calculate the next N runs for a cron job based on its time configuration.
     *
     * @param timeConfig The time configuration string (e.g., "every 1 hour", "every 4 day of week at 00:00")
     * @param count      The number of future runs to calculate
     * @return List of formatted date strings representing the next scheduled runs
     */
    public static List<String> getNextRuns(String timeConfig, int count) {
        List<String> runs = new ArrayList<>();

        try {
            TimeExpression expression = TimeExpression.parse(timeConfig);
            for (var run : expression.nextRuns(count, Instant.now())) {
                runs.add(run.format(DATE_FMT));
            }
        } catch (Exception e) {
            runs.add("Failed to calculate schedule: " + e.getMessage());
        }

        return runs;
    }

    public static String explain(String timeConfig) {
        try {
            return TimeExpression.parse(timeConfig).describe();
        } catch (Exception e) {
            return "Invalid expression: " + e.getMessage();
        }
    }
}

