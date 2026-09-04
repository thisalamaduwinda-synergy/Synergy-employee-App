package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import lk.synergypharma.employee.R;

/**
 * Leave buckets as the HR database holds them. Entitlements are <b>not</b> hard
 * coded here — they come down with the balance so HR can change policy without
 * a Play Store release.
 */
public enum LeaveType {

    ANNUAL("annual", R.string.leave_type_annual, R.color.synergy_blue_600, true),
    CASUAL("casual", R.string.leave_type_casual, R.color.synergy_blue_500, true),
    /**
     * The wire value is {@code sick}; the label says "Medical", which is what
     * staff and the certificate itself call it. Display language and wire
     * vocabulary are allowed to differ — HR's database gets to keep its name.
     */
    MEDICAL("sick", R.string.leave_type_medical, R.color.synergy_blue_800, true),
    NO_PAY("no_pay", R.string.leave_type_nopay, R.color.slate, false),

    // Not offered on the apply form, but history has to render them correctly.
    // Without these, an approved maternity leave would come back as "Annual".
    MATERNITY("maternity", R.string.leave_type_maternity, R.color.synergy_blue_800, true),
    SHORT_LEAVE("short_leave", R.string.leave_type_short, R.color.synergy_blue_500, true),
    OTHER("other", R.string.leave_type_other, R.color.slate, false);

    @NonNull
    private final String apiValue;
    @StringRes
    public final int labelRes;
    @ColorRes
    public final int colorRes;
    private final boolean tracksBalance;

    LeaveType(@NonNull String apiValue,
              @StringRes int labelRes,
              @ColorRes int colorRes,
              boolean tracksBalance) {
        this.apiValue = apiValue;
        this.labelRes = labelRes;
        this.colorRes = colorRes;
        this.tracksBalance = tracksBalance;
    }

    /** Types the employee can pick on the apply-for-leave form. */
    @NonNull
    public static LeaveType[] selectable() {
        return new LeaveType[]{ANNUAL, CASUAL, MEDICAL, NO_PAY};
    }

    /** No-pay and "other" have no entitlement to run down, so they show no bar. */
    public boolean hasBalance() {
        return tracksBalance;
    }

    @NonNull
    public String toApi() {
        return apiValue;
    }

    public static LeaveType fromApi(String raw) {
        if (raw == null) {
            return ANNUAL;
        }
        switch (raw.trim().toLowerCase().replace('-', '_').replace(' ', '_')) {
            case "casual":
                return CASUAL;
            case "sick":
            case "medical":
                return MEDICAL;
            case "no_pay":
            case "nopay":
                return NO_PAY;
            case "maternity":
                return MATERNITY;
            case "short_leave":
                return SHORT_LEAVE;
            case "other":
                return OTHER;
            default:
                return ANNUAL;
        }
    }
}
