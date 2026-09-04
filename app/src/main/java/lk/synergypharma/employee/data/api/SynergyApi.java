package lk.synergypharma.employee.data.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.domain.model.RegistrationChallenge;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.domain.model.enums.LeaveType;

/**
 * Everything the app can ask for, in the app's own vocabulary.
 *
 * <p>This is the seam that lets all fourteen screens be built and demonstrated
 * before the HR backend exists. {@code MockSynergyApi} implements it against
 * generated data today; a Retrofit-backed implementation over
 * {@code ApiService} replaces it later, and no ViewModel or Fragment changes —
 * only the one line in {@code ServiceLocator}.
 *
 * <p>Note what is <b>missing</b>: there is no way to create a gate event. The
 * face-recognition system is the single source of truth for attendance, and an
 * app that could write to it would be a second, conflicting one.
 */
public interface SynergyApi {

    // ------------------------------------------------------------------ auth

    void login(@NonNull String employeeId,
               @NonNull String password,
               @NonNull ApiCallback<Session> cb);

    void logout(@NonNull ApiCallback<Boolean> cb);

    // ---------------------------------------------------------- registration

    /**
     * Step 1 of first-time registration: match an employee code to the NIC HR
     * holds, and send an OTP to the number on that record.
     *
     * <p>This <em>activates</em> an existing employee; it never creates one.
     * Everyone is enrolled in the gate system before their first day.
     */
    void startRegistration(@NonNull String employeeCode,
                           @NonNull String nic,
                           @NonNull ApiCallback<RegistrationChallenge> cb);

    /** Step 2: confirm the code that arrived by SMS. */
    void verifyRegistrationOtp(@NonNull String challengeToken,
                               @NonNull String otp,
                               @NonNull ApiCallback<String> cb);

    /**
     * Step 3: set the password and sign in.
     *
     * @param verifiedToken from {@link #verifyRegistrationOtp}
     */
    void completeRegistration(@NonNull String verifiedToken,
                              @NonNull String password,
                              @NonNull ApiCallback<Session> cb);

    // ------------------------------------------------------------ attendance

    void attendanceMonth(@NonNull YearMonth month, @NonNull ApiCallback<MonthSummary> cb);

    /** Full gate-event timeline for one day. */
    void attendanceDay(@NonNull LocalDate date, @NonNull ApiCallback<AttendanceDay> cb);

    /** Files a dispute with HR. Changes nothing — it only opens a review. */
    void requestCorrection(@NonNull LocalDate date,
                           @NonNull String message,
                           @NonNull ApiCallback<Boolean> cb);

    // ----------------------------------------------------------------- leave

    void leaveSnapshot(@NonNull ApiCallback<LeaveSnapshot> cb);

    void applyLeave(@NonNull LeaveType type,
                    @NonNull LocalDate from,
                    @NonNull LocalDate to,
                    @NonNull LeaveRequest.DayPortion portion,
                    float days,
                    @NonNull String reason,
                    @Nullable String coveringOfficer,
                    @Nullable String approver,
                    @NonNull ApiCallback<LeaveRequest> cb);

    // --------------------------------------------------------------- absence

    void absenceReasons(@NonNull ApiCallback<List<AbsenceReason>> cb);

    void submitAbsenceReason(@NonNull AbsenceReason reason,
                             @NonNull ApiCallback<AbsenceReason> cb);

    // -------------------------------------------------------------- overtime

    void overtime(@NonNull YearMonth month, @NonNull ApiCallback<Overtime> cb);

    // ------------------------------------------------------- feed & contacts

    void announcements(@NonNull ApiCallback<List<Announcement>> cb);

    void notifications(@NonNull ApiCallback<List<AppNotification>> cb);

    void markAllNotificationsRead(@NonNull ApiCallback<Boolean> cb);

    /** Covering-officer and approver pickers on the leave form. */
    void colleagues(@NonNull ApiCallback<List<Colleague>> cb);
}
