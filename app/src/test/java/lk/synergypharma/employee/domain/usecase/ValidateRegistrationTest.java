package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Registration activates an existing employee record, so the identity step has
 * to accept every NIC actually in circulation at the plant — turning away a real
 * employee is the failure mode that matters here, not letting a typo through.
 */
public class ValidateRegistrationTest {

    private static final String CODE = "1213";
    private static final String NIC = "199512345678";

    // ------------------------------------------------------------- identity

    @Test
    public void acceptsTheNewTwelveDigitNic() {
        assertEquals(ValidateRegistration.IdentityError.NONE,
                ValidateRegistration.identity(CODE, "199512345678"));
    }

    @Test
    public void acceptsTheOldNineDigitNic() {
        // Anyone who joined before 2016 still carries this card.
        assertEquals(ValidateRegistration.IdentityError.NONE,
                ValidateRegistration.identity(CODE, "881234567V"));
        assertEquals(ValidateRegistration.IdentityError.NONE,
                ValidateRegistration.identity(CODE, "881234567X"));
    }

    @Test
    public void acceptsALowercaseOrSpacedNic() {
        assertTrue(ValidateRegistration.isValidNic("881234567v"));
        assertTrue(ValidateRegistration.isValidNic(" 1995 1234 5678 "));
    }

    @Test
    public void normalisesToASingleFormForTheServer() {
        assertEquals("881234567V", ValidateRegistration.normaliseNic(" 881234567v "));
    }

    @Test
    public void rejectsThingsThatAreNotNicNumbers() {
        assertFalse(ValidateRegistration.isValidNic("12345"));
        assertFalse(ValidateRegistration.isValidNic("88123456789"));   // 11 digits
        assertFalse(ValidateRegistration.isValidNic("881234567"));     // 9, no letter
        assertFalse(ValidateRegistration.isValidNic("19951234567A"));  // letter in new form
    }

    @Test
    public void requiresBothFields() {
        assertEquals(ValidateRegistration.IdentityError.NO_EMPLOYEE_CODE,
                ValidateRegistration.identity("  ", NIC));
        assertEquals(ValidateRegistration.IdentityError.NO_NIC,
                ValidateRegistration.identity(CODE, null));
        assertEquals(ValidateRegistration.IdentityError.NIC_FORMAT,
                ValidateRegistration.identity(CODE, "not-an-nic"));
    }

    // ------------------------------------------------------------- password

    @Test
    public void acceptsAReasonablePassword() {
        assertEquals(ValidateRegistration.PasswordError.NONE,
                ValidateRegistration.password(CODE, NIC, "plant2026", "plant2026"));
    }

    @Test
    public void rejectsAShortPassword() {
        assertEquals(ValidateRegistration.PasswordError.TOO_SHORT,
                ValidateRegistration.password(CODE, NIC, "abc123", "abc123"));
    }

    @Test
    public void rejectsAllDigitsAndAllLetters() {
        assertEquals(ValidateRegistration.PasswordError.NEEDS_LETTER_AND_DIGIT,
                ValidateRegistration.password(CODE, NIC, "12345678", "12345678"));
        assertEquals(ValidateRegistration.PasswordError.NEEDS_LETTER_AND_DIGIT,
                ValidateRegistration.password(CODE, NIC, "passsword", "passsword"));
    }

    @Test
    public void rejectsAPasswordBuiltFromTheEmployeeNumber() {
        // The one thing every colleague standing nearby already knows.
        assertEquals(ValidateRegistration.PasswordError.CONTAINS_IDENTITY,
                ValidateRegistration.password(CODE, NIC, "synergy1213", "synergy1213"));
    }

    @Test
    public void rejectsAPasswordBuiltFromTheNic() {
        assertEquals(ValidateRegistration.PasswordError.CONTAINS_IDENTITY,
                ValidateRegistration.password(CODE, NIC, "a199512345678", "a199512345678"));
    }

    @Test
    public void rejectsAMismatchedConfirmation() {
        assertEquals(ValidateRegistration.PasswordError.MISMATCH,
                ValidateRegistration.password(CODE, NIC, "plant2026", "plant2027"));
    }

    @Test
    public void reportsTheWeakestProblemFirst() {
        // A short password that also mismatches should say "too short" — telling
        // someone their passwords differ when neither would be accepted anyway
        // just sends them round the loop twice.
        assertEquals(ValidateRegistration.PasswordError.TOO_SHORT,
                ValidateRegistration.password(CODE, NIC, "ab1", "zz9"));
    }
}
