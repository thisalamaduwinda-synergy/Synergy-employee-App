package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * One row of the daily attendance sheet, plus the scans behind it.
 *
 * <p>Field names follow the recognition backend's {@code AttendanceOut} /
 * {@code AttendanceDetail} schemas so the employee API is a filter over the
 * existing table rather than a reshaping of it.
 */
public final class AttendanceDayDto {

    /** ISO date, {@code 2026-08-12}. */
    @SerializedName("work_date")
    public String workDate;

    /**
     * Backend {@code AttendanceStatus}: {@code present}, {@code late},
     * {@code half_day}, {@code absent}, {@code on_leave}, {@code holiday}.
     */
    @SerializedName("status")
    public String status;

    /** {@code HH:mm}. Sent alongside the timestamps so the app never guesses. */
    @SerializedName("shift_start")
    @Nullable
    public String shiftStart;

    @SerializedName("shift_end")
    @Nullable
    public String shiftEnd;

    @SerializedName("check_in_at")
    @Nullable
    public String checkInAt;

    @SerializedName("check_out_at")
    @Nullable
    public String checkOutAt;

    /** Net minutes credited for the day, after the break deduction. */
    @SerializedName("worked_minutes")
    public int workedMinutes;

    @SerializedName("break_minutes")
    public int breakMinutes;

    /** Every minute past the shift end, as measured. */
    @SerializedName("overtime_minutes")
    public int overtimeMinutes;

    /**
     * The part of the above that payroll actually credits, rounded to whole
     * blocks. The app shows this one — it is the figure on the payslip.
     */
    @SerializedName("overtime_counted_minutes")
    public int overtimeCountedMinutes;

    @SerializedName("late_minutes")
    public int lateMinutes;

    /** The gate saw an arrival but never a departure. */
    @SerializedName("missing_checkout")
    public boolean missingCheckout;

    @SerializedName("manually_adjusted")
    public boolean manuallyAdjusted;

    @SerializedName("adjusted_by")
    @Nullable
    public String adjustedBy;

    @SerializedName("adjustment_reason")
    @Nullable
    public String adjustmentReason;

    /** HR's note for the day, e.g. "Annual leave — approved". */
    @SerializedName("notes")
    @Nullable
    public String notes;

    /**
     * True once the employee has filed an explanation for a missing gate record.
     * Not on the HR schema — the employee API derives it from the absence
     * reasons table. Without it the home screen would keep nagging about a day
     * already sent to HR, which is the most annoying bug this app could ship.
     */
    @SerializedName("reason_submitted")
    public boolean reasonSubmitted;

    /**
     * Omitted by the month endpoint to keep the payload small; always present on
     * the single-day endpoint.
     */
    @SerializedName("scans")
    @Nullable
    public List<GateEventDto> scans;
}
