package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;

import lk.synergypharma.employee.domain.model.enums.NotificationType;

/**
 * One row in the notifications list.
 *
 * <p>{@link #targetDate} is what makes a notification actionable: tapping
 * "No gate record for 11 Aug" must land on the absence form already filled in
 * with that date, not on a generic screen.
 */
public final class AppNotification {

    @NonNull
    public final String id;
    @NonNull
    public final NotificationType type;
    @NonNull
    public final String title;
    @Nullable
    public final String body;
    @NonNull
    public final LocalDateTime timestamp;
    public final boolean read;
    /** ISO date the notification refers to, when it refers to one. */
    @Nullable
    public final String targetDate;

    public AppNotification(@NonNull String id,
                           @NonNull NotificationType type,
                           @NonNull String title,
                           @Nullable String body,
                           @NonNull LocalDateTime timestamp,
                           boolean read,
                           @Nullable String targetDate) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.read = read;
        this.targetDate = targetDate;
    }
}
