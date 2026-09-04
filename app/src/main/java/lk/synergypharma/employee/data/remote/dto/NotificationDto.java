package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * One notification row. The same shape arrives in the FCM {@code data} payload,
 * so a push and a pull render identically.
 */
public final class NotificationDto {

    @SerializedName("id")
    public String id;

    /**
     * {@code MISSING_GATE_RECORD}, {@code LEAVE_DECISION}, {@code OVERTIME},
     * {@code PAYSLIP}, {@code ANNOUNCEMENT}, {@code APPROVAL_REQUEST}.
     */
    @SerializedName("type")
    public String type;

    @SerializedName("title")
    public String title;

    @SerializedName("body")
    @Nullable
    public String body;

    /** ISO-8601 with offset. */
    @SerializedName("timestamp")
    public String timestamp;

    @SerializedName("read")
    public boolean read;

    /** ISO date the notification is about — what makes tapping it useful. */
    @SerializedName("target_date")
    @Nullable
    public String targetDate;
}
