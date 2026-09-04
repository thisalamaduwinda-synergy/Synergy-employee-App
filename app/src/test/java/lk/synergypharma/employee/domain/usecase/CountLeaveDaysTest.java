package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import lk.synergypharma.employee.domain.model.LeaveRequest;

/**
 * Nobody should burn annual leave on a day the plant was shut anyway, so the
 * weekend and Poya exclusions are the whole point of this class.
 */
public class CountLeaveDaysTest {

    private static final Set<DayOfWeek> WEEKEND =
            EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    // 2026-08-24 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 24);

    @Test
    public void countsWholeWorkingDays() {
        float days = CountLeaveDays.count(MONDAY, MONDAY.plusDays(2),
                LeaveRequest.DayPortion.FULL_DAY, Collections.emptySet(), WEEKEND);

        assertEquals(3f, days, 0.001f);
    }

    @Test
    public void skipsTheWeekend() {
        // Monday to the following Monday spans eight days but only six are worked.
        float days = CountLeaveDays.count(MONDAY, MONDAY.plusDays(7),
                LeaveRequest.DayPortion.FULL_DAY, Collections.emptySet(), WEEKEND);

        assertEquals(6f, days, 0.001f);
    }

    @Test
    public void skipsAPoyaDayInTheMiddleOfTheRange() {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(MONDAY.plusDays(1));

        float days = CountLeaveDays.count(MONDAY, MONDAY.plusDays(2),
                LeaveRequest.DayPortion.FULL_DAY, holidays, WEEKEND);

        assertEquals(2f, days, 0.001f);
    }

    @Test
    public void chargesHalfADayForASingleDayHalfRequest() {
        float days = CountLeaveDays.count(MONDAY, MONDAY,
                LeaveRequest.DayPortion.HALF_AM, Collections.emptySet(), WEEKEND);

        assertEquals(0.5f, days, 0.001f);
    }

    @Test
    public void ignoresTheHalfDayFlagOverARange() {
        // Away for three whole days regardless of which half was ticked.
        float days = CountLeaveDays.count(MONDAY, MONDAY.plusDays(2),
                LeaveRequest.DayPortion.HALF_PM, Collections.emptySet(), WEEKEND);

        assertEquals(3f, days, 0.001f);
    }

    @Test
    public void costsNothingWhenTheRangeIsAllWeekend() {
        LocalDate saturday = MONDAY.minusDays(2);

        float days = CountLeaveDays.count(saturday, saturday.plusDays(1),
                LeaveRequest.DayPortion.FULL_DAY, Collections.emptySet(), WEEKEND);

        assertEquals(0f, days, 0.001f);
    }

    @Test
    public void returnsZeroWhenTheEndIsBeforeTheStart() {
        float days = CountLeaveDays.count(MONDAY, MONDAY.minusDays(3),
                LeaveRequest.DayPortion.FULL_DAY, Collections.emptySet(), WEEKEND);

        assertEquals(0f, days, 0.001f);
    }
}
