package lk.synergypharma.employee.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * One row of the backend's {@code leave_balances} table — per employee, per
 * year, per leave type. {@code GET /leave/balance} returns a list of these.
 *
 * <p>Entitlements travel with the balance so HR can change leave policy without
 * shipping a new APK.
 */
public final class LeaveBalanceDto {

    @SerializedName("year")
    public int year;

    /** {@code annual}, {@code casual}, {@code sick}, {@code no_pay}, … */
    @SerializedName("leave_type")
    public String leaveType;

    @SerializedName("entitled_days")
    public float entitledDays;

    @SerializedName("used_days")
    public float usedDays;

    /**
     * Unused days brought in from last year. Entitlement alone understates what
     * the employee actually has, so the app adds this in rather than showing a
     * balance payroll would disagree with.
     */
    @SerializedName("carried_forward")
    public float carriedForward;
}
