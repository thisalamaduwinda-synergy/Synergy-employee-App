package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** One item in the announcements feed. */
public final class AnnouncementDto {

    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("body")
    @Nullable
    public String body;

    /** Publishing department, e.g. "Quality Assurance". */
    @SerializedName("category")
    public String category;

    /** ISO-8601 with offset. */
    @SerializedName("published_at")
    public String publishedAt;

    @SerializedName("pinned")
    public boolean pinned;

    @SerializedName("action_needed")
    public boolean actionNeeded;

    @SerializedName("read")
    public boolean read;
}
