package lk.synergypharma.employee.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.data.repository.AuthRepository;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/**
 * Holds the login attempt across a rotation, so turning the phone mid-request
 * does not fire a second one.
 */
public final class LoginViewModel extends ViewModel {

    private final AuthRepository auth = ServiceLocator.get().auth();
    private final Relay<Session> relay = new Relay<>();

    @NonNull
    public LiveData<Result<Session>> state() {
        return relay.live();
    }

    public void login(@NonNull String employeeId, @NonNull String password) {
        relay.from(auth.login(employeeId, password));
    }

    /** Clears the one-shot result once the screen has navigated away. */
    public void consume() {
        relay.reset();
    }

    @Nullable
    public String lastEmployeeId() {
        return auth.lastEmployeeId();
    }

    /** A session exists on the phone but is locked behind biometrics. */
    public boolean isLockedSession() {
        return auth.isLoggedIn() && auth.isBiometricEnabled();
    }

    public boolean isBiometricEnabled() {
        return auth.isBiometricEnabled();
    }

    public void setBiometricEnabled(boolean enabled) {
        auth.setBiometricEnabled(enabled);
    }
}
