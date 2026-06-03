package me.m0dii.srvcron.utils;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleCalculator {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
        String[] args = timeConfig.split(" ");

        Calendar next = Calendar.getInstance();

        try {
            if (timeConfig.startsWith("every day at")) {
                // Format: "every day at HH:mm"
                String time = timeConfig.substring("every day at".length()).trim();
                for (int i = 0; i < count; i++) {
                    next = getNextDailyRun(next, time);
                    runs.add(dateFormat.format(next.getTime()));
                }
            } else if (args.length >= 5 && args[2].contains("day") && args[4].contains("month")) {
                // Format: "every N day of month [at HH:mm]"
                int dayOfMonth = Integer.parseInt(args[1]);
                String clockTime = args.length >= 7 && args[5].contains("at") ? args[6] : null;

                for (int i = 0; i < count; i++) {
                    next = getNextMonthlyRun(next, dayOfMonth, clockTime);
                    runs.add(dateFormat.format(next.getTime()));
                }
            } else if (args.length >= 5 && args[2].contains("day") && args[4].contains("week")) {
                // Format: "every N day of week [at HH:mm]"
                int dayOfWeek = Integer.parseInt(args[1]) + 1; // Adjust to Calendar format
                String clockTime = args.length >= 7 && args[5].contains("at") ? args[6] : null;

                for (int i = 0; i < count; i++) {
                    next = getNextWeeklyRun(next, dayOfWeek, clockTime);
                    runs.add(dateFormat.format(next.getTime()));
                }
            } else if (args.length == 3) {
                // Format: "every N second/minute/hour/day(s)"
                int interval = Integer.parseInt(args[1]);
                String unit = args[2].toLowerCase();

                int seconds = 0;
                if (unit.contains("second")) {
                    seconds = interval;
                } else if (unit.contains("minute")) {
                    seconds = interval * 60;
                } else if (unit.contains("hour")) {
                    seconds = interval * 60 * 60;
                } else if (unit.contains("day")) {
                    seconds = interval * 60 * 60 * 24;
                }

                for (int i = 0; i < count; i++) {
                    next.add(Calendar.SECOND, seconds);
                    runs.add(dateFormat.format(next.getTime()));
                }
            } else {
                runs.add("&cInvalid time format");
            }
        } catch (Exception e) {
            runs.add("&cFailed to calculate schedule: " + e.getMessage());
        }

        return runs;
    }

    private static Calendar getNextDailyRun(Calendar current, String time) {
        Calendar next = Calendar.getInstance();
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);

        if (next.before(current)) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }

        return next;
    }

    private static Calendar getNextMonthlyRun(Calendar current, int dayOfMonth, String time) {
        Calendar next = Calendar.getInstance();

        // Set to the specified day and time
        next.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (time != null) {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            next.set(Calendar.HOUR_OF_DAY, hour);
            next.set(Calendar.MINUTE, minute);
            next.set(Calendar.SECOND, 0);
        } else {
            next.set(Calendar.HOUR_OF_DAY, 0);
            next.set(Calendar.MINUTE, 0);
            next.set(Calendar.SECOND, 0);
        }

        // If this date/time has already passed, move to next month
        if (next.before(current) || next.equals(current)) {
            next.add(Calendar.MONTH, 1);
        }

        return next;
    }

    private static Calendar getNextWeeklyRun(Calendar current, int dayOfWeek, String time) {
        Calendar next = Calendar.getInstance();

        // Set time if specified
        if (time != null) {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            next.set(Calendar.HOUR_OF_DAY, hour);
            next.set(Calendar.MINUTE, minute);
            next.set(Calendar.SECOND, 0);
        } else {
            next.set(Calendar.HOUR_OF_DAY, 0);
            next.set(Calendar.MINUTE, 0);
            next.set(Calendar.SECOND, 0);
        }

        // Calculate days until the target day of week
        int currentDay = next.get(Calendar.DAY_OF_WEEK);
        int daysUntil = (dayOfWeek - currentDay + 7) % 7;

        if (daysUntil == 0 && next.before(current)) {
            daysUntil = 7; // If today but time has passed, next week
        } else if (daysUntil == 0) {
            daysUntil = 0; // Today and time hasn't passed
        }

        next.add(Calendar.DAY_OF_MONTH, daysUntil);

        // If we're at the target day but the time has passed, go to next week
        if (next.before(current) || (daysUntil == 0 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == dayOfWeek && hasTimePassed(time))) {
            next.add(Calendar.DAY_OF_MONTH, 7);
        }

        return next;
    }

    private static boolean hasTimePassed(String time) {
        if (time == null) return false;

        try {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar now = Calendar.getInstance();
            Calendar checkTime = Calendar.getInstance();
            checkTime.set(Calendar.HOUR_OF_DAY, hour);
            checkTime.set(Calendar.MINUTE, minute);
            checkTime.set(Calendar.SECOND, 0);

            return now.after(checkTime);
        } catch (Exception e) {
            return false;
        }
    }
}

