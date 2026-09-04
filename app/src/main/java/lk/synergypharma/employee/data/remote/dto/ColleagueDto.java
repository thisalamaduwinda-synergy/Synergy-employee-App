package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Directory row, used for the covering-officer and approver pickers. */
public final class ColleagueDto {

    @SerializedName("employee_code")
    public String employeeCode;

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("designation")
    @Nullable
    public String designation;

    @SerializedName("department")
    @Nullable
    public String department;

    @SerializedName("phone")
    @Nullable
    public String phone;

    /**
     * True when this person may sign off leave. Derived server side — the
     * recognition backend has no such flag today, so the employee API has to
     * decide it from designation or an explicit approver table.
     */
    @SerializedName("can_approve")
    public boolean canApprove;
}
