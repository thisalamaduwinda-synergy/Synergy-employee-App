package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * A leave application, as sent and as returned. Matches the backend's
 * {@code LeaveOut} / {@code LeaveCreate} schemas.
 *
 * <p>Two fields here have <b>no column on the backend yet</b> and need adding
 * before the swap: {@link #halfDayPeriod} and {@link #coveringOfficer}. Both are
 * standard on a Sri Lankan leave form and both are already on the app's screen,
 * so they are sent regardless — a server that ignores them loses information,
 * but nothing breaks.
 */
public final class LeaveRequestDto {

    @SerializedName("id")
    @Nullable
    public String id;

    /** {@code annual}, {@code casual}, {@code sick}, {@code no_pay}, … */
    @SerializedName("leave_type")
    public String leaveType;

    @SerializedName("start_date")
    public String startDate;

    @SerializedName("end_date")
    public String endDate;

    @SerializedName("is_half_day")
    public boolean isHalfDay;

    /** {@code AM} or {@code PM}. Needs a new column — see the class note. */
    @SerializedName("half_day_period")
    @Nullable
    public String halfDayPeriod;

    /** Working days consumed. The server is authoritative on this. */
    @SerializedName("total_days")
    public float totalDays;

    @SerializedName("reason")
    @Nullable
    public String reason;

    /** Needs a new column — see the class note. */
    @SerializedName("covering_officer")
    @Nullable
    public String coveringOfficer;

    /**
     * Who the employee is asking, sent on create. Distinct from
     * {@link #approvedBy}, which is who actually signed it and is set by the
     * server. Also needs a new column.
     */
    @SerializedName("approver")
    @Nullable
    public String approver;

    /** {@code pending}, {@code approved}, {@code rejected}, {@code cancelled}. */
    @SerializedName("status")
    @Nullable
    public String status;

    @SerializedName("approved_by")
    @Nullable
    public String approvedBy;

    @SerializedName("approved_at")
    @Nullable
    public String approvedAt;

    /** The only genuinely useful part of a rejection. */
    @SerializedName("rejection_reason")
    @Nullable
    public String rejectionReason;

    @SerializedName("created_at")
    @Nullable
    public String createdAt;
}
