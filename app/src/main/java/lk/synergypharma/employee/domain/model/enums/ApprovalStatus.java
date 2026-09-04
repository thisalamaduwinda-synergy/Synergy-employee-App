package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import lk.synergypharma.employee.R;

/** Where a leave request, absence reason or OT claim sits with the approver. */
public enum ApprovalStatus {

    PENDING(R.string.status_pending, R.color.slate, R.color.slate_bg),
    APPROVED(R.string.status_approved, R.color.state_present, R.color.state_present_bg),
    REJECTED(R.string.status_rejected, R.color.state_absent, R.color.state_absent_bg),
    /** Sent back for more information — e.g. an unreadable medical certificate. */
    RETURNED(R.string.status_returned, R.color.synergy_blue_700, R.color.synergy_blue_50),
    CANCELLED(R.string.status_cancelled, R.color.muted, R.color.chip_grey_bg);

    @StringRes
    public final int labelRes;
    @ColorRes
    public final int fgColorRes;
    @ColorRes
    public final int bgColorRes;

    ApprovalStatus(@StringRes int labelRes, @ColorRes int fgColorRes, @ColorRes int bgColorRes) {
        this.labelRes = labelRes;
        this.fgColorRes = fgColorRes;
        this.bgColorRes = bgColorRes;
    }

    public boolean isOpen() {
        return this == PENDING || this == RETURNED;
    }

    public static ApprovalStatus fromApi(String raw) {
        if (raw == null) {
            return PENDING;
        }
        switch (raw.trim().toUpperCase()) {
            case "APPROVED":
                return APPROVED;
            case "REJECTED":
                return REJECTED;
            case "RETURNED":
                return RETURNED;
            case "CANCELLED":
                return CANCELLED;
            default:
                return PENDING;
        }
    }
}
