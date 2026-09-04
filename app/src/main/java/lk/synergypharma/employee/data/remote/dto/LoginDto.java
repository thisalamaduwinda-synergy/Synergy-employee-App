package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** Login request and response, kept together because they are read together. */
public final class LoginDto {

    public static final class Request {
        /** The EPF / employee number, e.g. {@code 1213}. */
        @SerializedName("employee_code")
        public String employeeCode;

        @SerializedName("password")
        public String password;

        /** Registered here so push can reach the phone straight after login. */
        @SerializedName("fcm_token")
        @Nullable
        public String fcmToken;

        public Request(String employeeCode, String password, @Nullable String fcmToken) {
            this.employeeCode = employeeCode;
            this.password = password;
            this.fcmToken = fcmToken;
        }
    }

    public static final class Response {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("refresh_token")
        public String refreshToken;

        /** Seconds. Drives the silent refresh in the token authenticator. */
        @SerializedName("expires_in")
        public long expiresIn;

        @SerializedName("employee")
        public EmployeeDto employee;
    }
}
