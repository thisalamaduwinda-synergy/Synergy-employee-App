package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalTime;

/** OT is money, so the grace window and the rounding both have to be exact. */
public class CalculateOvertimeTest {

    private static final LocalTime SHIFT_END = LocalTime.of(17, 0);

    @Test
    public void isZeroWhenTheEmployeeLeftOnTime() {
        assertEquals(0, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 0)));
    }

    @Test
    public void isZeroWhenTheEmployeeLeftEarly() {
        assertEquals(0, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(16, 30)));
    }

    @Test
    public void ignoresAnythingInsideTheGraceWindow() {
        // Leaving at 17:04 is leaving late, not overtime — and it should not
        // create a four-minute claim for a supervisor to approve.
        assertEquals(0, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 4)));
        assertEquals(0, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 14)));
    }

    @Test
    public void roundsDownToAPayableBlock() {
        assertEquals(15, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 15)));
        assertEquals(15, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 29)));
        assertEquals(30, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(17, 30)));
        assertEquals(75, CalculateOvertime.minutes(SHIFT_END, LocalTime.of(18, 20)));
    }

    @Test
    public void isZeroWhileTheEmployeeIsStillInside() {
        assertEquals(0, CalculateOvertime.minutes(SHIFT_END, null));
    }
}
