package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response of {@code GET /me/attendance?month=2026-08} — one employee's month.
 *
 * <p>The counters could be derived from {@link #days}, and are sent anyway: the
 * server is the authority on what "worked" means for payroll, and the app must
 * not quietly arrive at a different total than the payslip.
 */
public final class MonthSummaryDto {

    /** {@code yyyy-MM}. */
    @SerializedName("month")
    public String month;

    @SerializedName("worked_days")
    public int workedDays;

    /** Counted overtime for the month — the figure payroll credits. */
    @SerializedName("overtime_minutes")
    public int overtimeMinutes;

    @SerializedName("leave_days")
    public int leaveDays;

    @SerializedName("absent_days")
    public int absentDays;

    @SerializedName("days")
    @Nullable
    public List<AttendanceDayDto> days;
}
