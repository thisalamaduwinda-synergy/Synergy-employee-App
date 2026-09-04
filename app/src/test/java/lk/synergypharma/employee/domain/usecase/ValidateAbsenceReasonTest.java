package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;

/**
 * The form that decides whether someone keeps a day's pay, so it is strict on
 * purpose — HR would bounce these anyway, by which time the seven-day window may
 * have closed.
 */
public class ValidateAbsenceReasonTest {

    private static Attachment file(long bytes) {
        return new Attachment("cert.jpg", bytes, "image/jpeg", "file:///tmp/cert.jpg", null);
    }

    @Test
    public void acceptsAMedicalReasonWithACertificate() {
        List<Attachment> files = Arrays.asList(file(400 * 1024));

        assertEquals(ValidateAbsenceReason.Error.NONE, ValidateAbsenceReason.validate(
                AbsenceReasonType.MEDICAL, "Fever, advised one day rest", files));
    }

    @Test
    public void rejectsAMedicalReasonWithNoCertificate() {
        assertEquals(ValidateAbsenceReason.Error.MISSING_DOCUMENT,
                ValidateAbsenceReason.validate(
                        AbsenceReasonType.MEDICAL, "Fever, advised one day rest",
                        Collections.emptyList()));
    }

    @Test
    public void acceptsANonMedicalReasonWithNoCertificate() {
        assertEquals(ValidateAbsenceReason.Error.NONE, ValidateAbsenceReason.validate(
                AbsenceReasonType.PERSONAL, "Family emergency at home",
                Collections.emptyList()));
    }

    @Test
    public void rejectsAMissingReasonType() {
        assertEquals(ValidateAbsenceReason.Error.NO_TYPE, ValidateAbsenceReason.validate(
                null, "Fever", Collections.emptyList()));
    }

    @Test
    public void rejectsAnExplanationTooShortToBeUseful() {
        assertEquals(ValidateAbsenceReason.Error.NO_EXPLANATION,
                ValidateAbsenceReason.validate(
                        AbsenceReasonType.PERSONAL, "ok", Collections.emptyList()));
    }

    @Test
    public void rejectsAFileOverTheUploadLimit() {
        List<Attachment> files = Arrays.asList(
                file(ValidateAbsenceReason.MAX_FILE_BYTES + 1));

        assertEquals(ValidateAbsenceReason.Error.FILE_TOO_LARGE,
                ValidateAbsenceReason.validate(
                        AbsenceReasonType.MEDICAL, "Fever, advised rest", files));
    }
}
