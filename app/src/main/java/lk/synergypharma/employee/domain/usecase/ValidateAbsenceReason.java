package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;

/**
 * Guards the one form that decides whether someone keeps a day's pay, so it is
 * strict on purpose: a medical reason with no certificate will be bounced by HR
 * anyway, and by then the seven-day window may have closed.
 */
public final class ValidateAbsenceReason {

    /** HR's window for explaining a missing gate record. */
    public static final int SUBMISSION_WINDOW_DAYS = 7;

    /** Plant staff are often on mobile data — anything larger gets compressed. */
    public static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;

    public enum Error {
        NONE,
        NO_TYPE,
        NO_EXPLANATION,
        MISSING_DOCUMENT,
        FILE_TOO_LARGE
    }

    private ValidateAbsenceReason() {
    }

    public static Error validate(@Nullable AbsenceReasonType type,
                                 @Nullable String explanation,
                                 @NonNull List<Attachment> attachments) {
        if (type == null) {
            return Error.NO_TYPE;
        }
        if (explanation == null || explanation.trim().length() < 5) {
            return Error.NO_EXPLANATION;
        }
        if (type.requiresDocument && attachments.isEmpty()) {
            return Error.MISSING_DOCUMENT;
        }
        for (Attachment a : attachments) {
            if (a.sizeBytes > MAX_FILE_BYTES) {
                return Error.FILE_TOO_LARGE;
            }
        }
        return Error.NONE;
    }
}
