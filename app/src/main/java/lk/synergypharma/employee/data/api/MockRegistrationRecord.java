package lk.synergypharma.employee.data.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The identity facts registration checks against, kept in their own fixture.
 *
 * <p>Deliberately <b>not</b> on {@code EmployeeDto}: the NIC is needed once, to
 * prove a record belongs to the person claiming it, and putting it on the
 * profile payload would mean shipping everyone's national ID number down the
 * wire on every app launch for no reason.
 */
final class MockRegistrationRecord {

    @SerializedName("employee_code")
    String employeeCode;

    @SerializedName("nic")
    String nic;

    /** The number HR holds. The OTP goes here and nowhere else. */
    @SerializedName("phone")
    String phone;

    /** Stands in for "the row already has a password hash". */
    @SerializedName("already_registered")
    boolean alreadyRegistered;

    boolean matches(@NonNull String code, @NonNull String enteredNic) {
        return code.equalsIgnoreCase(trim(employeeCode))
                && trim(nic).equalsIgnoreCase(trim(enteredNic));
    }

    /**
     * {@code +94 71 234 5678} → {@code +94 7• ••• 5678}: recognisable to its
     * owner, useless to anyone who has guessed an employee number.
     */
    @NonNull
    String maskedPhone() {
        String raw = trim(phone).replace(" ", "");
        if (raw.length() < 8) {
            return "•••• ••••";
        }
        String head = raw.substring(0, 4);
        String tail = raw.substring(raw.length() - 4);
        return head + "• ••• " + tail;
    }

    @NonNull
    private static String trim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
