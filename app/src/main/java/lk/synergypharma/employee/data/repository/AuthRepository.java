package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import lk.synergypharma.employee.data.api.ApiCallback;
import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.data.local.PrefsManager;
import lk.synergypharma.employee.data.mapper.EmployeeMapper;
import lk.synergypharma.employee.data.remote.dto.EmployeeDto;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.domain.model.RegistrationChallenge;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.util.Result;

/**
 * Login, logout and "who is signed in".
 *
 * <p>The signed-in {@link Employee} is exposed as LiveData because the role on
 * it decides what the navigation shows. There is one login for everybody —
 * a manager is an employee too — and the extra supervisor destinations simply
 * appear for whoever the server says is a supervisor.
 */
public final class AuthRepository {

    @NonNull
    private final SynergyApi api;
    @NonNull
    private final PrefsManager prefs;

    private final MutableLiveData<Employee> employee = new MutableLiveData<>();

    public AuthRepository(@NonNull SynergyApi api, @NonNull PrefsManager prefs) {
        this.api = api;
        this.prefs = prefs;

        // Render the cached record immediately on a warm start.
        EmployeeDto cached = prefs.cachedEmployee();
        if (cached != null) {
            employee.setValue(EmployeeMapper.toDomain(cached));
        }
    }

    @NonNull
    public LiveData<Employee> employee() {
        return employee;
    }

    @Nullable
    public Employee currentEmployee() {
        return employee.getValue();
    }

    public boolean isLoggedIn() {
        return prefs.isLoggedIn();
    }

    /** Pre-fills the login form so nobody types their EPF number twice a day. */
    @Nullable
    public String lastEmployeeId() {
        return prefs.lastEmployeeId();
    }

    public boolean isBiometricEnabled() {
        return prefs.isBiometricEnabled();
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.setBiometricEnabled(enabled);
    }

    @NonNull
    public LiveData<Result<Session>> login(@NonNull String employeeId, @NonNull String password) {
        MutableLiveData<Result<Session>> stream = new MutableLiveData<>(Result.loading());
        api.login(employeeId, password, new ApiCallback<Session>() {
            @Override
            public void onSuccess(@NonNull Session session) {
                prefs.saveSession(session.accessToken, session.refreshToken,
                        EmployeeMapper.toDto(session.employee));
                employee.setValue(session.employee);
                stream.setValue(Result.success(session));
            }

            @Override
            public void onError(@NonNull String message) {
                stream.setValue(Result.error(message));
            }
        });
        return stream;
    }

    // ---------------------------------------------------------- registration

    /**
     * Step 1 — match the employee code to the NIC HR holds and send an OTP to
     * the number on that record.
     */
    @NonNull
    public LiveData<Result<RegistrationChallenge>> startRegistration(
            @NonNull String employeeCode, @NonNull String nic) {
        MutableLiveData<Result<RegistrationChallenge>> stream =
                new MutableLiveData<>(Result.loading());
        api.startRegistration(employeeCode, nic, new ApiCallback<RegistrationChallenge>() {
            @Override
            public void onSuccess(@NonNull RegistrationChallenge data) {
                stream.setValue(Result.success(data));
            }

            @Override
            public void onError(@NonNull String message) {
                stream.setValue(Result.error(message));
            }
        });
        return stream;
    }

    /** Step 2 — confirm the SMS code. Yields a token good for one password set. */
    @NonNull
    public LiveData<Result<String>> verifyRegistrationOtp(@NonNull String challengeToken,
                                                          @NonNull String otp) {
        MutableLiveData<Result<String>> stream = new MutableLiveData<>(Result.loading());
        api.verifyRegistrationOtp(challengeToken, otp, new ApiCallback<String>() {
            @Override
            public void onSuccess(@NonNull String token) {
                stream.setValue(Result.success(token));
            }

            @Override
            public void onError(@NonNull String message) {
                stream.setValue(Result.error(message));
            }
        });
        return stream;
    }

    /**
     * Step 3 — set the password. Ends signed in, and persists the session
     * exactly as a password login does, so the app opens straight to Home.
     */
    @NonNull
    public LiveData<Result<Session>> completeRegistration(@NonNull String verifiedToken,
                                                          @NonNull String password) {
        MutableLiveData<Result<Session>> stream = new MutableLiveData<>(Result.loading());
        api.completeRegistration(verifiedToken, password, new ApiCallback<Session>() {
            @Override
            public void onSuccess(@NonNull Session session) {
                prefs.saveSession(session.accessToken, session.refreshToken,
                        EmployeeMapper.toDto(session.employee));
                employee.setValue(session.employee);
                stream.setValue(Result.success(session));
            }

            @Override
            public void onError(@NonNull String message) {
                stream.setValue(Result.error(message));
            }
        });
        return stream;
    }

    @NonNull
    public LiveData<Result<Boolean>> logout() {
        MutableLiveData<Result<Boolean>> stream = new MutableLiveData<>(Result.loading());
        api.logout(new ApiCallback<Boolean>() {
            @Override
            public void onSuccess(@NonNull Boolean data) {
                finishLogout(stream);
            }

            @Override
            public void onError(@NonNull String message) {
                // A failed logout call must still clear the phone. Leaving a
                // token behind because the network blipped is the worse outcome.
                finishLogout(stream);
            }
        });
        return stream;
    }

    private void finishLogout(@NonNull MutableLiveData<Result<Boolean>> stream) {
        prefs.clearSession();
        employee.setValue(null);
        stream.setValue(Result.success(Boolean.TRUE));
    }
}
