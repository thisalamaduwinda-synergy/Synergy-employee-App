package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.time.LocalDate;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import lk.synergypharma.employee.util.Result;

/** Leave balance, history and applications. */
public final class LeaveRepository {

    @NonNull
    private final SynergyApi api;

    public LeaveRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    /**
     * Balance, holidays and existing requests in one shot — the apply form needs
     * all three before it can count days or reject an overlap.
     */
    @NonNull
    public LiveData<Result<LeaveSnapshot>> snapshot() {
        return Calls.live(api::leaveSnapshot);
    }

    @NonNull
    public LiveData<Result<LeaveRequest>> apply(@NonNull LeaveType type,
                                                @NonNull LocalDate from,
                                                @NonNull LocalDate to,
                                                @NonNull LeaveRequest.DayPortion portion,
                                                float days,
                                                @NonNull String reason,
                                                @Nullable String coveringOfficer,
                                                @Nullable String approver) {
        return Calls.live(cb -> api.applyLeave(type, from, to, portion, days, reason,
                coveringOfficer, approver, cb));
    }
}
