package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.function.Consumer;

import lk.synergypharma.employee.data.api.ApiCallback;
import lk.synergypharma.employee.util.Result;

/**
 * Bridges the callback-shaped {@code SynergyApi} to the LiveData the ViewModels
 * observe, so no repository has to write the same anonymous callback twice.
 *
 * <p>The returned stream starts in {@link Result#loading()}, which is what lets
 * every screen show its spinner without a separate boolean.
 */
final class Calls {

    private Calls() {
    }

    @NonNull
    static <T> LiveData<Result<T>> live(@NonNull Consumer<ApiCallback<T>> starter) {
        MutableLiveData<Result<T>> stream = new MutableLiveData<>(Result.loading());
        starter.accept(new ApiCallback<T>() {
            @Override
            public void onSuccess(@NonNull T data) {
                // SynergyApi guarantees the main thread, so setValue is safe.
                stream.setValue(Result.success(data));
            }

            @Override
            public void onError(@NonNull String message) {
                stream.setValue(Result.error(message));
            }
        });
        return stream;
    }
}
