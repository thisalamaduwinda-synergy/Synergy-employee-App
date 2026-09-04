package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * What the server hands back once it has matched an employee code to an NIC.
 *
 * <p>It carries a one-time token and a <b>masked</b> phone number — enough for
 * the employee to recognise their own number, not enough for anyone else to
 * learn it. The token is what the next two calls are authorised by; the app
 * never sees the OTP itself, which is the entire point of sending one.
 */
public final class RegistrationChallenge {

    /** Short-lived, single-purpose. Not a session token. */
    @NonNull
    public final String challengeToken;
    /** e.g. {@code +94 7• ••• 5678} — recognisable, not readable. */
    @NonNull
    public final String maskedPhone;
    public final int expiresInSeconds;
    public final int otpLength;
    /**
     * Mock builds only: the code that was "sent", so the flow can be walked
     * without an SMS gateway. Always null once a real backend is wired.
     */
    @Nullable
    public final String demoOtp;

    public RegistrationChallenge(@NonNull String challengeToken,
                                 @NonNull String maskedPhone,
                                 int expiresInSeconds,
                                 int otpLength,
                                 @Nullable String demoOtp) {
        this.challengeToken = challengeToken;
        this.maskedPhone = maskedPhone;
        this.expiresInSeconds = expiresInSeconds;
        this.otpLength = otpLength;
        this.demoOtp = demoOtp;
    }
}
