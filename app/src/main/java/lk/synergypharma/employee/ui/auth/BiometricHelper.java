package lk.synergypharma.employee.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Fingerprint / face unlock for re-entering an existing session.
 *
 * <p>Note what this does <b>not</b> do: it never creates a session. The employee
 * signs in with their EPF number and password at least once, the token is stored
 * encrypted, and biometrics only decide whether the app will reopen it. That
 * keeps the HR server the only thing that can authenticate anybody.
 *
 * <p>{@code BIOMETRIC_WEAK} is deliberate — face unlock on most mid-range phones
 * in the plant is classified weak, and requiring STRONG would silently switch
 * the feature off for the people most likely to use it. The token is not
 * protected by a biometric-bound key, so weak is an acceptable trade here.
 */
public final class BiometricHelper {

    private static final int ALLOWED = BiometricManager.Authenticators.BIOMETRIC_WEAK;

    public interface Listener {
        void onUnlocked();

        /** @param message null when the employee simply cancelled */
        void onFailed(String message);
    }

    private BiometricHelper() {
    }

    /** True when something is actually enrolled and usable right now. */
    public static boolean canAuthenticate(@NonNull Context context) {
        return BiometricManager.from(context).canAuthenticate(ALLOWED)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void prompt(@NonNull Fragment fragment,
                              @NonNull String title,
                              @NonNull String subtitle,
                              @NonNull String cancelLabel,
                              @NonNull Listener listener) {
        Context context = fragment.requireContext();

        BiometricPrompt prompt = new BiometricPrompt(
                fragment,
                ContextCompat.getMainExecutor(context),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        listener.onUnlocked();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        boolean cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                || errorCode == BiometricPrompt.ERROR_CANCELED;
                        listener.onFailed(cancelled ? null : errString.toString());
                    }
                });

        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancelLabel)
                .setAllowedAuthenticators(ALLOWED)
                .setConfirmationRequired(false)
                .build());
    }
}
