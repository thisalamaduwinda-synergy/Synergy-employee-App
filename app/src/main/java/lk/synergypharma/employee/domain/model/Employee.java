package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;

import lk.synergypharma.employee.domain.model.enums.UserRole;

/** A person as the HR employee table holds them. */
public final class Employee {

    /** Login key. Matches the HR record, e.g. {@code SPC-1042}. */
    @NonNull
    public final String employeeId;
    @NonNull
    public final String epfNumber;
    @NonNull
    public final String fullName;
    @NonNull
    public final String designation;
    @NonNull
    public final String department;
    /** Plant or branch the person is attached to, e.g. "Ratmalana plant". */
    @NonNull
    public final String location;
    @NonNull
    public final UserRole role;
    /** Rostered shift. OT is measured against {@link #shiftEnd}. */
    @NonNull
    public final LocalTime shiftStart;
    @NonNull
    public final LocalTime shiftEnd;
    @Nullable
    public final String phone;
    @Nullable
    public final String email;

    public Employee(@NonNull String employeeId,
                    @NonNull String epfNumber,
                    @NonNull String fullName,
                    @NonNull String designation,
                    @NonNull String department,
                    @NonNull String location,
                    @NonNull UserRole role,
                    @NonNull LocalTime shiftStart,
                    @NonNull LocalTime shiftEnd,
                    @Nullable String phone,
                    @Nullable String email) {
        this.employeeId = employeeId;
        this.epfNumber = epfNumber;
        this.fullName = fullName;
        this.designation = designation;
        this.department = department;
        this.location = location;
        this.role = role;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.phone = phone;
        this.email = email;
    }

    /** "Thisala Maduwinda" → "Thisala M." for the home-screen greeting. */
    @NonNull
    public String shortName() {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 2) {
            return fullName;
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    /** Two-letter avatar fallback used everywhere a photo would go. */
    @NonNull
    public String initials() {
        return initialsOf(fullName);
    }

    @NonNull
    public static String initialsOf(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
