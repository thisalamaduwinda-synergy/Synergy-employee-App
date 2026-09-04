package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * A non-working day, from the backend's {@code holidays} table.
 *
 * <p>Sent down rather than baked into the APK because the Poya calendar moves
 * every year — a hard-coded list would silently start charging people annual
 * leave for a company holiday.
 */
public final class HolidayDto {

    /** ISO date. */
    @SerializedName("holiday_date")
    public String holidayDate;

    @SerializedName("name")
    public String name;

    /**
     * {@code public}, {@code mercantile}, {@code poya}, {@code company},
     * {@code weekend}.
     */
    @SerializedName("holiday_type")
    @Nullable
    public String holidayType;

    /** An unpaid holiday still costs no leave, so the app ignores this today. */
    @SerializedName("is_paid")
    public boolean isPaid;
}
