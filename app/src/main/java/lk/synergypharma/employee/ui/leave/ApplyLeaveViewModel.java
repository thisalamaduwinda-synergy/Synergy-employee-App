package lk.synergypharma.employee.ui.leave;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import lk.synergypharma.employee.domain.usecase.CountLeaveDays;
import lk.synergypharma.employee.domain.usecase.ValidateLeaveRequest;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.Result;

/**
 * The apply-for-leave form.
 *
 * <p>Day counting happens here rather than in the fragment so the total on
 * screen and the number sent to HR are produced by the same code path — and so
 * the Poya calendar that comes down with the balance is the only holiday list
 * either of them sees.
 */
public final class ApplyLeaveViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();

    private final Relay<LeaveSnapshot> snapshot = new Relay<>();
    private final Relay<List<Colleague>> colleagues = new Relay<>();
    private final Relay<LeaveRequest> submission = new Relay<>();

    private final MutableLiveData<Float> totalDays = new MutableLiveData<>(0f);

    private LeaveType type = LeaveType.ANNUAL;
    private LocalDate from = LocalDate.now().plusDays(1);
    private LocalDate to = LocalDate.now().plusDays(1);
    private LeaveRequest.DayPortion portion = LeaveRequest.DayPortion.FULL_DAY;
    @Nullable
    private String coveringOfficer;
    @Nullable
    private String approver;

    public ApplyLeaveViewModel() {
        snapshot.from(services.leave().snapshot());
        colleagues.from(services.directory().colleagues());
    }

    // ------------------------------------------------------------- streams

    @NonNull
    public LiveData<Result<LeaveSnapshot>> snapshot() {
        return snapshot.live();
    }

    @NonNull
    public LiveData<Result<List<Colleague>>> colleagues() {
        return colleagues.live();
    }

    @NonNull
    public LiveData<Result<LeaveRequest>> submission() {
        return submission.live();
    }

    @NonNull
    public LiveData<Float> totalDays() {
        return totalDays;
    }

    // -------------------------------------------------------------- form

    @NonNull
    public LeaveType type() {
        return type;
    }

    public void setType(@NonNull LeaveType type) {
        this.type = type;
    }

    @NonNull
    public LocalDate from() {
        return from;
    }

    @NonNull
    public LocalDate to() {
        return to;
    }

    /** Dragging the start date past the end date fixes the end date, not errors. */
    public void setFrom(@NonNull LocalDate value) {
        this.from = value;
        if (to.isBefore(from)) {
            to = from;
        }
        recount();
    }

    public void setTo(@NonNull LocalDate value) {
        this.to = value;
        recount();
    }

    @NonNull
    public LeaveRequest.DayPortion portion() {
        return portion;
    }

    public void setPortion(@NonNull LeaveRequest.DayPortion value) {
        this.portion = value;
        recount();
    }

    public void setCoveringOfficer(@Nullable String name) {
        this.coveringOfficer = name;
    }

    public void setApprover(@Nullable String name) {
        this.approver = name;
    }

    /** Recount once the holidays have arrived, not just when a date changes. */
    public void recount() {
        totalDays.setValue(CountLeaveDays.count(
                from, to, portion, holidays(), Constants.DEFAULT_REST_DAYS));
    }

    @NonNull
    private java.util.Set<LocalDate> holidays() {
        Result<LeaveSnapshot> current = snapshot.live().getValue();
        if (current == null || current.data == null) {
            return Collections.emptySet();
        }
        return current.data.holidays;
    }

    // ------------------------------------------------------------- submit

    @NonNull
    public ValidateLeaveRequest.Outcome validate(@Nullable String reason) {
        Result<LeaveSnapshot> current = snapshot.live().getValue();
        LeaveSnapshot data = current == null ? null : current.data;
        Float days = totalDays.getValue();

        return ValidateLeaveRequest.validate(
                type, from, to, days == null ? 0f : days, reason, coveringOfficer,
                data == null ? null : data.balance,
                data == null ? Collections.emptyList() : data.requests);
    }

    public void submit(@NonNull String reason) {
        Float days = totalDays.getValue();
        submission.from(services.leave().apply(
                type, from, to, portion, days == null ? 0f : days,
                reason, coveringOfficer, approver));
    }

    public void consumeSubmission() {
        submission.reset();
    }
}
