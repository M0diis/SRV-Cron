package me.m0dii.srvcron.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleCalculatorTest {

    @Test
    void returnsUsefulOutputForValidExpressions() {
        List<String> runs = ScheduleCalculator.getNextRuns("every day at 08:00 timezone Etc/UTC", 3);

        assertFalse(runs.isEmpty());
        assertTrue(runs.size() <= 3);
        assertFalse(runs.getFirst().startsWith("Failed to calculate schedule:"));
        assertTrue(runs.getFirst().contains(":"));
    }

    @Test
    void returnsErrorForInvalidExpressions() {
        List<String> runs = ScheduleCalculator.getNextRuns("every unicorn at 10:00", 2);

        assertFalse(runs.isEmpty());
        assertTrue(runs.getFirst().startsWith("Failed to calculate schedule:"));
    }

    @Test
    void explainShowsParsedSummaryOrValidationError() {
        String ok = ScheduleCalculator.explain("cron: 0 0 * * 3 timezone Etc/UTC");
        String invalid = ScheduleCalculator.explain("bad expression");

        assertTrue(ok.contains("cron: 0 0 * * 3"));
        assertTrue(ok.contains("timezone Etc/UTC"));
        assertTrue(invalid.startsWith("Invalid expression:"));
    }
}

