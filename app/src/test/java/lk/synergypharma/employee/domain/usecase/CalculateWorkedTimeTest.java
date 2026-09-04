package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.enums.GateDirection;

/**
 * The gate drops events in the real world — a mask, a cap, a camera down, a
 * power cut — so these cases are the normal ones, not the edge ones.
 */
public class CalculateWorkedTimeTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 12);

    private static GateEvent event(int hour, int minute, GateDirection direction) {
        return new GateEvent(
                hour + ":" + minute,
                LocalDateTime.of(DAY, LocalTime.of(hour, minute)),
                direction,
                "Main Entrance - IN",
                "1",
                0.63f,
                null,
                false);
    }

    @Test
    public void pairsASimpleInAndOut() {
        List<GateEvent> events = Arrays.asList(
                event(8, 12, GateDirection.IN),
                event(17, 12, GateDirection.OUT));

        assertEquals(9 * 60, CalculateWorkedTime.insideMinutes(events, null));
    }

    @Test
    public void excludesTimeSpentOutsideForLunch() {
        List<GateEvent> events = Arrays.asList(
                event(8, 0, GateDirection.IN),
                event(12, 30, GateDirection.OUT),
                event(13, 0, GateDirection.IN),
                event(17, 0, GateDirection.OUT));

        // 4h30m before lunch + 4h after — the half hour outside does not count.
        assertEquals(8 * 60 + 30, CalculateWorkedTime.insideMinutes(events, null));
    }

    @Test
    public void ignoresADuplicateInBecauseThePersonNeverLeft() {
        List<GateEvent> events = Arrays.asList(
                event(8, 0, GateDirection.IN),
                event(8, 5, GateDirection.IN),
                event(17, 0, GateDirection.OUT));

        assertEquals(9 * 60, CalculateWorkedTime.insideMinutes(events, null));
    }

    @Test
    public void ignoresAnOutWithNoMatchingIn() {
        // The morning IN was missed entirely; the day must not go negative.
        List<GateEvent> events = Arrays.asList(
                event(12, 30, GateDirection.OUT),
                event(13, 0, GateDirection.IN),
                event(17, 0, GateDirection.OUT));

        assertEquals(4 * 60, CalculateWorkedTime.insideMinutes(events, null));
    }

    @Test
    public void countsAnOpenSpanUpToNowSoTheHomeCardTicks() {
        List<GateEvent> events = Arrays.asList(event(8, 0, GateDirection.IN));
        LocalDateTime now = LocalDateTime.of(DAY, LocalTime.of(12, 48));

        assertEquals(4 * 60 + 48, CalculateWorkedTime.insideMinutes(events, now));
    }

    @Test
    public void leavesAnOpenSpanUncountedWhenNoNowIsGiven() {
        List<GateEvent> events = Arrays.asList(event(8, 0, GateDirection.IN));

        assertEquals(0, CalculateWorkedTime.insideMinutes(events, null));
    }

    @Test
    public void handlesAnEmptyDay() {
        assertEquals(0, CalculateWorkedTime.insideMinutes(new ArrayList<>(), null));
        assertFalse(CalculateWorkedTime.isOpen(new ArrayList<>()));
    }

    @Test
    public void reportsStillInsideWhenTheLastEventWasAnIn() {
        assertTrue(CalculateWorkedTime.isOpen(
                Arrays.asList(event(8, 0, GateDirection.IN))));
        assertFalse(CalculateWorkedTime.isOpen(Arrays.asList(
                event(8, 0, GateDirection.IN),
                event(17, 0, GateDirection.OUT))));
    }
}
