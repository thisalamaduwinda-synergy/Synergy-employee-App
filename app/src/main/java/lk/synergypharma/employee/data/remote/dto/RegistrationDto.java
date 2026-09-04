package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * First-time registration, in three calls.
 *
 * <p><b>None of this exists on the recognition backend yet.</b> Its
 * {@code employees} table has no password column at all, and its {@code users}
 * table is for {@code admin} / {@code hr} / {@code security} operators. This is
 * the proposal.
 *
 * <p>The important property is that registration <b>activates</b> a row that HR
 * already created; it must never insert one. Everybody is enrolled in the gate
 * system before their first day, so an employee code with no matching record is
 * not a new joiner — it is somebody guessing.
 *
 * <h3>What the server has to enforce</h3>
 * <ol>
 *   <li>Match on {@code employee_code} <em>and</em> {@code nic}, and only for a
 *       record whose status is {@code active}. A resigned employee must not be
 *       able to activate an account.</li>
 *   <li>Refuse if a password already exists — direct them to sign in or reset,
 *       rather than letting anyone overwrite a live account.</li>
 *   <li>Rate-limit by employee code <em>and</em> by IP, and return the same
 *       generic failure whether the code was unknown or the NIC was wrong. Two
 *       different messages turn this into an employee-number enumerator.</li>
 *   <li>Send the OTP to the {@code phone} column only — never to a number the
 *       client supplies.</li>
 *   <li>Expire the challenge token in minutes, allow a small fixed number of
 *       OTP attempts, and invalidate it on the first success.</li>
 * </ol>
 */
public final class RegistrationDto {

    /** Step 1 — prove the record is yours. */
    public static final class IdentityRequest {
        @SerializedName("employee_code")
        public String employeeCode;

        /** Old ({@code 881234567V}) or new ({@code 198812345678}) form. */
        @SerializedName("nic")
        public String nic;

        public IdentityRequest(String employeeCode, String nic) {
            this.employeeCode = employeeCode;
            this.nic = nic;
        }
    }

    /** Step 1 response — an OTP is now on its way to the number HR holds. */
    public static final class ChallengeResponse {
        @SerializedName("challenge_token")
        public String challengeToken;

        /** Masked: recognisable to its owner, useless to anyone else. */
        @SerializedName("masked_phone")
        public String maskedPhone;

        @SerializedName("expires_in")
        public int expiresIn;

        @SerializedName("otp_length")
        public int otpLength;

        /**
         * Mock builds only. A real server must never return the OTP it just
         * sent — that would make the SMS round trip decorative.
         */
        @SerializedName("demo_otp")
        @Nullable
        public String demoOtp;
    }

    /** Step 2 — confirm the code that arrived by SMS. */
    public static final class OtpRequest {
        @SerializedName("challenge_token")
        public String challengeToken;

        @SerializedName("otp")
        public String otp;

        public OtpRequest(String challengeToken, String otp) {
            this.challengeToken = challengeToken;
            this.otp = otp;
        }
    }

    /** Step 2 response — a token that authorises exactly one password set. */
    public static final class VerifiedResponse {
        @SerializedName("verified_token")
        public String verifiedToken;
    }

    /** Step 3 — choose a password. The server hashes it; it is never stored raw. */
    public static final class PasswordRequest {
        @SerializedName("verified_token")
        public String verifiedToken;

        @SerializedName("password")
        public String password;

        public PasswordRequest(String verifiedToken, String password) {
            this.verifiedToken = verifiedToken;
            this.password = password;
        }
    }
}
