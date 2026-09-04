package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.time.YearMonth;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.util.Result;

/**
 * Read-only access to what the gate recorded, plus the one write the employee is
 * allowed: disputing a day.
 *
 * <p>There is intentionally no {@code checkIn()} here and there never will be.
 * The gate already identified the employee by face; a second way to record
 * attendance would create a second source of truth, and the moment the two
 * disagree the audit trail is worthless.
 */
public final class AttendanceRepository {

    @NonNull
    private final SynergyApi api;

    public AttendanceRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    @NonNull
    public LiveData<Result<MonthSummary>> month(@NonNull YearMonth month) {
        return Calls.live(cb -> api.attendanceMonth(month, cb));
    }

    /** One day with its full gate-event timeline. */
    @NonNull
    public LiveData<Result<AttendanceDay>> day(@NonNull LocalDate date) {
        return Calls.live(cb -> api.attendanceDay(date, cb));
    }

    /**
     * Opens a review task with HR. Nothing on the attendance record changes —
     * only HR can act on it, and only in the HR system.
     */
    @NonNull
    public LiveData<Result<Boolean>> requestCorrection(@NonNull LocalDate date,
                                                       @NonNull String message) {
        return Calls.live(cb -> api.requestCorrection(date, message, cb));
    }
}
