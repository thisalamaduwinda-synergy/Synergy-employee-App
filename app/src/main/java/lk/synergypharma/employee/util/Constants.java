package lk.synergypharma.employee.util;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Values that more than one layer needs to agree on. */
public final class Constants {

    private Constants() {
    }

    /**
     * Gate timestamps arrive with an offset, but everything the employee reads —
     * "you walked in at 08:12" — must be in plant local time regardless of where
     * the phone thinks it is.
     */
    public static final ZoneId PLANT_ZONE = ZoneId.of("Asia/Colombo");

    /**
     * Default roster. HR sends the real rest days per employee once the API
     * exists; until then Saturday/Sunday matches the Colombo office.
     */
    public static final Set<DayOfWeek> DEFAULT_REST_DAYS =
            Collections.unmodifiableSet(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

    /**
     * Cosine similarity at which the gate accepts a face as a match — the
     * recogniser's {@code FACE_MATCH_THRESHOLD}.
     *
     * <p>This is not a percentage and must never be shown as one. Real accepted
     * scans on the plant cameras sit around 0.53–0.68; rendering that as
     * "match 53%" would tell an employee their own attendance record is a coin
     * flip. What the number means is only legible <em>relative to this
     * threshold</em>, which is why the app carries it.
     *
     * <p>Move it into the login response when the API exists — retuning the
     * recogniser must not need a Play Store release.
     */
    public static final float FACE_MATCH_THRESHOLD = 0.45f;

    /**
     * How far above the accept threshold a scan has to sit before it stops
     * being worth a second look. Inside this band the match was accepted, but
     * it is the record an employee is most likely to want to dispute.
     */
    public static final float FACE_MATCH_BORDERLINE_MARGIN = 0.10f;

    /** Certificate photos are squeezed under this before they leave the phone. */
    public static final long UPLOAD_TARGET_BYTES = 500L * 1024L;

    public static final String MIME_IMAGE = "image/*";
    public static final String MIME_PDF = "application/pdf";

    /**
     * HR help desk. Hard-coded for v1; move it into the login response as soon
     * as the API exists, so a change of extension does not need a Play Store
     * release.
     */
    public static final String HR_CONTACT_NUMBER = "+94112635000";

    /** Bundle keys for fragment arguments. */
    public static final String ARG_DATE = "arg_date";
    public static final String ARG_MONTH = "arg_month";
}
