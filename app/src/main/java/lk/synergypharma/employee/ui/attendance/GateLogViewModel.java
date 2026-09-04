package lk.synergypharma.employee.ui.attendance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/** One day's gate events, plus the dispute the employee can raise about them. */
public final class GateLogViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<AttendanceDay> day = new Relay<>();
    private final Relay<Boolean> correction = new Relay<>();

    @Nullable
    private LocalDate loaded;

    @NonNull
    public LiveData<Result<AttendanceDay>> day() {
        return day.live();
    }

    @NonNull
    public LiveData<Result<Boolean>> correction() {
        return correction.live();
    }

    /** Idempotent, so a rotation does not refetch. */
    public void load(@NonNull LocalDate date) {
        if (date.equals(loaded)) {
            return;
        }
        loaded = date;
        day.from(services.attendance().day(date));
    }

    public void reload() {
        if (loaded != null) {
            day.from(services.attendance().day(loaded));
        }
    }

    public void submitCorrection(@NonNull LocalDate date, @NonNull String message) {
        correction.from(services.attendance().requestCorrection(date, message));
    }

    public void consumeCorrection() {
        correction.reset();
    }
}
