package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.StringRes;

import lk.synergypharma.employee.R;

/** Why the gate has no record for a working day. */
public enum AbsenceReasonType {

    /** The only type that must carry a certificate — see ValidateAbsenceReason. */
    MEDICAL(R.string.reason_medical, true),
    PERSONAL(R.string.reason_personal, false),
    EMERGENCY(R.string.reason_emergency, false),
    /** Off site on company business: a customer visit, an audit, a training day. */
    OFFICIAL_DUTY(R.string.reason_official, false);

    @StringRes
    public final int labelRes;
    public final boolean requiresDocument;

    AbsenceReasonType(@StringRes int labelRes, boolean requiresDocument) {
        this.labelRes = labelRes;
        this.requiresDocument = requiresDocument;
    }

    /** Lowercase on the wire, matching the backend's other string enums. */
    public String toApi() {
        return name().toLowerCase();
    }

    public static AbsenceReasonType fromApi(String raw) {
        if (raw == null) {
            return PERSONAL;
        }
        switch (raw.trim().toLowerCase().replace('-', '_').replace(' ', '_')) {
            case "medical":
            case "sick":
                return MEDICAL;
            case "emergency":
                return EMERGENCY;
            case "official_duty":
            case "official":
                return OFFICIAL_DUTY;
            default:
                return PERSONAL;
        }
    }
}
