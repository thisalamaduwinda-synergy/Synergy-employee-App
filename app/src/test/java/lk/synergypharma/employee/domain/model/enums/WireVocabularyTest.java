package lk.synergypharma.employee.domain.model.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The app's enums against the recognition backend's actual string vocabulary.
 *
 * <p>All of it is lowercase and snake_case there, and several names differ from
 * the app's — {@code sick} for medical leave, {@code on_leave} for a leave day.
 * A silent mismatch here does not crash: it renders a maternity leave as
 * "Annual" or paints an approved leave day red, which is far worse.
 */
public class WireVocabularyTest {

    @Test
    public void mapsEveryBackendAttendanceStatus() {
        assertEquals(DayState.WORKED, DayState.fromApi("present"));
        assertEquals(DayState.WORKED, DayState.fromApi("late"));
        assertEquals(DayState.WORKED, DayState.fromApi("half_day"));
        assertEquals(DayState.ABSENT, DayState.fromApi("absent"));
        assertEquals(DayState.LEAVE, DayState.fromApi("on_leave"));
        assertEquals(DayState.HOLIDAY, DayState.fromApi("holiday"));
    }

    @Test
    public void treatsAnUnknownStatusAsANonWorkingDay() {
        // A weekend has no attendance row at all; anything unrecognised must not
        // become an absence the employee is asked to explain.
        assertEquals(DayState.OFF, DayState.fromApi("off"));
        assertEquals(DayState.OFF, DayState.fromApi(null));
        assertEquals(DayState.OFF, DayState.fromApi("something_new"));
    }

    @Test
    public void aLateArrivalIsStillAWorkedDay() {
        assertFalse(DayState.fromApi("late").needsExplanation());
        assertTrue(DayState.fromApi("absent").needsExplanation());
    }

    @Test
    public void readsLowercaseGateDirections() {
        assertEquals(GateDirection.IN, GateDirection.fromApi("in"));
        assertEquals(GateDirection.OUT, GateDirection.fromApi("out"));
    }

    @Test
    public void mapsSickLeaveOntoTheMedicalLabel() {
        // The backend calls it "sick"; staff and the certificate call it medical.
        assertEquals(LeaveType.MEDICAL, LeaveType.fromApi("sick"));
        assertEquals(LeaveType.MEDICAL, LeaveType.fromApi("medical"));
        assertEquals("sick", LeaveType.MEDICAL.toApi());
    }

    @Test
    public void mapsTheLeaveTypesTheFormDoesNotOffer() {
        // These are raised by HR, never by the employee, but history has to
        // render them as themselves rather than defaulting to Annual.
        assertEquals(LeaveType.MATERNITY, LeaveType.fromApi("maternity"));
        assertEquals(LeaveType.SHORT_LEAVE, LeaveType.fromApi("short_leave"));
        assertEquals(LeaveType.OTHER, LeaveType.fromApi("other"));
        assertEquals(LeaveType.NO_PAY, LeaveType.fromApi("no_pay"));
    }

    @Test
    public void offersOnlyTheFourTypesAnEmployeeMayApplyFor() {
        assertEquals(4, LeaveType.selectable().length);
    }

    @Test
    public void mapsLowercaseLeaveStatuses() {
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromApi("pending"));
        assertEquals(ApprovalStatus.APPROVED, ApprovalStatus.fromApi("approved"));
        assertEquals(ApprovalStatus.REJECTED, ApprovalStatus.fromApi("rejected"));
        assertEquals(ApprovalStatus.CANCELLED, ApprovalStatus.fromApi("cancelled"));
    }

    @Test
    public void onlyASupervisorSeesApprovals() {
        assertTrue(UserRole.fromApi("supervisor").canApprove());
        assertFalse(UserRole.fromApi("employee").canApprove());
        // The recognition backend's own operator roles are not employee roles;
        // anything unrecognised must fall to the least privilege.
        assertFalse(UserRole.fromApi("security").canApprove());
        assertFalse(UserRole.fromApi(null).canApprove());
    }
}
