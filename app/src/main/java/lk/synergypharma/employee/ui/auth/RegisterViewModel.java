package lk.synergypharma.employee.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.data.repository.AuthRepository;
import lk.synergypharma.employee.domain.model.RegistrationChallenge;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/**
 * Holds the three-step registration across rotations, and — more importantly —
 * across the trip to the SMS app to read the code, which on a low-memory phone
 * can take the fragment with it.
 */
public final class RegisterViewModel extends ViewModel {

    public static final int STEP_IDENTITY = 1;
    public static final int STEP_OTP = 2;
    public static final int STEP_PASSWORD = 3;

    private final AuthRepository auth = ServiceLocator.get().auth();

    private final MutableLiveData<Integer> step = new MutableLiveData<>(STEP_IDENTITY);
    private final Relay<RegistrationChallenge> challengeRelay = new Relay<>();
    private final Relay<String> verifyRelay = new Relay<>();
    private final Relay<Session> completeRelay = new Relay<>();

    @Nullable
    private String employeeCode;
    @Nullable
    private String nic;
    @Nullable
    private RegistrationChallenge challenge;
    @Nullable
    private String verifiedToken;

    // --------------------------------------------------------------- streams

    @NonNull
    public LiveData<Integer> step() {
        return step;
    }

    @NonNull
    public LiveData<Result<RegistrationChallenge>> challenge() {
        return challengeRelay.live();
    }

    @NonNull
    public LiveData<Result<String>> verification() {
        return verifyRelay.live();
    }

    @NonNull
    public LiveData<Result<Session>> completion() {
        return completeRelay.live();
    }

    // ------------------------------------------------------------- accessors

    @Nullable
    public String employeeCode() {
        return employeeCode;
    }

    @Nullable
    public String nic() {
        return nic;
    }

    @Nullable
    public RegistrationChallenge currentChallenge() {
        return challenge;
    }

    public int currentStep() {
        Integer value = step.getValue();
        return value == null ? STEP_IDENTITY : value;
    }

    /**
     * @return true if there was a step to go back to; false means the screen
     *         itself should close
     */
    public boolean goBackAStep() {
        int current = currentStep();
        if (current <= STEP_IDENTITY) {
            return false;
        }
        step.setValue(current - 1);
        return true;
    }

    // ----------------------------------------------------------------- steps

    public void startRegistration(@NonNull String employeeCode, @NonNull String nic) {
        this.employeeCode = employeeCode;
        this.nic = nic;
        challengeRelay.from(auth.startRegistration(employeeCode, nic));
    }

    /** Called once the challenge has been consumed by the screen. */
    public void onChallengeReceived(@NonNull RegistrationChallenge received) {
        this.challenge = received;
        challengeRelay.reset();
        step.setValue(STEP_OTP);
    }

    public void consumeChallengeError() {
        challengeRelay.reset();
    }

    /** Re-runs step 1 with the details already held, issuing a fresh code. */
    public void resendOtp() {
        if (employeeCode == null || nic == null) {
            return;
        }
        challengeRelay.from(auth.startRegistration(employeeCode, nic));
    }

    public void verifyOtp(@NonNull String otp) {
        if (challenge == null) {
            return;
        }
        verifyRelay.from(auth.verifyRegistrationOtp(challenge.challengeToken, otp));
    }

    public void onVerified(@NonNull String token) {
        this.verifiedToken = token;
        verifyRelay.reset();
        step.setValue(STEP_PASSWORD);
    }

    public void consumeVerifyError() {
        verifyRelay.reset();
    }

    public void completeRegistration(@NonNull String password) {
        if (verifiedToken == null) {
            return;
        }
        completeRelay.from(auth.completeRegistration(verifiedToken, password));
    }

    public void consumeCompletion() {
        completeRelay.reset();
    }
}
