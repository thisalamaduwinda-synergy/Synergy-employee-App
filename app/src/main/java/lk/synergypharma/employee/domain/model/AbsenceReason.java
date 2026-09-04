package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;

/**
 * The employee's explanation for a day the gate has no record of.
 *
 * <p>This and the leave request are the <em>only</em> two things the app writes.
 * Everything else it shows is read-only, which is what keeps the
 * face-recognition system the single source of truth.
 */
public final class AbsenceReason {

    @Nullable
    public final String id;
    @NonNull
    public final LocalDate date;
    @NonNull
    public final AbsenceReasonType type;
    @NonNull
    public final String explanation;
    @NonNull
    public final List<Attachment> attachments;
    @NonNull
    public final ApprovalStatus status;
    @Nullable
    public final LocalDateTime submittedAt;

    public AbsenceReason(@Nullable String id,
                         @NonNull LocalDate date,
                         @NonNull AbsenceReasonType type,
                         @NonNull String explanation,
                         @NonNull List<Attachment> attachments,
                         @NonNull ApprovalStatus status,
                         @Nullable LocalDateTime submittedAt) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.explanation = explanation;
        this.attachments = Collections.unmodifiableList(new ArrayList<>(attachments));
        this.status = status;
        this.submittedAt = submittedAt;
    }

    /** Draft being filled in on screen, before it has an id from the server. */
    @NonNull
    public static AbsenceReason draft(@NonNull LocalDate date,
                                      @NonNull AbsenceReasonType type,
                                      @NonNull String explanation,
                                      @NonNull List<Attachment> attachments) {
        return new AbsenceReason(null, date, type, explanation, attachments,
                ApprovalStatus.PENDING, null);
    }
}
