package lk.synergypharma.employee.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.LeaveBalance;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;
import lk.synergypharma.employee.domain.model.enums.LeaveType;

/** Stops the four mistakes that would otherwise bounce back from a supervisor. */
public class ValidateLeaveRequestTest {

    private static final LocalDate FROM = LocalDate.of(2026, 8, 25);
    private static final LocalDate TO = LocalDate.of(2026, 8, 26);

    private static LeaveBalance balance(float used, float entitled) {
        return new LeaveBalance(2026, Collections.singletonList(
                new LeaveBalance.Bucket(LeaveType.ANNUAL, used, entitled)));
    }

    private static LeaveRequest existing(LocalDate from, LocalDate to, ApprovalStatus status) {
        return new LeaveRequest("lv-1", LeaveType.ANNUAL, from, to,
                LeaveRequest.DayPortion.FULL_DAY, 1f, "reason",
                "Covering", "Approver", status, null, null);
    }

    @Test
    public void acceptsAWellFormedRequest() {
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 2f, "Family function in Kandy",
                "K. Jayasinghe", balance(14f, 21f), Collections.emptyList());

        assertTrue(outcome.isValid());
    }

    @Test
    public void rejectsAnEndDateBeforeTheStart() {
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, TO, FROM, 2f, "Family function",
                "K. Jayasinghe", balance(0f, 21f), Collections.emptyList());

        assertEquals(ValidateLeaveRequest.Error.END_BEFORE_START, outcome.error);
    }

    @Test
    public void rejectsAnEmptyReason() {
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 2f, "  ",
                "K. Jayasinghe", balance(0f, 21f), Collections.emptyList());

        assertEquals(ValidateLeaveRequest.Error.NO_REASON, outcome.error);
    }

    @Test
    public void rejectsAMissingCoveringOfficer() {
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 2f, "Family function",
                null, balance(0f, 21f), Collections.emptyList());

        assertEquals(ValidateLeaveRequest.Error.NO_COVERING_OFFICER, outcome.error);
    }

    @Test
    public void rejectsARequestBiggerThanTheBalanceAndReportsWhatIsLeft() {
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 5f, "Family function",
                "K. Jayasinghe", balance(19f, 21f), Collections.emptyList());

        assertEquals(ValidateLeaveRequest.Error.INSUFFICIENT_BALANCE, outcome.error);
        assertEquals(2f, outcome.remaining, 0.001f);
    }

    @Test
    public void allowsNoPayLeaveToExceedAnyBalance() {
        // No-pay has no entitlement to run down, so the balance check is skipped.
        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.NO_PAY, FROM, TO, 30f, "Extended family matter",
                "K. Jayasinghe", balance(21f, 21f), Collections.emptyList());

        assertTrue(outcome.isValid());
    }

    @Test
    public void rejectsAnOverlapWithAnApprovedRequest() {
        List<LeaveRequest> existing = Arrays.asList(
                existing(FROM, FROM, ApprovalStatus.APPROVED));

        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 2f, "Family function",
                "K. Jayasinghe", balance(0f, 21f), existing);

        assertEquals(ValidateLeaveRequest.Error.OVERLAPS_EXISTING, outcome.error);
    }

    @Test
    public void ignoresAnOverlapWithARejectedRequest() {
        // A rejected request is not booked time — the dates are free again.
        List<LeaveRequest> existing = Arrays.asList(
                existing(FROM, FROM, ApprovalStatus.REJECTED));

        ValidateLeaveRequest.Outcome outcome = ValidateLeaveRequest.validate(
                LeaveType.ANNUAL, FROM, TO, 2f, "Family function",
                "K. Jayasinghe", balance(0f, 21f), existing);

        assertTrue(outcome.isValid());
    }
}
