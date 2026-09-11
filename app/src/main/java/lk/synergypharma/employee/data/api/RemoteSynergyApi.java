package lk.synergypharma.employee.data.api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.synergypharma.employee.data.mapper.AbsenceMapper;
import lk.synergypharma.employee.data.mapper.AttendanceMapper;
import lk.synergypharma.employee.data.mapper.EmployeeMapper;
import lk.synergypharma.employee.data.remote.ApiService;
import lk.synergypharma.employee.data.remote.dto.ApiResponse;
import lk.synergypharma.employee.data.remote.dto.LoginDto;
import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.domain.model.RegistrationChallenge;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * {@link SynergyApi} backed by the recognition system's {@code /api/v1/me/…}
 * endpoints (FastAPI, {@code backend/app/api/v1/me.py}).
 *
 * <p><b>Phase 9, in progress.</b> Login, profile, attendance and absence
 * reasons are real. Every other call still goes to {@link MockSynergyApi}
 * until its backend endpoint exists, and is marked {@code // MOCK} below so
 * nothing is forgotten. Leave, overtime, announcements and the directory are
 * therefore demo data on a build that shows real gate scans - keep that in
 * mind when showing it to someone.
 */
public final class RemoteSynergyApi implements SynergyApi {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    @NonNull
    private final ApiService service;
    @NonNull
    private final SynergyApi mock;
    @NonNull
    private final Context context;
    @NonNull
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Reads attachment bytes off the main thread before an upload. */
    @NonNull
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @NonNull
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public RemoteSynergyApi(@NonNull ApiService service, @NonNull Context context) {
        this.service = service;
        this.context = context.getApplicationContext();
        this.mock = new MockSynergyApi(context);
    }

    // ------------------------------------------------------------------ auth

    @Override
    public void login(@NonNull String employeeId,
                      @NonNull String password,
                      @NonNull ApiCallback<Session> cb) {
        LoginDto.Request body = new LoginDto.Request(employeeId.trim(), password, null);
        enqueue(service.login(body), cb, EmployeeMapper::toSession);
    }

    @Override
    public void logout(@NonNull ApiCallback<Boolean> cb) {
        // Best effort: the local session is cleared by AuthRepository whatever
        // the server says, so a dead network must not block signing out.
        service.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                main.post(() -> cb.onSuccess(Boolean.TRUE));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                main.post(() -> cb.onSuccess(Boolean.TRUE));
            }
        });
    }

    // ---------------------------------------------------------- registration
    // MOCK - self-registration (NIC + OTP) needs an SMS gateway on the server.

    @Override
    public void startRegistration(@NonNull String employeeCode,
                                  @NonNull String nic,
                                  @NonNull ApiCallback<RegistrationChallenge> cb) {
        mock.startRegistration(employeeCode, nic, cb);
    }

    @Override
    public void verifyRegistrationOtp(@NonNull String challengeToken,
                                      @NonNull String otp,
                                      @NonNull ApiCallback<String> cb) {
        mock.verifyRegistrationOtp(challengeToken, otp, cb);
    }

    @Override
    public void completeRegistration(@NonNull String verifiedToken,
                                     @NonNull String password,
                                     @NonNull ApiCallback<Session> cb) {
        mock.completeRegistration(verifiedToken, password, cb);
    }

    // ------------------------------------------------------------ attendance

    @Override
    public void attendanceMonth(@NonNull YearMonth month, @NonNull ApiCallback<MonthSummary> cb) {
        enqueue(service.attendanceMonth(month.format(MONTH)), cb, AttendanceMapper::toDomain);
    }

    @Override
    public void attendanceDay(@NonNull LocalDate date, @NonNull ApiCallback<AttendanceDay> cb) {
        enqueue(service.attendanceDay(date.toString()), cb, AttendanceMapper::toDomain);
    }

    @Override
    public void requestCorrection(@NonNull LocalDate date,
                                  @NonNull String message,
                                  @NonNull ApiCallback<Boolean> cb) {
        mock.requestCorrection(date, message, cb); // MOCK - needs a disputes table
    }

    // ----------------------------------------------------------------- leave
    // MOCK - `/me/leave/*` not built yet (leave_requests/leave_balances exist).

    @Override
    public void leaveSnapshot(@NonNull ApiCallback<LeaveSnapshot> cb) {
        mock.leaveSnapshot(cb);
    }

    @Override
    public void applyLeave(@NonNull LeaveType type, @NonNull LocalDate from, @NonNull LocalDate to,
                           @NonNull LeaveRequest.DayPortion portion, float days,
                           @NonNull String reason, @Nullable String coveringOfficer,
                           @Nullable String approver, @NonNull ApiCallback<LeaveRequest> cb) {
        mock.applyLeave(type, from, to, portion, days, reason, coveringOfficer, approver, cb);
    }

    // --------------------------------------------------------------- absence

    @Override
    public void absenceReasons(@NonNull ApiCallback<List<AbsenceReason>> cb) {
        enqueue(service.absenceReasons(), cb, AbsenceMapper::toReasons);
    }

    /**
     * Multipart: a {@code reason} JSON part plus one {@code files} part per
     * attachment. The picker already compressed photos into the cache
     * ({@code file://}); PDFs arrive as {@code content://} and are streamed
     * through the ContentResolver. Bytes are read on {@link #io}, never on the
     * main thread.
     */
    @Override
    public void submitAbsenceReason(@NonNull AbsenceReason reason,
                                    @NonNull ApiCallback<AbsenceReason> cb) {
        io.execute(() -> {
            final List<MultipartBody.Part> parts = new ArrayList<>();
            try {
                for (Attachment a : reason.attachments) {
                    if (a.localUri == null) {
                        continue; // already on the server (a re-submit) - nothing to upload
                    }
                    byte[] bytes = readAll(Uri.parse(a.localUri));
                    RequestBody body = RequestBody.create(bytes, MediaType.parse(a.mimeType));
                    parts.add(MultipartBody.Part.createFormData("files", a.fileName, body));
                }
            } catch (IOException | RuntimeException e) {
                main.post(() -> cb.onError("Could not read the attached file. Please add it again."));
                return;
            }
            RequestBody reasonJson = RequestBody.create(
                    gson.toJson(AbsenceMapper.toDto(reason)), JSON);
            enqueue(service.submitAbsenceReason(reasonJson, parts), cb, AbsenceMapper::toDomain);
        });
    }

    @NonNull
    private byte[] readAll(@NonNull Uri uri) throws IOException {
        InputStream in;
        if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
            in = new java.io.FileInputStream(new File(uri.getPath()));
        } else {
            in = context.getContentResolver().openInputStream(uri);
        }
        if (in == null) {
            throw new IOException("Cannot open " + uri);
        }
        try (InputStream src = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = src.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    // -------------------------------------------------------------- overtime

    @Override
    public void overtime(@NonNull YearMonth month, @NonNull ApiCallback<Overtime> cb) {
        mock.overtime(month, cb); // MOCK - derive from /me/attendance next
    }

    // ------------------------------------------------------- feed & contacts
    // MOCK - announcements / notifications / directory endpoints not built.

    @Override
    public void announcements(@NonNull ApiCallback<List<Announcement>> cb) {
        mock.announcements(cb);
    }

    @Override
    public void notifications(@NonNull ApiCallback<List<AppNotification>> cb) {
        mock.notifications(cb);
    }

    @Override
    public void markAllNotificationsRead(@NonNull ApiCallback<Boolean> cb) {
        mock.markAllNotificationsRead(cb);
    }

    @Override
    public void colleagues(@NonNull ApiCallback<List<Colleague>> cb) {
        mock.colleagues(cb);
    }

    // -------------------------------------------------------------- plumbing

    /** DTO → domain, run on the background thread before the callback. */
    private interface Mapper<D, T> {
        @NonNull
        T map(@NonNull D dto);
    }

    /**
     * Unwraps {@link ApiResponse}, maps the payload and delivers on the main
     * thread - the contract every repository relies on.
     */
    private <D, T> void enqueue(@NonNull Call<ApiResponse<D>> call,
                                @NonNull ApiCallback<T> cb,
                                @NonNull Mapper<D, T> mapper) {
        call.enqueue(new Callback<ApiResponse<D>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<D>> c,
                                   @NonNull Response<ApiResponse<D>> response) {
                ApiResponse<D> env = response.body();
                if (response.isSuccessful() && env != null && env.success && env.data != null) {
                    final T value;
                    try {
                        value = mapper.map(env.data);
                    } catch (RuntimeException e) {
                        main.post(() -> cb.onError("Unexpected data from the server."));
                        return;
                    }
                    main.post(() -> cb.onSuccess(value));
                    return;
                }
                final String message = errorMessage(response, env);
                main.post(() -> cb.onError(message));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<D>> c, @NonNull Throwable t) {
                final String message = t instanceof IOException
                        ? "Cannot reach the server. Check your connection and try again."
                        : "Something went wrong. Please try again.";
                main.post(() -> cb.onError(message));
            }
        });
    }

    /**
     * FastAPI's {@code HTTPException} arrives as {@code {"detail": "…"}}, not
     * in the envelope, so both shapes are read before falling back to a generic
     * line the employee can act on.
     */
    @NonNull
    private String errorMessage(@NonNull Response<?> response, @Nullable ApiResponse<?> env) {
        if (env != null && env.message != null && !env.message.isEmpty()) {
            return env.message;
        }
        ResponseBody raw = response.errorBody();
        if (raw != null) {
            try {
                JsonObject obj = JsonParser.parseString(raw.string()).getAsJsonObject();
                if (obj.has("detail") && obj.get("detail").isJsonPrimitive()) {
                    return obj.get("detail").getAsString();
                }
                if (obj.has("message") && obj.get("message").isJsonPrimitive()) {
                    return obj.get("message").getAsString();
                }
            } catch (IOException | RuntimeException ignored) {
                // fall through to the status-based message
            }
        }
        switch (response.code()) {
            case 401:
                return "Employee number or password is incorrect";
            case 403:
                return "This account has been deactivated. Please contact HR.";
            case 404:
                return "No record found.";
            case 423:
                return "Too many failed attempts. Please try again later.";
            default:
                return "Server error (" + response.code() + "). Please try again.";
        }
    }
}
