package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * One row of the recognition backend's {@code employees} table, flattened.
 *
 * <p>{@code employee_code} is the login key and, at Synergy, is the EPF number —
 * the enrolled face images on the server are named after it ({@code 1213.jpg}).
 * Codes are numeric strings, with a handful of older {@code SYN####} records
 * still around, so it is carried as a string and never parsed as an int.
 */
public final class EmployeeDto {

    @SerializedName("employee_code")
    public String employeeCode;

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("designation")
    @Nullable
    public String designation;

    /** Department name, already resolved from {@code department_id}. */
    @SerializedName("department")
    @Nullable
    public String department;

    /** Branch name, e.g. "Ratmalana plant". Resolved from {@code branch_id}. */
    @SerializedName("branch")
    @Nullable
    public String branch;

    /** {@code active}, {@code inactive}, {@code suspended}, {@code resigned}. */
    @SerializedName("status")
    @Nullable
    public String status;

    /**
     * Not on the recognition backend at all: it only knows {@code admin},
     * {@code hr} and {@code security} users. The employee API has to supply
     * {@code employee} or {@code supervisor}, and re-check it server side on
     * every approval call — this field only decides what is on screen.
     */
    @SerializedName("role")
    @Nullable
    public String role;

    /** {@code HH:mm}, from the employee's default shift. */
    @SerializedName("shift_start")
    @Nullable
    public String shiftStart;

    @SerializedName("shift_end")
    @Nullable
    public String shiftEnd;

    @SerializedName("phone")
    @Nullable
    public String phone;

    @SerializedName("email")
    @Nullable
    public String email;

    /**
     * Chairman, directors and anyone else whose movements are hidden from the
     * shared dashboard. Their own attendance is recorded and shown to them
     * normally — the flag only ever restricts what <em>other</em> people see, so
     * the app carries it purely so a future team screen can honour it.
     */
    @SerializedName("is_vip")
    public boolean isVip;
}
