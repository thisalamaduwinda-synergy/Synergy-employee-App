package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;
import lk.synergypharma.employee.domain.model.enums.LeaveType;

/** One leave application and where it got to. */
public final class LeaveRequest {

    /** How much of the first/last day is being taken. */
    public enum DayPortion {
        FULL_DAY(1.0f),
        HALF_AM(0.5f),
        HALF_PM(0.5f);

        public final float factor;

        DayPortion(float factor) {
            this.factor = factor;
        }

        public static DayPortion fromApi(String raw) {
            if (raw == null) {
                return FULL_DAY;
            }
            switch (raw.trim().toUpperCase().replace('-', '_')) {
                case "HALF_AM":
                    return HALF_AM;
                case "HALF_PM":
                    return HALF_PM;
                default:
                    return FULL_DAY;
            }
        }
    }

    @NonNull
    public final String id;
    @NonNull
    public final LeaveType type;
    @NonNull
    public final LocalDate fromDate;
    @NonNull
    public final LocalDate toDate;
    @NonNull
    public final DayPortion portion;
    /** Working days this request consumes, holidays already excluded by HR. */
    public final float days;
    @NonNull
    public final String reason;
    @Nullable
    public final String coveringOfficer;
    @Nullable
    public final String approver;
    @NonNull
    public final ApprovalStatus status;
    /** Approver's note — the reason a rejection was a rejection. */
    @Nullable
    public final String decisionNote;
    @Nullable
    public final LocalDateTime decidedAt;

    public LeaveRequest(@NonNull String id,
                        @NonNull LeaveType type,
                        @NonNull LocalDate fromDate,
                        @NonNull LocalDate toDate,
                        @NonNull DayPortion portion,
                        float days,
                        @NonNull String reason,
                        @Nullable String coveringOfficer,
                        @Nullable String approver,
                        @NonNull ApprovalStatus status,
                        @Nullable String decisionNote,
                        @Nullable LocalDateTime decidedAt) {
        this.id = id;
        this.type = type;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.portion = portion;
        this.days = days;
        this.reason = reason;
        this.coveringOfficer = coveringOfficer;
        this.approver = approver;
        this.status = status;
        this.decisionNote = decisionNote;
        this.decidedAt = decidedAt;
    }

    public boolean isSingleDay() {
        return fromDate.equals(toDate);
    }
}
