package lk.synergypharma.employee.data.remote;

import java.util.List;

import lk.synergypharma.employee.data.remote.dto.AbsenceReasonDto;
import lk.synergypharma.employee.data.remote.dto.AnnouncementDto;
import lk.synergypharma.employee.data.remote.dto.ApiResponse;
import lk.synergypharma.employee.data.remote.dto.AttendanceDayDto;
import lk.synergypharma.employee.data.remote.dto.ColleagueDto;
import lk.synergypharma.employee.data.remote.dto.EmployeeDto;
import lk.synergypharma.employee.data.remote.dto.HolidayDto;
import lk.synergypharma.employee.data.remote.dto.LeaveBalanceDto;
import lk.synergypharma.employee.data.remote.dto.LeaveRequestDto;
import lk.synergypharma.employee.data.remote.dto.LoginDto;
import lk.synergypharma.employee.data.remote.dto.MonthSummaryDto;
import lk.synergypharma.employee.data.remote.dto.NotificationDto;
import lk.synergypharma.employee.data.remote.dto.RegistrationDto;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The HTTP contract between this app and the Synergy backend.
 *
 * <p><b>Nothing calls this yet.</b> Every screen currently runs off
 * {@code MockSynergyApi}; this interface exists so the shape is agreed and the
 * DTOs are compiled and reviewed before anyone writes the server.
 *
 * <h3>How this sits against the existing recognition system</h3>
 *
 * The face-recognition backend (FastAPI, {@code /api/v1}) already owns the data:
 * {@code employees}, {@code attendance}, {@code attendance_scans},
 * {@code leave_requests}, {@code leave_balances}, {@code holidays},
 * {@code shifts}. Every DTO in this package is named after those columns so the
 * new endpoints are a <em>filter</em> over existing tables, not a reshaping.
 *
 * <p>What it does <b>not</b> have, and what therefore has to be built:
 * <ol>
 *   <li><b>Employee authentication.</b> Its {@code users} table holds
 *       {@code admin} / {@code hr} / {@code security} operator accounts only.
 *       Employees have no credentials at all today.</li>
 *   <li><b>Employee-scoped endpoints.</b> Everything under {@code /api/v1} is
 *       HR-facing and returns the whole workforce. The {@code /me/…} routes
 *       below must resolve the employee from the token and never accept an
 *       employee id from the client — otherwise anyone could read a colleague's
 *       attendance by changing a number.</li>
 *   <li><b>Absence reasons.</b> No table exists. This is the app's headline
 *       feature, so it needs one: date, type, explanation, attachments, status.</li>
 *   <li><b>Announcements and notifications.</b> Neither exists yet.</li>
 *   <li><b>Two leave columns:</b> {@code covering_officer} and
 *       {@code half_day_period} — see {@link LeaveRequestDto}.</li>
 * </ol>
 *
 * <h3>Two rules the server side must hold to</h3>
 * <ol>
 *   <li><b>Never expose the HR database directly to the app.</b> Credentials
 *       inside an APK can be extracted in minutes.</li>
 *   <li><b>Attendance is read-only.</b> There is deliberately no endpoint here
 *       that creates a gate scan. The only writes are a <em>dispute</em>, an
 *       absence reason and a leave request.</li>
 * </ol>
 */
public interface ApiService {

    // ------------------------------------------------------------------ auth

    /** New. Authenticates on {@code employee_code} + a password HR issues. */
    @POST("me/auth/login")
    Call<ApiResponse<LoginDto.Response>> login(@Body LoginDto.Request body);

    @POST("me/auth/refresh")
    Call<ApiResponse<LoginDto.Response>> refresh(@Body RequestBody refreshToken);

    @POST("me/auth/logout")
    Call<ApiResponse<Void>> logout();

    @GET("me/profile")
    Call<ApiResponse<EmployeeDto>> profile();

    // ---------------------------------------------------------- registration

