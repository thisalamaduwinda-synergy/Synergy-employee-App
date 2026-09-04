package lk.synergypharma.employee.data.api;

import androidx.annotation.NonNull;

/**
 * Result of one data call, always delivered on the main thread.
 *
 * @param <T> payload type
 */
public interface ApiCallback<T> {

    void onSuccess(@NonNull T data);

    /** @param message already human-readable and safe to show the employee */
    void onError(@NonNull String message);
}
