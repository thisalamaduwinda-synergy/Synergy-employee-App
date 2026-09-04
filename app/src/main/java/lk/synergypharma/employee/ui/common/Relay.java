package lk.synergypharma.employee.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import lk.synergypharma.employee.util.Result;

/**
 * Forwards whichever repository call is currently in flight to one stable
 * LiveData the screen observes.
 *
 * <p>Repositories return a fresh stream per call, so without this a fragment
 * would have to re-subscribe every time — and a rotation mid-request would leave
 * the old stream attached. {@link #reset()} exists for the same reason: after a
 * screen acts on a success (navigating away, closing a form) the value must be
 * cleared, or rotating the phone replays the navigation.
 *
 * @param <T> payload type
 */
public final class Relay<T> {

    private final MediatorLiveData<Result<T>> live = new MediatorLiveData<>();
    @Nullable
    private LiveData<Result<T>> source;

    @NonNull
    public LiveData<Result<T>> live() {
        return live;
    }

    /** Detaches the previous call, if any, and follows {@code next} instead. */
    public void from(@NonNull LiveData<Result<T>> next) {
        detach();
        source = next;
        live.addSource(next, live::setValue);
    }

    /** Call once a one-shot result has been acted on. */
    public void reset() {
        detach();
        live.setValue(null);
    }

    private void detach() {
        if (source != null) {
            live.removeSource(source);
            source = null;
        }
    }
}
