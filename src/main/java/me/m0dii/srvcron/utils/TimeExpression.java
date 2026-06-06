package me.m0dii.srvcron.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeExpression {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Pattern BETWEEN_DATES = Pattern.compile("\\s+between\\s+(\\d{4}-\\d{2}-\\d{2})\\s+and\\s+(\\d{4}-\\d{2}-\\d{2})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMEZONE = Pattern.compile("\\s+timezone\\s+([A-Za-z_]+/[A-Za-z_]+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JITTER = Pattern.compile("\\s+jitter\\s+(\\d+)(s|m)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, DayOfWeek> DAY_NAME_TO_WEEKDAY = buildWeekdayMap();
    private static final Map<String, Month> MONTHS = buildMonthMap();

    private enum WeekdayNumbering {
        MONDAY_FIRST,
        SUNDAY_FIRST
    }

    private static volatile WeekdayNumbering weekdayNumbering = WeekdayNumbering.MONDAY_FIRST;

    private final String source;
    private final String normalized;
    private final ZoneId zoneId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int jitterSeconds;
    private final Kind kind;

    private final int intervalValue;
    private final IntervalUnit intervalUnit;
    private final LocalTime windowStart;
    private final LocalTime windowEnd;

    private final Set<Integer> weekdayDslNumbers;
    private final Set<Integer> monthDays;
    private final Set<Month> months;
    private final List<LocalTime> times;

    private final Integer nth;
    private final DayOfWeek nthWeekday;
    private final boolean lastWeekdayOfMonth;
    private final boolean lastDayOfMonth;

    private final LocalDateTime oneShotAt;

    private final CronMatcher cron;

    private enum Kind {
        INTERVAL,
        DAILY_TIMES,
        WEEKDAY_TIMES,
        WEEKEND_TIMES,
        WEEKLY_DAYS_TIMES,
        MONTHLY_DAYS_TIMES,
        MONTHLY_NTH_WEEKDAY,
        MONTHLY_LAST_WEEKDAY,
        MONTHLY_LAST_DAY,
        ONE_SHOT,
        CRON
    }

    private enum IntervalUnit {
        SECOND,
        MINUTE,
        HOUR,
        DAY
    }

    private TimeExpression(
            String source,
            String normalized,
            ZoneId zoneId,
            LocalDate startDate,
            LocalDate endDate,
            int jitterSeconds,
            Kind kind,
            int intervalValue,
            IntervalUnit intervalUnit,
            LocalTime windowStart,
            LocalTime windowEnd,
            Set<Integer> weekdayDslNumbers,
            Set<Integer> monthDays,
            Set<Month> months,
            List<LocalTime> times,
            Integer nth,
            DayOfWeek nthWeekday,
            boolean lastWeekdayOfMonth,
            boolean lastDayOfMonth,
            LocalDateTime oneShotAt,
            CronMatcher cron
    ) {
        this.source = source;
        this.normalized = normalized;
        this.zoneId = zoneId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.jitterSeconds = jitterSeconds;
        this.kind = kind;
        this.intervalValue = intervalValue;
        this.intervalUnit = intervalUnit;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.weekdayDslNumbers = weekdayDslNumbers;
        this.monthDays = monthDays;
        this.months = months;
        this.times = times;
        this.nth = nth;
        this.nthWeekday = nthWeekday;
        this.lastWeekdayOfMonth = lastWeekdayOfMonth;
        this.lastDayOfMonth = lastDayOfMonth;
        this.oneShotAt = oneShotAt;
        this.cron = cron;
    }

    public static TimeExpression parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Time expression cannot be empty.");
        }

        String source = expression.trim();
        String work = source;

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate startDate = null;
        LocalDate endDate = null;
        int jitterSeconds = 0;

        Matcher tzMatcher = TIMEZONE.matcher(work);
        if (tzMatcher.find()) {
            zoneId = ZoneId.of(tzMatcher.group(1));
            work = work.substring(0, tzMatcher.start());
        }

        Matcher jitterMatcher = JITTER.matcher(work);
        if (jitterMatcher.find()) {
            int value = Integer.parseInt(jitterMatcher.group(1));
            String unit = jitterMatcher.group(2).toLowerCase(Locale.ROOT);
            jitterSeconds = unit.equals("m") ? value * 60 : value;
            work = work.substring(0, jitterMatcher.start());
        }

        Matcher betweenMatcher = BETWEEN_DATES.matcher(work);
        if (betweenMatcher.find()) {
            startDate = parseDate(betweenMatcher.group(1), "start date");
            endDate = parseDate(betweenMatcher.group(2), "end date");
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("End date cannot be before start date.");
            }
            work = work.substring(0, betweenMatcher.start());
        }

        work = work.trim();

        // one-shot
        if (work.toLowerCase(Locale.ROOT).startsWith("at ")) {
            LocalDateTime at = parseDateTime(work.substring(3).trim());
            String normalized = "at " + at.format(DATE_TIME_FMT);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.ONE_SHOT, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                    null, null, false, false, at, null);
        }

        // classic cron
        if (work.toLowerCase(Locale.ROOT).startsWith("cron:")) {
            String cronExpr = work.substring("cron:".length()).trim();
            CronMatcher cronMatcher = CronMatcher.parse(cronExpr);
            return withOptions(source, "cron: " + cronMatcher.raw(), zoneId, startDate, endDate, jitterSeconds,
                    Kind.CRON, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                    null, null, false, false, null, cronMatcher);
        }

        if (!work.toLowerCase(Locale.ROOT).startsWith("every ")) {
            throw new IllegalArgumentException("Expression must start with 'every', 'at' or 'cron:'.");
        }

        String body = work.substring(6).trim();

        // every 15 minutes from 09:00 to 17:00
        Matcher intervalWindow = Pattern.compile("^(\\d+)\\s+(seconds?|minutes?|hours?|days?)\\s+from\\s+([0-2]\\d:[0-5]\\d)\\s+to\\s+([0-2]\\d:[0-5]\\d)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (intervalWindow.find()) {
            int value = Integer.parseInt(intervalWindow.group(1));
            IntervalUnit unit = parseUnit(intervalWindow.group(2));
            LocalTime from = parseTime(intervalWindow.group(3));
            LocalTime to = parseTime(intervalWindow.group(4));
            String normalized = String.format(Locale.ROOT, "every %d %s from %s to %s", value, unitName(unit, value), from, to);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.INTERVAL, value, unit, from, to,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                    null, null, false, false, null, null);
        }

        // every 5 minutes / every 1 hour
        Matcher interval = Pattern.compile("^(\\d+)\\s+(seconds?|minutes?|hours?|days?)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (interval.find()) {
            int value = Integer.parseInt(interval.group(1));
            IntervalUnit unit = parseUnit(interval.group(2));
            String normalized = String.format(Locale.ROOT, "every %d %s", value, unitName(unit, value));
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.INTERVAL, value, unit, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
                    null, null, false, false, null, null);
        }

        // every weekday at 09:00 / every weekend at 11:00
        Matcher weekdayWeekend = Pattern.compile("^(weekday|weekend)\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (weekdayWeekend.find()) {
            List<LocalTime> parsedTimes = parseTimes(weekdayWeekend.group(2));
            String key = weekdayWeekend.group(1).toLowerCase(Locale.ROOT);
            Kind kind = key.equals("weekday") ? Kind.WEEKDAY_TIMES : Kind.WEEKEND_TIMES;
            String normalized = "every " + key + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    kind, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // every month on last-day at 23:55
        Matcher lastDay = Pattern.compile("^month\\s+on\\s+last-day\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (lastDay.find()) {
            List<LocalTime> parsedTimes = parseTimes(lastDay.group(1));
            String normalized = "every month on last-day at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_LAST_DAY, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, true, null, null);
        }

        // every 2nd monday of month at 10:00
        Matcher nthWeekday = Pattern.compile("^(\\d+)(st|nd|rd|th)\\s+([a-z]+)\\s+of\\s+month\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (nthWeekday.find()) {
            int nth = Integer.parseInt(nthWeekday.group(1));
            DayOfWeek dayOfWeek = parseWeekdayName(nthWeekday.group(3));
            List<LocalTime> parsedTimes = parseTimes(nthWeekday.group(4));
            String normalized = "every " + nth + ordinalSuffix(nth) + " " + dayOfWeek.name().toLowerCase(Locale.ROOT) + " of month at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_NTH_WEEKDAY, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    nth, dayOfWeek, false, false, null, null);
        }

        // every last friday of month at 22:00
        Matcher lastWeekday = Pattern.compile("^last\\s+([a-z]+)\\s+of\\s+month\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (lastWeekday.find()) {
            DayOfWeek dayOfWeek = parseWeekdayName(lastWeekday.group(1));
            List<LocalTime> parsedTimes = parseTimes(lastWeekday.group(2));
            String normalized = "every last " + dayOfWeek.name().toLowerCase(Locale.ROOT) + " of month at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_LAST_WEEKDAY, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, dayOfWeek, true, false, null, null);
        }

        // every day of week in 1,3,5 at 12:00
        Matcher weekIn = Pattern.compile("^day\\s+of\\s+week\\s+in\\s+([^\\s]+)\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (weekIn.find()) {
            Set<Integer> days = parseDows(weekIn.group(1));
            List<LocalTime> parsedTimes = parseTimes(weekIn.group(2));
            String normalized = "every day of week in " + joinInts(days) + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.WEEKLY_DAYS_TIMES, 0, null, null, null,
                    days, Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // every day of month in 1..5 at 09:00
        Matcher monthIn = Pattern.compile("^day\\s+of\\s+month\\s+in\\s+([^\\s]+)\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (monthIn.find()) {
            Set<Integer> days = parseMonthDays(monthIn.group(1));
            List<LocalTime> parsedTimes = parseTimes(monthIn.group(2));
            String normalized = "every day of month in " + joinInts(days) + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_DAYS_TIMES, 0, null, null, null,
                    Collections.emptySet(), days, Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // every january,march day 1 at 08:00
        Matcher monthNames = Pattern.compile("^([a-z,]+)\\s+day\\s+(\\d{1,2})\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (monthNames.find()) {
            Set<Month> monthSet = parseMonths(monthNames.group(1));
            int day = Integer.parseInt(monthNames.group(2));
            List<LocalTime> parsedTimes = parseTimes(monthNames.group(3));
            String normalized = "every " + joinMonths(monthSet) + " day " + day + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_DAYS_TIMES, 0, null, null, null,
                    Collections.emptySet(), Collections.singleton(day), monthSet, parsedTimes,
                    null, null, false, false, null, null);
        }

        // every monday,friday at 18:30 / every wednesday at 00:00
        Matcher namedDays = Pattern.compile("^([a-z,]+)\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (namedDays.find()) {
            if ("day".equalsIgnoreCase(namedDays.group(1).trim())) {
                List<LocalTime> parsedTimes = parseTimes(namedDays.group(2));
                String normalized = "every day at " + formatTimes(parsedTimes);
                return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                        Kind.DAILY_TIMES, 0, null, null, null,
                        Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                        null, null, false, false, null, null);
            }

            Set<Integer> days = parseDows(namedDays.group(1));
            List<LocalTime> parsedTimes = parseTimes(namedDays.group(2));
            String normalized = "every day of week in " + joinInts(days) + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.WEEKLY_DAYS_TIMES, 0, null, null, null,
                    days, Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // every day at 08:00,12:00,18:00
        Matcher everyDayAt = Pattern.compile("^day\\s+at\\s+([0-2]\\d:[0-5]\\d(?:,[0-2]\\d:[0-5]\\d)*)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (everyDayAt.find()) {
            List<LocalTime> parsedTimes = parseTimes(everyDayAt.group(1));
            String normalized = "every day at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.DAILY_TIMES, 0, null, null, null,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // legacy: every 4 day of week at 00:00
        Matcher legacyWeek = Pattern.compile("^(\\d+)\\s+day\\s+of\\s+week(?:\\s+at\\s+([0-2]\\d:[0-5]\\d))?$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (legacyWeek.find()) {
            int dsl = Integer.parseInt(legacyWeek.group(1));
            validateRange(dsl, 1, 7, "week day");
            Set<Integer> days = Collections.singleton(dsl);
            List<LocalTime> parsedTimes = legacyWeek.group(2) == null ? Collections.singletonList(LocalTime.MIDNIGHT) : parseTimes(legacyWeek.group(2));
            String normalized = "every day of week in " + dsl + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.WEEKLY_DAYS_TIMES, 0, null, null, null,
                    days, Collections.emptySet(), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        // legacy: every 1 day of month at 09:00
        Matcher legacyMonth = Pattern.compile("^(\\d+)\\s+day\\s+of\\s+month(?:\\s+at\\s+([0-2]\\d:[0-5]\\d))?$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (legacyMonth.find()) {
            int dom = Integer.parseInt(legacyMonth.group(1));
            validateRange(dom, 1, 31, "month day");
            List<LocalTime> parsedTimes = legacyMonth.group(2) == null ? Collections.singletonList(LocalTime.MIDNIGHT) : parseTimes(legacyMonth.group(2));
            String normalized = "every day of month in " + dom + " at " + formatTimes(parsedTimes);
            return withOptions(source, normalized, zoneId, startDate, endDate, jitterSeconds,
                    Kind.MONTHLY_DAYS_TIMES, 0, null, null, null,
                    Collections.emptySet(), Collections.singleton(dom), Collections.emptySet(), parsedTimes,
                    null, null, false, false, null, null);
        }

        throw new IllegalArgumentException("Unsupported time syntax: '" + source + "'");
    }

    /**
     * Configures numeric day-of-week interpretation for DSL expressions.
     * Returns false when the provided value is invalid and Monday-first fallback is used.
     */
    public static boolean configureWeekdayNumbering(String configuredValue) {
        if (configuredValue == null) {
            weekdayNumbering = WeekdayNumbering.MONDAY_FIRST;
            return false;
        }

        String normalized = configuredValue.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.equals("monday-first") || normalized.equals("monday")
                || normalized.equals("iso") || normalized.equals("iso-8601")) {
            weekdayNumbering = WeekdayNumbering.MONDAY_FIRST;
            return true;
        }
        if (normalized.equals("sunday-first") || normalized.equals("sunday") || normalized.equals("legacy")) {
            weekdayNumbering = WeekdayNumbering.SUNDAY_FIRST;
            return true;
        }

        weekdayNumbering = WeekdayNumbering.MONDAY_FIRST;
        return false;
    }

    public String source() {
        return source;
    }

    public String normalized() {
        return normalized;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public int jitterSeconds() {
        return jitterSeconds;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder(normalized);
        if (startDate != null || endDate != null) {
            sb.append(" | active ").append(startDate).append(" -> ").append(endDate);
        }
        if (jitterSeconds > 0) {
            sb.append(" | jitter ").append(jitterSeconds).append("s");
        }
        sb.append(" | timezone ").append(zoneId.getId());
        return sb.toString();
    }

    public boolean supportsSecondResolution() {
        return kind == Kind.INTERVAL && intervalUnit == IntervalUnit.SECOND;
    }

    public boolean shouldRunAt(Instant instant) {
        return matches(ZonedDateTime.ofInstant(instant, zoneId));
    }

    public List<ZonedDateTime> nextRuns(int count, Instant fromInclusive) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        ZonedDateTime cursor = ZonedDateTime.ofInstant(fromInclusive, zoneId);
        List<ZonedDateTime> out = new ArrayList<>();

        int stepSeconds = supportsSecondResolution() ? 1 : 60;
        int maxIterations = supportsSecondResolution() ? 2_000_000 : 1_000_000;

        if (!supportsSecondResolution()) {
            // Align to exact minute edges to avoid drifting when fromInclusive has non-zero seconds.
            cursor = cursor.withSecond(0).withNano(0);
        }

        for (int i = 0; i < maxIterations && out.size() < count; i++) {
            cursor = cursor.plusSeconds(stepSeconds);
            if (matches(cursor)) {
                out.add(cursor);
            }
        }

        return out;
    }

    private boolean matches(ZonedDateTime now) {
        if (!withinDateRange(now.toLocalDate())) {
            return false;
        }

        switch (kind) {
            case ONE_SHOT:
                return now.withSecond(0).withNano(0).toLocalDateTime().equals(oneShotAt);
            case INTERVAL:
                return matchInterval(now);
            case DAILY_TIMES:
                return isMinuteEdge(now) && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case WEEKDAY_TIMES:
                return isMinuteEdge(now)
                        && isWeekday(now.getDayOfWeek())
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case WEEKEND_TIMES:
                return isMinuteEdge(now)
                        && !isWeekday(now.getDayOfWeek())
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case WEEKLY_DAYS_TIMES:
                return isMinuteEdge(now)
                        && weekdayDslNumbers.contains(dayOfWeekToDsl(now.getDayOfWeek()))
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case MONTHLY_DAYS_TIMES:
                if (!isMinuteEdge(now)) {
                    return false;
                }
                if (!months.isEmpty() && !months.contains(now.getMonth())) {
                    return false;
                }
                return monthDays.contains(now.getDayOfMonth())
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case MONTHLY_NTH_WEEKDAY:
                if (!isMinuteEdge(now) || now.getDayOfWeek() != nthWeekday) {
                    return false;
                }
                LocalDate nthDate = now.toLocalDate().with(TemporalAdjusters.dayOfWeekInMonth(nth, nthWeekday));
                return nthDate.equals(now.toLocalDate())
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case MONTHLY_LAST_WEEKDAY:
                if (!isMinuteEdge(now) || !lastWeekdayOfMonth || now.getDayOfWeek() != nthWeekday) {
                    return false;
                }
                LocalDate lastDate = now.toLocalDate().with(TemporalAdjusters.lastInMonth(nthWeekday));
                return lastDate.equals(now.toLocalDate())
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case MONTHLY_LAST_DAY:
                if (!isMinuteEdge(now) || !lastDayOfMonth) {
                    return false;
                }
                int lastDom = now.toLocalDate().lengthOfMonth();
                return now.getDayOfMonth() == lastDom
                        && times.contains(now.toLocalTime().withSecond(0).withNano(0));
            case CRON:
                return isMinuteEdge(now) && cron.matches(now);
            default:
                return false;
        }
    }

    private boolean withinDateRange(LocalDate date) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private boolean matchInterval(ZonedDateTime now) {
        if (intervalUnit != IntervalUnit.SECOND && !isMinuteEdge(now)) {
            return false;
        }

        if (windowStart != null && windowEnd != null && !isWithinWindow(now.toLocalTime())) {
            return false;
        }

        long value;
        switch (intervalUnit) {
            case SECOND:
                value = now.toEpochSecond();
                break;
            case MINUTE:
                value = now.toEpochSecond() / 60;
                break;
            case HOUR:
                value = now.toEpochSecond() / 3600;
                break;
            case DAY:
                value = now.toLocalDate().toEpochDay();
                break;
            default:
                return false;
        }

        return value % intervalValue == 0;
    }

    private boolean isWithinWindow(LocalTime time) {
        if (windowStart.equals(windowEnd)) {
            return true;
        }

        if (windowStart.isBefore(windowEnd) || windowStart.equals(windowEnd)) {
            return !time.isBefore(windowStart) && !time.isAfter(windowEnd);
        }

        // Overnight window: 23:00 -> 03:00
        return !time.isBefore(windowStart) || !time.isAfter(windowEnd);
    }

    private static boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private static boolean isMinuteEdge(ZonedDateTime now) {
        return now.getSecond() == 0;
    }

    private static TimeExpression withOptions(
            String source,
            String normalized,
            ZoneId zoneId,
            LocalDate startDate,
            LocalDate endDate,
            int jitterSeconds,
            Kind kind,
            int intervalValue,
            IntervalUnit intervalUnit,
            LocalTime windowStart,
            LocalTime windowEnd,
            Set<Integer> weekdayDslNumbers,
            Set<Integer> monthDays,
            Set<Month> months,
            List<LocalTime> times,
            Integer nth,
            DayOfWeek nthWeekday,
            boolean lastWeekdayOfMonth,
            boolean lastDayOfMonth,
            LocalDateTime oneShotAt,
            CronMatcher cron
    ) {
        return new TimeExpression(
                source,
                normalized,
                zoneId,
                startDate,
                endDate,
                jitterSeconds,
                kind,
                intervalValue,
                intervalUnit,
                windowStart,
                windowEnd,
                new HashSet<>(weekdayDslNumbers),
                new HashSet<>(monthDays),
                new HashSet<>(months),
                new ArrayList<>(times),
                nth,
                nthWeekday,
                lastWeekdayOfMonth,
                lastDayOfMonth,
                oneShotAt,
                cron
        );
    }

    private static Set<Integer> parseDows(String input) {
        Set<Integer> out = new HashSet<>();
        for (String token : input.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (t.contains("..")) {
                String[] range = t.split("\\.\\.");
                int from = parseDowSingle(range[0]);
                int to = parseDowSingle(range[1]);
                if (from > to) {
                    throw new IllegalArgumentException("Invalid weekday range: " + token);
                }
                for (int i = from; i <= to; i++) {
                    out.add(i);
                }
            } else {
                out.add(parseDowSingle(t));
            }
        }
        return out;
    }

    private static int parseDowSingle(String token) {
        if (token.matches("\\d+")) {
            int val = Integer.parseInt(token);
            if (val < 1 || val > 7) {
                throw new IllegalArgumentException("week day out of range: " + val + " (expected 1-7, mode " + weekdayNumberingLabel() + ")");
            }
            return val;
        }

        DayOfWeek dayOfWeek = DAY_NAME_TO_WEEKDAY.get(token);
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Unknown weekday: " + token);
        }
        return dayOfWeekToDsl(dayOfWeek);
    }

    private static String weekdayNumberingLabel() {
        return weekdayNumbering == WeekdayNumbering.SUNDAY_FIRST ? "sunday-first" : "monday-first";
    }

    private static Set<Integer> parseMonthDays(String input) {
        Set<Integer> out = new HashSet<>();
        for (String token : input.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (t.contains("..")) {
                String[] range = t.split("\\.\\.");
                int from = Integer.parseInt(range[0]);
                int to = Integer.parseInt(range[1]);
                validateRange(from, 1, 31, "month day");
                validateRange(to, 1, 31, "month day");
                if (from > to) {
                    throw new IllegalArgumentException("Invalid month-day range: " + token);
                }
                for (int i = from; i <= to; i++) {
                    out.add(i);
                }
            } else {
                int val = Integer.parseInt(t);
                validateRange(val, 1, 31, "month day");
                out.add(val);
            }
        }
        return out;
    }

    private static Set<Month> parseMonths(String input) {
        Set<Month> out = new HashSet<>();
        for (String token : input.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            Month month = MONTHS.get(t);
            if (month == null) {
                throw new IllegalArgumentException("Unknown month: " + token);
            }
            out.add(month);
        }
        return out;
    }

    private static List<LocalTime> parseTimes(String csv) {
        List<LocalTime> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            out.add(parseTime(part.trim()));
        }
        Collections.sort(out);
        return out;
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid time: " + text);
        }
    }

    private static LocalDate parseDate(String text, String label) {
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + text);
        }
    }

    private static LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid one-shot date/time: " + text + ". Use yyyy-MM-dd HH:mm");
        }
    }

    private static String formatTimes(List<LocalTime> times) {
        List<String> out = new ArrayList<>();
        for (LocalTime time : times) {
            out.add(time.toString());
        }
        return String.join(",", out);
    }

    private static String joinInts(Set<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        List<String> out = new ArrayList<>();
        for (Integer val : sorted) {
            out.add(String.valueOf(val));
        }
        return String.join(",", out);
    }

    private static String joinMonths(Set<Month> monthSet) {
        List<Month> sorted = new ArrayList<>(monthSet);
        Collections.sort(sorted);
        List<String> out = new ArrayList<>();
        for (Month month : sorted) {
            out.add(month.name().toLowerCase(Locale.ROOT));
        }
        return String.join(",", out);
    }

    private static IntervalUnit parseUnit(String unit) {
        String normalized = unit.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("second")) {
            return IntervalUnit.SECOND;
        }
        if (normalized.startsWith("minute")) {
            return IntervalUnit.MINUTE;
        }
        if (normalized.startsWith("hour")) {
            return IntervalUnit.HOUR;
        }
        if (normalized.startsWith("day")) {
            return IntervalUnit.DAY;
        }
        throw new IllegalArgumentException("Unsupported interval unit: " + unit);
    }

    private static String unitName(IntervalUnit unit, int value) {
        return switch (unit) {
            case SECOND -> value == 1 ? "second" : "seconds";
            case MINUTE -> value == 1 ? "minute" : "minutes";
            case HOUR -> value == 1 ? "hour" : "hours";
            case DAY -> value == 1 ? "day" : "days";
        };
    }

    private static DayOfWeek parseWeekdayName(String token) {
        DayOfWeek dayOfWeek = DAY_NAME_TO_WEEKDAY.get(token.toLowerCase(Locale.ROOT));
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Unknown weekday: " + token);
        }
        return dayOfWeek;
    }

    private static DayOfWeek dslToDayOfWeek(int dsl) {
        if (weekdayNumbering == WeekdayNumbering.SUNDAY_FIRST) {
            return switch (dsl) {
                case 1 -> DayOfWeek.SUNDAY;
                case 2 -> DayOfWeek.MONDAY;
                case 3 -> DayOfWeek.TUESDAY;
                case 4 -> DayOfWeek.WEDNESDAY;
                case 5 -> DayOfWeek.THURSDAY;
                case 6 -> DayOfWeek.FRIDAY;
                case 7 -> DayOfWeek.SATURDAY;
                default -> throw new IllegalArgumentException("Invalid DSL weekday: " + dsl);
            };
        }

        return switch (dsl) {
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            case 6 -> DayOfWeek.SATURDAY;
            case 7 -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid DSL weekday: " + dsl);
        };
    }

    private static int dayOfWeekToDsl(DayOfWeek day) {
        if (weekdayNumbering == WeekdayNumbering.SUNDAY_FIRST) {
            return switch (day) {
                case SUNDAY -> 1;
                case MONDAY -> 2;
                case TUESDAY -> 3;
                case WEDNESDAY -> 4;
                case THURSDAY -> 5;
                case FRIDAY -> 6;
                case SATURDAY -> 7;
                default -> 0;
            };
        }

        return switch (day) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
            default -> 0;
        };
    }

    private static void validateRange(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " out of range: " + value + " (expected " + min + "-" + max + ")");
        }
    }

    private static String ordinalSuffix(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) {
            return "th";
        }
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private static Map<String, DayOfWeek> buildWeekdayMap() {
        Map<String, DayOfWeek> out = new HashMap<>();
        out.put("mon", DayOfWeek.MONDAY);
        out.put("monday", DayOfWeek.MONDAY);
        out.put("tue", DayOfWeek.TUESDAY);
        out.put("tues", DayOfWeek.TUESDAY);
        out.put("tuesday", DayOfWeek.TUESDAY);
        out.put("wed", DayOfWeek.WEDNESDAY);
        out.put("wednesday", DayOfWeek.WEDNESDAY);
        out.put("thu", DayOfWeek.THURSDAY);
        out.put("thur", DayOfWeek.THURSDAY);
        out.put("thurs", DayOfWeek.THURSDAY);
        out.put("thursday", DayOfWeek.THURSDAY);
        out.put("fri", DayOfWeek.FRIDAY);
        out.put("friday", DayOfWeek.FRIDAY);
        out.put("sat", DayOfWeek.SATURDAY);
        out.put("saturday", DayOfWeek.SATURDAY);
        out.put("sun", DayOfWeek.SUNDAY);
        out.put("sunday", DayOfWeek.SUNDAY);
        return out;
    }

    private static Map<String, Month> buildMonthMap() {
        Map<String, Month> out = new HashMap<>();
        for (Month month : Month.values()) {
            out.put(month.name().toLowerCase(Locale.ROOT), month);
            out.put(month.name().substring(0, 3).toLowerCase(Locale.ROOT), month);
        }
        return out;
    }

    private record CronMatcher(
            String raw,
            IntMatcher minute,
            IntMatcher hour,
            IntMatcher dayOfMonth,
            IntMatcher month,
            IntMatcher dayOfWeek
    ) {

        static CronMatcher parse(String expr) {
            String[] split = expr.trim().split("\\s+");
            if (split.length != 5) {
                throw new IllegalArgumentException("Cron requires 5 fields: minute hour day-of-month month day-of-week");
            }

            IntMatcher minute = IntMatcher.parse(split[0], 0, 59, null);
            IntMatcher hour = IntMatcher.parse(split[1], 0, 23, null);
            IntMatcher dom = IntMatcher.parse(split[2], 1, 31, null);
            IntMatcher month = IntMatcher.parse(split[3], 1, 12, MONTHS_TO_INT);
            IntMatcher dow = IntMatcher.parse(split[4], 0, 7, DOWS_TO_INT);

            return new CronMatcher(expr.trim(), minute, hour, dom, month, dow);
        }

        boolean matches(ZonedDateTime now) {
            int dow = now.getDayOfWeek().getValue() % 7; // Sunday = 0
            return minute.matches(now.getMinute())
                    && hour.matches(now.getHour())
                    && dayOfMonth.matches(now.getDayOfMonth())
                    && month.matches(now.getMonthValue())
                    && dayOfWeek.matches(dow);
        }

        private static final Map<String, Integer> MONTHS_TO_INT = new HashMap<>();
        private static final Map<String, Integer> DOWS_TO_INT = new HashMap<>();

        static {
            MONTHS_TO_INT.put("jan", 1);
            MONTHS_TO_INT.put("feb", 2);
            MONTHS_TO_INT.put("mar", 3);
            MONTHS_TO_INT.put("apr", 4);
            MONTHS_TO_INT.put("may", 5);
            MONTHS_TO_INT.put("jun", 6);
            MONTHS_TO_INT.put("jul", 7);
            MONTHS_TO_INT.put("aug", 8);
            MONTHS_TO_INT.put("sep", 9);
            MONTHS_TO_INT.put("oct", 10);
            MONTHS_TO_INT.put("nov", 11);
            MONTHS_TO_INT.put("dec", 12);

            DOWS_TO_INT.put("sun", 0);
            DOWS_TO_INT.put("mon", 1);
            DOWS_TO_INT.put("tue", 2);
            DOWS_TO_INT.put("wed", 3);
            DOWS_TO_INT.put("thu", 4);
            DOWS_TO_INT.put("fri", 5);
            DOWS_TO_INT.put("sat", 6);
        }
    }

    private record IntMatcher(Set<Integer> allowed) {

        static IntMatcher parse(String expr, int min, int max, Map<String, Integer> aliases) {
            Set<Integer> values = new HashSet<>();
            String[] parts = expr.split(",");
            for (String rawPart : parts) {
                String part = rawPart.trim().toLowerCase(Locale.ROOT);
                if (part.equals("*")) {
                    for (int i = min; i <= max; i++) {
                        values.add(i);
                    }
                    continue;
                }

                String[] stepSplit = part.split("/");
                String base = stepSplit[0];
                int step = stepSplit.length == 2 ? Integer.parseInt(stepSplit[1]) : 1;

                List<Integer> baseValues = new ArrayList<>();

                if (base.equals("*")) {
                    for (int i = min; i <= max; i++) {
                        baseValues.add(i);
                    }
                } else if (base.contains("-")) {
                    String[] range = base.split("-");
                    int from = parseAliasOrInt(range[0], aliases);
                    int to = parseAliasOrInt(range[1], aliases);
                    if (from > to) {
                        throw new IllegalArgumentException("Invalid cron range: " + base);
                    }
                    for (int i = from; i <= to; i++) {
                        baseValues.add(i);
                    }
                } else {
                    baseValues.add(parseAliasOrInt(base, aliases));
                }

                for (int i = 0; i < baseValues.size(); i += step) {
                    int val = baseValues.get(i);
                    if (val == 7 && max == 7) {
                        val = 0;
                    }
                    if (val < min || val > max) {
                        throw new IllegalArgumentException("Cron value out of range: " + val);
                    }
                    values.add(val);
                }
            }

            return new IntMatcher(values);
        }

        boolean matches(int value) {
            return allowed.contains(value);
        }

        private static int parseAliasOrInt(String value, Map<String, Integer> aliases) {
            if (aliases != null && aliases.containsKey(value)) {
                return aliases.get(value);
            }
            return Integer.parseInt(value);
        }
    }
}

