package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Guards the two forms in first-time registration.
 *
 * <p>Registration here <b>activates an existing employee record</b>; it never
 * creates one. Everybody is already in the recognition system — HR enrolled
 * their face — and the only thing missing is a password. So the identity step
 * is not "who are you", it is "prove you are the person this record belongs
 * to", which is why it asks for the NIC HR already holds rather than anything
 * the employee could choose.
 */
public final class ValidateRegistration {

    /**
     * Sri Lankan NIC, both forms in circulation:
     * old {@code 881234567V} (9 digits + V or X) and new {@code 198812345678}
     * (12 digits). Staff who joined before 2016 still carry the old card, so
     * accepting only the new one would lock out most of the plant.
     */
    private static final Pattern NIC_OLD = Pattern.compile("^\\d{9}[VvXx]$");
    private static final Pattern NIC_NEW = Pattern.compile("^\\d{12}$");

    public static final int MIN_PASSWORD_LENGTH = 8;

    public enum IdentityError {
        NONE,
        NO_EMPLOYEE_CODE,
        NO_NIC,
        NIC_FORMAT
    }

    public enum PasswordError {
        NONE,
        TOO_SHORT,
        NEEDS_LETTER_AND_DIGIT,
        CONTAINS_IDENTITY,
        MISMATCH
    }

    private ValidateRegistration() {
    }

    // --------------------------------------------------------------- step 1

    @NonNull
    public static IdentityError identity(@Nullable String employeeCode, @Nullable String nic) {
        if (isBlank(employeeCode)) {
            return IdentityError.NO_EMPLOYEE_CODE;
        }
        if (isBlank(nic)) {
            return IdentityError.NO_NIC;
        }
        if (!isValidNic(nic.trim())) {
            return IdentityError.NIC_FORMAT;
        }
        return IdentityError.NONE;
    }

    public static boolean isValidNic(@NonNull String nic) {
        String cleaned = nic.trim().replace(" ", "");
        return NIC_OLD.matcher(cleaned).matches() || NIC_NEW.matcher(cleaned).matches();
    }

    /** {@code 881234567v} → {@code 881234567V}, so the server sees one form. */
    @NonNull
    public static String normaliseNic(@NonNull String nic) {
        return nic.trim().replace(" ", "").toUpperCase();
    }

    // --------------------------------------------------------------- step 3

    /**
     * Password rules kept to what plant and warehouse staff can actually
     * remember and type on a phone: eight characters with at least one letter
     * and one digit. Anything stricter gets written on the back of an ID badge,
     * which is worse than a weaker password.
     *
     * <p>The one hard rule is that it must not contain the employee code or the
     * NIC — those are the two things a colleague standing next to them already
     * knows.
     */
    @NonNull
    public static PasswordError password(@Nullable String employeeCode,
                                         @Nullable String nic,
                                         @Nullable String password,
                                         @Nullable String confirmation) {
        String value = password == null ? "" : password;

        if (value.length() < MIN_PASSWORD_LENGTH) {
            return PasswordError.TOO_SHORT;
        }
        if (!hasLetter(value) || !hasDigit(value)) {
            return PasswordError.NEEDS_LETTER_AND_DIGIT;
        }
        if (containsIgnoreCase(value, employeeCode) || containsIgnoreCase(value, nic)) {
            return PasswordError.CONTAINS_IDENTITY;
        }
        if (!value.equals(confirmation)) {
            return PasswordError.MISMATCH;
        }
        return PasswordError.NONE;
    }

    // ----------------------------------------------------------------- utils

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasLetter(@NonNull String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(@NonNull String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(@NonNull String haystack, @Nullable String needle) {
        if (isBlank(needle) || needle.trim().length() < 3) {
            return false;
        }
        return haystack.toLowerCase().contains(needle.trim().toLowerCase());
    }
}
