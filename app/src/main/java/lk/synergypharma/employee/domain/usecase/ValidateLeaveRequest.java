package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;

import lk.synergypharma.employee.domain.model.LeaveBalance;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.enums.LeaveType;

/**
 * Catches the four mistakes that would otherwise reach a supervisor's queue and
 * come straight back. Server-side validation still runs — this only saves the
 * employee a round trip.
 */
public final class ValidateLeaveRequest {

    /** What went wrong, if anything. Field-level so the form can point at it. */
    public enum Error {
        NONE,
        END_BEFORE_START,
        NO_REASON,
        NO_COVERING_OFFICER,
        INSUFFICIENT_BALANCE,
        OVERLAPS_EXISTING
    }

    public static final class Outcome {
        @NonNull
        public final Error error;
        /** Days left in the bucket, populated when the error is a balance one. */
        public final float remaining;
        @Nullable
        public final LeaveType type;

        Outcome(@NonNull Error error, float remaining, @Nullable LeaveType type) {
            this.error = error;
            this.remaining = remaining;
            this.type = type;
        }

        public boolean isValid() {
            return error == Error.NONE;
        }
    }

    private static final Outcome OK = new Outcome(Error.NONE, 0f, null);

    private ValidateLeaveRequest() {
    }

    public static Outcome validate(@NonNull LeaveType type,
                                   @NonNull LocalDate from,
                                   @NonNull LocalDate to,
                                   float days,
                                   @Nullable String reason,
                                   @Nullable String coveringOfficer,
                                   @Nullable LeaveBalance balance,
                                   @NonNull List<LeaveRequest> existing) {
        if (to.isBefore(from)) {
            return new Outcome(Error.END_BEFORE_START, 0f, type);
        }
        if (reason == null || reason.trim().length() < 3) {
            return new Outcome(Error.NO_REASON, 0f, type);
        }
        if (coveringOfficer == null || coveringOfficer.trim().isEmpty()) {
            return new Outcome(Error.NO_COVERING_OFFICER, 0f, type);
        }

        // No-pay leave has nothing to run down, so it skips the balance check.
        if (type.hasBalance() && balance != null) {
            LeaveBalance.Bucket bucket = balance.bucketOf(type);
            float remaining = bucket == null ? 0f : bucket.remaining();
            if (days > remaining) {
                return new Outcome(Error.INSUFFICIENT_BALANCE, remaining, type);
            }
        }

        for (LeaveRequest r : existing) {
            if (!r.status.isOpen() && r.status != lk.synergypharma.employee.domain.model
                    .enums.ApprovalStatus.APPROVED) {
                continue;
            }
            boolean overlaps = !from.isAfter(r.toDate) && !to.isBefore(r.fromDate);
            if (overlaps) {
                return new Outcome(Error.OVERLAPS_EXISTING, 0f, type);
            }
        }

        return OK;
    }
}
