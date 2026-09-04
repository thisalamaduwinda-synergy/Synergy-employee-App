package lk.synergypharma.employee.domain.model.enums;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import lk.synergypharma.employee.R;

/**
 * What a single calendar day resolved to. One colour per state — this is the
 * only place the mapping lives, so the calendar, the day list and the summary
 * chips can never disagree with each other.
 */
public enum DayState {

    /** Gate recorded an IN and an OUT (or is still inside). */
    WORKED(R.string.day_state_worked, R.color.state_present, R.color.state_present_bg),

    /** Approved leave — the gate is expected to have nothing. */
    LEAVE(R.string.day_state_leave, R.color.state_leave, R.color.state_leave_bg),

    /** A working day with no gate record and no approved leave. Needs a reason. */
    ABSENT(R.string.day_state_absent, R.color.state_absent, R.color.state_absent_bg),

    /** Poya, mercantile or company holiday. */
    HOLIDAY(R.string.day_state_holiday, R.color.slate, R.color.slate_bg),

    /** Weekend or rostered off day. */
    OFF(R.string.day_state_off, R.color.calendar_off_text, R.color.calendar_off);

    @StringRes
    public final int labelRes;
    @ColorRes
    public final int fgColorRes;
    @ColorRes
    public final int bgColorRes;

    DayState(@StringRes int labelRes, @ColorRes int fgColorRes, @ColorRes int bgColorRes) {
        this.labelRes = labelRes;
        this.fgColorRes = fgColorRes;
        this.bgColorRes = bgColorRes;
    }

    /** True when the employee owes HR an explanation for this day. */
    public boolean needsExplanation() {
        return this == ABSENT;
    }

    /**
     * Accepts the recognition backend's {@code AttendanceStatus} vocabulary —
     * {@code present}, {@code late}, {@code half_day}, {@code absent},
     * {@code on_leave}, {@code holiday}.
     *
     * <p>Late and half-day both collapse into {@link #WORKED} on purpose. They
     * are payroll distinctions, not calendar ones: the employee <em>was</em>
     * there, and painting a late arrival the same colour as a day with no gate
     * record at all would be both wrong and alarming. Lateness travels
     * separately on {@code AttendanceDay.lateMinutes}.
     */
    public static DayState fromApi(String raw) {
        if (raw == null) {
            return OFF;
        }
        switch (raw.trim().toUpperCase().replace('-', '_').replace(' ', '_')) {
            case "PRESENT":
            case "LATE":
            case "HALF_DAY":
            case "WORKED":
                return WORKED;
            case "ON_LEAVE":
            case "LEAVE":
                return LEAVE;
            case "ABSENT":
                return ABSENT;
            case "HOLIDAY":
                return HOLIDAY;
            default:
                return OFF;
        }
    }
}
