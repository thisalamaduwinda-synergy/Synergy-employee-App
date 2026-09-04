package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A lightweight person row, used for the covering-officer and approver pickers
 * on the leave form. The full directory screen lands in v1.1.
 */
public final class Colleague {

    @NonNull
    public final String employeeId;
    @NonNull
    public final String fullName;
    @NonNull
    public final String designation;
    @NonNull
    public final String department;
    @Nullable
    public final String phone;
    /** True when this person can sign off leave — i.e. a valid approver. */
    public final boolean canApprove;

    public Colleague(@NonNull String employeeId,
                     @NonNull String fullName,
                     @NonNull String designation,
                     @NonNull String department,
                     @Nullable String phone,
                     boolean canApprove) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.designation = designation;
        this.department = department;
        this.phone = phone;
        this.canApprove = canApprove;
    }

    @NonNull
    public String initials() {
        return Employee.initialsOf(fullName);
    }
}
