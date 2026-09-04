package lk.synergypharma.employee.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Envelope every endpoint returns.
 *
 * <p>Wrapping the payload means an error can carry a message the app is allowed
 * to show the employee verbatim ("Your leave balance was updated by HR, please
 * reload") instead of the app inventing one from an HTTP status code.
 *
 * @param <T> payload type
 */
public final class ApiResponse<T> {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    @Nullable
    public T data;

    /** Human-readable, already translated by the server where possible. */
    @SerializedName("message")
    @Nullable
    public String message;

    /** Stable machine code, e.g. {@code LEAVE_BALANCE_EXCEEDED}. */
    @SerializedName("errorCode")
    @Nullable
    public String errorCode;
}
