package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Overtime = how long after shift end the employee walked out of the gate.
 *
 * <p>Two rules that matter in practice:
 * <ul>
 *   <li>a grace window, so someone leaving at 17:04 does not generate a 4-minute
 *       OT claim that a supervisor then has to approve;</li>
 *   <li>rounding down to whole blocks, matching how payroll pays it.</li>
 * </ul>
 *
 * <p>The number this produces is <b>recorded</b>, not approved. A supervisor
 * still has to sign it off — the app never implies otherwise.
 */
public final class CalculateOvertime {

    /** Below this, leaving late is just leaving late. */
    public static final int GRACE_MINUTES = 15;

    /** Payroll pays OT in 15-minute blocks. */
    public static final int BLOCK_MINUTES = 15;

    private CalculateOvertime() {
    }

    /**
     * @param shiftEnd rostered end of the shift
     * @param lastOut  final gate OUT of the day, or null if the person is still inside
     * @return whole OT minutes, already rounded down to a payable block
     */
    public static int minutes(@NonNull LocalTime shiftEnd, @Nullable LocalTime lastOut) {
        if (lastOut == null || !lastOut.isAfter(shiftEnd)) {
            return 0;
        }
        long raw = Duration.between(shiftEnd, lastOut).toMinutes();
        if (raw < GRACE_MINUTES) {
            return 0;
        }
        return (int) (raw / BLOCK_MINUTES) * BLOCK_MINUTES;
    }
}
