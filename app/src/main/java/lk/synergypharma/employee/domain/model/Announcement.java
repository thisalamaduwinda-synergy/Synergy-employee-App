package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;

/** A message from HR, Quality or Sales shown on the home feed. */
public final class Announcement {

    @NonNull
    public final String id;
    @NonNull
    public final String title;
    @Nullable
    public final String body;
    /** Publishing department, e.g. "Quality Assurance". */
    @NonNull
    public final String category;
    @NonNull
    public final LocalDateTime publishedAt;
    public final boolean pinned;
    /** Needs the employee to do something — register, confirm, acknowledge. */
    public final boolean actionNeeded;
    public final boolean read;

    public Announcement(@NonNull String id,
                        @NonNull String title,
                        @Nullable String body,
                        @NonNull String category,
                        @NonNull LocalDateTime publishedAt,
                        boolean pinned,
                        boolean actionNeeded,
                        boolean read) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.category = category;
        this.publishedAt = publishedAt;
        this.pinned = pinned;
        this.actionNeeded = actionNeeded;
        this.read = read;
    }
}
