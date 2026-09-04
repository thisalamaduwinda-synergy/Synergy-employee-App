package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;

import lk.synergypharma.employee.domain.model.enums.GateDirection;
import lk.synergypharma.employee.util.Constants;

/**
 * One face-recognition hit at a gate camera.
 *
 * <p>This is the app's single source of truth for attendance and it is strictly
 * read-only — the app never creates one. Showing {@link #matchScore} and
 * {@link #frameUrl} alongside the time is deliberate: it turns "the machine says
 * you were late" into something the employee can check for themselves.
 */
public final class GateEvent {

    @NonNull
    public final String id;
    @NonNull
    public final LocalDateTime timestamp;
    @NonNull
    public final GateDirection direction;
    /** Human name of the gate, e.g. "Main Gate", "Canteen Gate". */
    @NonNull
    public final String gateName;
    @NonNull
    public final String cameraId;
    /**
     * Cosine similarity from the recogniser, 0.0–1.0.
     *
     * <p><b>Not a percentage.</b> The gate accepts anything at or above
     * {@link Constants#FACE_MATCH_THRESHOLD} (0.45), and real accepted scans on
     * the plant cameras land around 0.53–0.68. Read it through
     * {@link #matchLabel()}, never as "68% sure".
     */
    public final float matchScore;
    /** Frame the camera captured. Null until the FR system exposes image URLs. */
    @Nullable
    public final String frameUrl;
    /**
     * The scan landed inside the recogniser's cooldown window and did not move
     * the attendance record. Kept for the audit trail, hidden from the employee.
     */
    public final boolean duplicate;

    public GateEvent(@NonNull String id,
                     @NonNull LocalDateTime timestamp,
                     @NonNull GateDirection direction,
                     @NonNull String gateName,
                     @NonNull String cameraId,
                     float matchScore,
                     @Nullable String frameUrl,
                     boolean duplicate) {
        this.id = id;
        this.timestamp = timestamp;
        this.direction = direction;
        this.gateName = gateName;
        this.cameraId = cameraId;
        this.matchScore = matchScore;
        this.frameUrl = frameUrl;
        this.duplicate = duplicate;
    }

    /**
     * "0.68" — the raw similarity, shown next to the gate and camera.
     *
     * <p>Deliberately not a percentage: on a cosine-similarity recogniser a
     * genuine, accepted match reads 0.55, and "match 55%" would make an
     * employee doubt a record that is in fact solid. The number is there so it
     * can be quoted to HR in a dispute; {@link #isBorderlineMatch()} is what
     * tells the employee whether it is worth quoting.
     */
    @NonNull
    public String matchLabel() {
        return String.format(java.util.Locale.US, "%.2f", matchScore);
    }

    /**
     * Accepted, but only just — within {@code FACE_MATCH_BORDERLINE_MARGIN} of
     * the threshold the gate uses to let anyone through at all.
     *
     * <p>This is the record most worth a second look: a borderline scan on the
     * wrong side of a shift boundary is exactly the kind of thing that costs
     * somebody an hour of overtime.
     */
    public boolean isBorderlineMatch() {
        return matchScore
                < Constants.FACE_MATCH_THRESHOLD + Constants.FACE_MATCH_BORDERLINE_MARGIN;
    }
}
