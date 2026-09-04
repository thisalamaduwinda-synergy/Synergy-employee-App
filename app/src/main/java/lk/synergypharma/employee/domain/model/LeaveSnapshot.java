package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Everything the leave screens need, fetched together.
 *
 * <p>The balance, the holiday calendar and the existing requests are useless
 * apart: the apply form cannot count days without the holidays, and cannot
 * validate without the balance and the overlaps. One call, one render.
 */
public final class LeaveSnapshot {

    @NonNull
    public final LeaveBalance balance;
    /** Poya and mercantile days, straight from HR. */
    @NonNull
    public final Set<LocalDate> holidays;
    @NonNull
    public final List<LeaveRequest> requests;

    public LeaveSnapshot(@NonNull LeaveBalance balance,
                         @NonNull Set<LocalDate> holidays,
                         @NonNull List<LeaveRequest> requests) {
        this.balance = balance;
        this.holidays = Collections.unmodifiableSet(holidays);
        this.requests = Collections.unmodifiableList(requests);
    }
}
