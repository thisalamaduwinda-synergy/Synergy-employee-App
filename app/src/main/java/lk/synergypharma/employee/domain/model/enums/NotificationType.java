package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import lk.synergypharma.employee.R;

/**
 * Drives the icon and tint on the notifications list, and later the FCM
 * {@code data.type} field so a push can deep-link to the right screen.
 */
public enum NotificationType {

    MISSING_GATE_RECORD(R.drawable.ic_warning, R.color.state_absent, R.color.state_absent_bg),
    LEAVE_DECISION(R.drawable.ic_check, R.color.state_present, R.color.state_present_bg),
    OVERTIME(R.drawable.ic_clock, R.color.slate, R.color.slate_bg),
    PAYSLIP(R.drawable.ic_wallet, R.color.synergy_blue_700, R.color.synergy_blue_50),
    ANNOUNCEMENT(R.drawable.ic_megaphone, R.color.synergy_blue_700, R.color.synergy_blue_50),
    APPROVAL_REQUEST(R.drawable.ic_users, R.color.synergy_blue_800, R.color.synergy_blue_50);

    @DrawableRes
    public final int iconRes;
    @ColorRes
    public final int fgColorRes;
    @ColorRes
    public final int bgColorRes;

    NotificationType(@DrawableRes int iconRes, @ColorRes int fgColorRes, @ColorRes int bgColorRes) {
        this.iconRes = iconRes;
        this.fgColorRes = fgColorRes;
        this.bgColorRes = bgColorRes;
    }

    public static NotificationType fromApi(String raw) {
        if (raw == null) {
            return ANNOUNCEMENT;
        }
        switch (raw.trim().toUpperCase()) {
            case "MISSING_GATE_RECORD":
                return MISSING_GATE_RECORD;
            case "LEAVE_DECISION":
                return LEAVE_DECISION;
            case "OVERTIME":
                return OVERTIME;
            case "PAYSLIP":
                return PAYSLIP;
            case "APPROVAL_REQUEST":
                return APPROVAL_REQUEST;
            default:
                return ANNOUNCEMENT;
        }
    }
}
