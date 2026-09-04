package lk.synergypharma.employee.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Response of {@code GET /overtime?month=2026-08}. */
public final class OvertimeDto {

    public static final class Entry {
        @SerializedName("work_date")
        public String date;

        /** {@code HH:mm} of the final gate OUT. */
        @SerializedName("out_time")
        public String outTime;

        /** Counted minutes — the figure payroll credits, not the raw stay. */
        @SerializedName("minutes")
        public int minutes;

        /** {@code pending}, {@code approved}, {@code rejected}. */
        @SerializedName("status")
        public String status;
    }

    @SerializedName("month")
    public String month;

    @SerializedName("total_minutes")
    public int totalMinutes;

    @SerializedName("approved_minutes")
    public int approvedMinutes;

    @SerializedName("pending_minutes")
    public int pendingMinutes;

    @SerializedName("rate_multiplier")
    public float rateMultiplier;

    /** Provisional value in LKR, as a string so no rupee is lost to a float. */
    @SerializedName("estimated_value")
    public String estimatedValue;

    @SerializedName("entries")
    public List<Entry> entries;
}
