package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.enums.DayState;
import lk.synergypharma.employee.domain.model.enums.GateDirection;

/**
 * The precedence here is the part that matters: an employee on approved leave
 * must never be painted as absent. That is the mistake that generates the angry
 * phone call to HR.
 */
public class ResolveDayStateTest {

    private static final Set<DayOfWeek> WEEKEND =
            EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    /** A Wednesday safely in the past. */
    private static final LocalDate PAST_WEEKDAY = LocalDate.of(2026, 8, 5);

    private static List<GateEvent> oneEvent(LocalDate day) {
        return Arrays.asList(new GateEvent("e1",
                LocalDateTime.of(day, LocalTime.of(8, 12)),
                GateDirection.IN, "Main Entrance - IN", "1", 0.63f, null, false));
    }

    @Test
    public void aDayWithGateEventsIsWorked() {
        assertEquals(DayState.WORKED, ResolveDayState.resolve(
                PAST_WEEKDAY, oneEvent(PAST_WEEKDAY), false,
                Collections.emptySet(), WEEKEND));
    }

    @Test
    public void approvedLeaveBeatsEverythingElse() {
        assertEquals(DayState.LEAVE, ResolveDayState.resolve(
                PAST_WEEKDAY, Collections.emptyList(), true,
                Collections.emptySet(), WEEKEND));
    }

    @Test
    public void aHolidayIsNotAnAbsence() {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(PAST_WEEKDAY);

        assertEquals(DayState.HOLIDAY, ResolveDayState.resolve(
                PAST_WEEKDAY, Collections.emptyList(), false, holidays, WEEKEND));
    }

    @Test
    public void aRestDayIsNotAnAbsence() {
        LocalDate saturday = LocalDate.of(2026, 8, 8);

        assertEquals(DayState.OFF, ResolveDayState.resolve(
                saturday, Collections.emptyList(), false,
                Collections.emptySet(), WEEKEND));
    }

    @Test
    public void aPastWorkingDayWithNoRecordIsAbsent() {
        assertEquals(DayState.ABSENT, ResolveDayState.resolve(
                PAST_WEEKDAY, Collections.emptyList(), false,
                Collections.emptySet(), WEEKEND));
    }

    @Test
    public void aFutureWorkingDayHasSimplyNotHappenedYet() {
        LocalDate future = LocalDate.now().plusDays(30);
        // Picked so it cannot land on a weekend and pass for the wrong reason.
        while (WEEKEND.contains(future.getDayOfWeek())) {
            future = future.plusDays(1);
        }

        assertEquals(DayState.OFF, ResolveDayState.resolve(
                future, Collections.emptyList(), false,
                Collections.emptySet(), WEEKEND));
    }
}
