package lk.synergypharma.employee.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Loading / Success / Error in one object, so every screen renders the same
 * three states without each fragment inventing its own boolean flags.
 *
 * @param <T> payload type
 */
public final class Result<T> {

    public enum Status {LOADING, SUCCESS, ERROR}

    @NonNull
    public final Status status;
    @Nullable
    public final T data;
    @Nullable
    public final String message;

    private Result(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    @NonNull
    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }

    /**
     * Loading, but with the previous payload still attached. Lets a pull-to-refresh
     * keep the old data on screen instead of flashing an empty state.
     */
    @NonNull
    public static <T> Result<T> loading(@Nullable T stale) {
        return new Result<>(Status.LOADING, stale, null);
    }

    @NonNull
    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message) {
        return new Result<>(Status.ERROR, null, message);
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
