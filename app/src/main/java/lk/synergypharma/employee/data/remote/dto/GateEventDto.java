package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * One accepted face-recognition scan.
 *
 * <p>Field names match the recognition backend's {@code AttendanceScanOut}
 * schema exactly, so the employee API can serve rows straight off
 * {@code attendance_scans} without a translation layer:
 *
 * <pre>
 * {
 *   "id": 412,
 *   "scanned_at": "2026-08-11 13:17:43.957009",
 *   "direction": "out",
 *   "confidence": 0.5303717454274496,
 *   "liveness": "real",
 *   "camera_id": 2,
 *   "camera_name": "Main Entrance - OUT",
 *   "image_path": "captures/2026-08-11/emp4_out_131743_d7e3971f.jpg",
 *   "source": "face_recognition",
 *   "is_duplicate": false
 * }
 * </pre>
 *
 * <p>Three things about this payload that the app has to handle and that are
 * easy to get wrong:
 * <ul>
 *   <li>{@code scanned_at} is a <b>naive</b> timestamp with a space separator
 *       and microseconds — not ISO-8601 with an offset. See {@code Wire}.</li>
 *   <li>{@code confidence} is a <b>cosine similarity</b>, not a percentage. The
 *       gate accepts at 0.45; real scans read 0.53–0.68.</li>
 *   <li>{@code image_path} is a path relative to the capture store, not a URL.
 *       The employee API has to sign it or proxy it.</li>
 * </ul>
 */
public final class GateEventDto {

    @SerializedName("id")
    public long id;

    /** {@code yyyy-MM-dd HH:mm:ss.ffffff} in plant local time. */
    @SerializedName("scanned_at")
    public String scannedAt;

    /** {@code in} or {@code out} — lowercase, as the backend's enum defines. */
    @SerializedName("direction")
    public String direction;

    /** Cosine similarity, 0.0–1.0. Never render this as a percentage. */
    @SerializedName("confidence")
    public float confidence;

    /** Anti-spoofing verdict: {@code real}, {@code fake}, {@code unchecked}. */
    @SerializedName("liveness")
    @Nullable
    public String liveness;

    @SerializedName("camera_id")
    @Nullable
    public Integer cameraId;

    /**
     * Human name of the camera, e.g. "Main Entrance - OUT". Not on the HR
     * schema — the employee API must join it in, because a bare camera id means
     * nothing to the person reading their own timeline.
     */
    @SerializedName("camera_name")
    @Nullable
    public String cameraName;

    @SerializedName("image_path")
    @Nullable
    public String imagePath;

    /** {@code face_recognition}, {@code manual}, {@code auto_checkout}, … */
    @SerializedName("source")
    @Nullable
    public String source;

    /** Landed inside the scan cooldown window and moved nothing. */
    @SerializedName("is_duplicate")
    public boolean isDuplicate;
}
