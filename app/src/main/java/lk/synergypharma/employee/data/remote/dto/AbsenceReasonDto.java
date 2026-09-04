package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The employee's explanation for a missing gate record.
 *
 * <p><b>No table exists for this yet.</b> The recognition backend records that
 * somebody was absent but has nowhere to put why. This shape is the proposal:
 * one row per employee per date, with attachments, moving through the same
 * pending/approved/rejected states as leave.
 *
 * <p>Posted as {@code multipart/form-data}: this object as the {@code reason}
 * part, plus one {@code files} part per certificate.
 */
public final class AbsenceReasonDto {

    public static final class AttachmentDto {
        @SerializedName("file_name")
        public String fileName;

        @SerializedName("size_bytes")
        public long sizeBytes;

        @SerializedName("mime_type")
        public String mimeType;

        /** Set by the server once stored. */
        @SerializedName("url")
        @Nullable
        public String url;
    }

    @SerializedName("id")
    @Nullable
    public String id;

    /** ISO date of the day being explained. */
    @SerializedName("work_date")
    public String workDate;

    /** {@code medical}, {@code personal}, {@code emergency}, {@code official_duty}. */
    @SerializedName("reason_type")
    public String reasonType;

    @SerializedName("explanation")
    public String explanation;

    /** {@code pending}, {@code approved}, {@code rejected}, {@code returned}. */
    @SerializedName("status")
    @Nullable
    public String status;

    @SerializedName("submitted_at")
    @Nullable
    public String submittedAt;

    @SerializedName("attachments")
    @Nullable
    public List<AttachmentDto> attachments;
}
