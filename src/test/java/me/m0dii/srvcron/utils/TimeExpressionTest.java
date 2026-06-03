package me.m0dii.srvcron.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeExpressionTest {
    private static final ZoneId UTC = ZoneId.of("Etc/UTC");

    private static ZonedDateTime zdt(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, UTC);
    }

    private static void assertSameInstant(ZonedDateTime expected, ZonedDateTime actual) {
        assertEquals(expected.toInstant(), actual.toInstant());
    }

    @Test
    void parsesNamedWeekdayAndFindsNextRuns() {
        TimeExpression expression = TimeExpression.parse("every wednesday at 00:00 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 0, 0).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(2, from);

        assertEquals(2, next.size());
        assertSameInstant(zdt(2026, 6, 3, 0, 0), next.get(0));
        assertSameInstant(zdt(2026, 6, 10, 0, 0), next.get(1));
    }

    @Test
    void supportsWeekdayListsByName() {
        TimeExpression expression = TimeExpression.parse("every monday,friday at 18:30 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 18, 0).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(3, from);

        assertSameInstant(zdt(2026, 6, 1, 18, 30), next.get(0));
        assertSameInstant(zdt(2026, 6, 5, 18, 30), next.get(1));
        assertSameInstant(zdt(2026, 6, 8, 18, 30), next.get(2));
    }

    @Test
    void supportsMonthDayRanges() {
        TimeExpression expression = TimeExpression.parse("every day of month in 1..3 at 09:00 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 8, 0).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(4, from);

        assertSameInstant(zdt(2026, 6, 1, 9, 0), next.get(0));
        assertSameInstant(zdt(2026, 6, 2, 9, 0), next.get(1));
        assertSameInstant(zdt(2026, 6, 3, 9, 0), next.get(2));
        assertSameInstant(zdt(2026, 7, 1, 9, 0), next.get(3));
    }

    @Test
    void supportsClassicCronSyntax() {
        TimeExpression expression = TimeExpression.parse("cron: 0 0 * * 3 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 0, 0).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(2, from);

        assertSameInstant(zdt(2026, 6, 3, 0, 0), next.get(0));
        assertSameInstant(zdt(2026, 6, 10, 0, 0), next.get(1));
    }

    @Test
    void supportsIntervalWindows() {
        TimeExpression expression = TimeExpression.parse("every 15 minutes from 09:00 to 10:00 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 8, 58).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(5, from);

        assertSameInstant(zdt(2026, 6, 1, 9, 0), next.get(0));
        assertSameInstant(zdt(2026, 6, 1, 9, 15), next.get(1));
        assertSameInstant(zdt(2026, 6, 1, 9, 30), next.get(2));
        assertSameInstant(zdt(2026, 6, 1, 9, 45), next.get(3));
        assertSameInstant(zdt(2026, 6, 1, 10, 0), next.get(4));
    }

    @Test
    void supportsMultipleDailyTimes() {
        TimeExpression expression = TimeExpression.parse("every day at 08:00,12:00,18:00 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 7, 59).toInstant();
        List<ZonedDateTime> next = expression.nextRuns(4, from);

        assertSameInstant(zdt(2026, 6, 1, 8, 0), next.get(0));
        assertSameInstant(zdt(2026, 6, 1, 12, 0), next.get(1));
        assertSameInstant(zdt(2026, 6, 1, 18, 0), next.get(2));
        assertSameInstant(zdt(2026, 6, 2, 8, 0), next.get(3));
    }

    @Test
    void supportsNthAndLastWeekdayInMonth() {
        TimeExpression secondMonday = TimeExpression.parse("every 2nd monday of month at 10:00 timezone Etc/UTC");
        TimeExpression lastFriday = TimeExpression.parse("every last friday of month at 22:00 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 1, 0, 0).toInstant();

        List<ZonedDateTime> secondMondayRuns = secondMonday.nextRuns(1, from);
        List<ZonedDateTime> lastFridayRuns = lastFriday.nextRuns(1, from);

        assertSameInstant(zdt(2026, 6, 8, 10, 0), secondMondayRuns.get(0));
        assertSameInstant(zdt(2026, 6, 26, 22, 0), lastFridayRuns.get(0));
    }

    @Test
    void supportsRelativeKeywordsAndLastDay() {
        TimeExpression weekday = TimeExpression.parse("every weekday at 09:00 timezone Etc/UTC");
        TimeExpression weekend = TimeExpression.parse("every weekend at 11:00 timezone Etc/UTC");
        TimeExpression lastDay = TimeExpression.parse("every month on last-day at 23:55 timezone Etc/UTC");

        Instant from = zdt(2026, 6, 5, 8, 59).toInstant(); // Friday

        assertSameInstant(zdt(2026, 6, 5, 9, 0), weekday.nextRuns(1, from).get(0));
        assertSameInstant(zdt(2026, 6, 6, 11, 0), weekend.nextRuns(1, from).get(0));

        Instant monthFrom = zdt(2026, 6, 1, 0, 0).toInstant();
        assertSameInstant(zdt(2026, 6, 30, 23, 55), lastDay.nextRuns(1, monthFrom).get(0));
    }

    @Test
    void supportsOneShotAndDateRangeConstraints() {
        TimeExpression oneShot = TimeExpression.parse("at 2026-06-10 14:30 timezone Etc/UTC");
        Instant from = zdt(2026, 6, 10, 14, 29).toInstant();

        List<ZonedDateTime> oneShotRuns = oneShot.nextRuns(3, from);
        assertEquals(1, oneShotRuns.size());
        assertSameInstant(zdt(2026, 6, 10, 14, 30), oneShotRuns.get(0));

        TimeExpression ranged = TimeExpression.parse("every 1 hour between 2026-06-01 and 2026-06-01 timezone Etc/UTC");
        Instant beforeRange = zdt(2026, 5, 31, 22, 0).toInstant();
        List<ZonedDateTime> rangeRuns = ranged.nextRuns(30, beforeRange);

        assertFalse(rangeRuns.isEmpty());
        assertTrue(rangeRuns.stream().allMatch(zdt -> zdt.toLocalDate().toString().equals("2026-06-01")));
    }

    @Test
    void parsesJitterAndTimezoneMetadata() {
        TimeExpression expression = TimeExpression.parse("every 5 minutes jitter 30s timezone Europe/Berlin");

        assertEquals(30, expression.jitterSeconds());
        assertTrue(expression.describe().contains("timezone Europe/Berlin"));
        assertTrue(expression.describe().contains("jitter 30s"));
    }
}