    /**
     * New. Activates an existing employee record — see {@link RegistrationDto}
     * for the rules the server has to enforce, including rate limiting and
     * returning one generic failure for both "unknown code" and "wrong NIC".
     */
    @POST("me/auth/register/identity")
    Call<ApiResponse<RegistrationDto.ChallengeResponse>> startRegistration(
            @Body RegistrationDto.IdentityRequest body);

    @POST("me/auth/register/verify")
    Call<ApiResponse<RegistrationDto.VerifiedResponse>> verifyRegistrationOtp(
            @Body RegistrationDto.OtpRequest body);

    /** Sets the password and returns a session, so registration ends signed in. */
    @POST("me/auth/register/complete")
    Call<ApiResponse<LoginDto.Response>> completeRegistration(
            @Body RegistrationDto.PasswordRequest body);

    // ------------------------------------------------------------ attendance

    /**
     * Employee's own month. Filters {@code attendance} by the token's employee.
     *
     * @param month {@code yyyy-MM}
     */
    @GET("me/attendance")
    Call<ApiResponse<MonthSummaryDto>> attendanceMonth(@Query("month") String month);

    /**
     * One day including its {@code attendance_scans}. Join the camera name in —
     * a bare {@code camera_id} means nothing to the person reading it.
     *
     * @param date {@code yyyy-MM-dd}
     */
    @GET("me/attendance/{date}")
    Call<ApiResponse<AttendanceDayDto>> attendanceDay(@Path("date") String date);

    /**
     * The employee disputes a day's record. This is the <em>only</em> way the
     * app touches attendance, and it creates a review task for HR rather than
     * changing anything.
     */
    @POST("me/attendance/{date}/dispute")
    Call<ApiResponse<Void>> requestCorrection(@Path("date") String date,
                                              @Body DisputeBody body);

    final class DisputeBody {
        public final String message;

        public DisputeBody(String message) {
            this.message = message;
        }
    }

    // ----------------------------------------------------------------- leave

    /** One row per leave type, straight off {@code leave_balances}. */
    @GET("me/leave/balance")
    Call<ApiResponse<List<LeaveBalanceDto>>> leaveBalance();

    /** Poya, mercantile and company days for the year, from {@code holidays}. */
    @GET("holidays")
    Call<ApiResponse<List<HolidayDto>>> holidays(@Query("year") int year);

    @GET("me/leave")
    Call<ApiResponse<List<LeaveRequestDto>>> leaveRequests();

    @POST("me/leave")
    Call<ApiResponse<LeaveRequestDto>> applyLeave(@Body LeaveRequestDto body);

    @DELETE("me/leave/{id}")
    Call<ApiResponse<Void>> cancelLeave(@Path("id") String id);

    // --------------------------------------------------------------- absence

    /**
     * Multipart because the certificate photo travels with the reason. Compress
     * the image on the phone first — see {@code FileUtils#compressImage}.
     */
    @Multipart
    @POST("me/absence-reasons")
    Call<ApiResponse<AbsenceReasonDto>> submitAbsenceReason(
            @Part("reason") RequestBody reasonJson,
            @Part List<MultipartBody.Part> files);

    @GET("me/absence-reasons")
    Call<ApiResponse<List<AbsenceReasonDto>>> absenceReasons();

    // ------------------------------------------------------------- directory

    /** Covering-officer and approver pickers; the full directory in v1.1. */
    @GET("me/colleagues")
    Call<ApiResponse<List<ColleagueDto>>> colleagues(@Query("q") String query);

    // ------------------------------------------------- announcements & push

    @GET("me/announcements")
    Call<ApiResponse<List<AnnouncementDto>>> announcements();

    @POST("me/announcements/{id}/read")
    Call<ApiResponse<Void>> markAnnouncementRead(@Path("id") String id);

    @GET("me/notifications")
    Call<ApiResponse<List<NotificationDto>>> notifications();

    @POST("me/notifications/read-all")
    Call<ApiResponse<Void>> markAllNotificationsRead();

    @POST("me/devices/fcm-token")
    Call<ApiResponse<Void>> registerFcmToken(@Body RequestBody token);
}
