package lk.synergypharma.employee.ui.attendance;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.YearMonth;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/** Holds which month is on screen and the data for it. */
public final class AttendanceViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<MonthSummary> summary = new Relay<>();
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>(YearMonth.now());

    public AttendanceViewModel() {
        load();
    }

    @NonNull
    public LiveData<YearMonth> month() {
        return month;
    }

    @NonNull
    public LiveData<Result<MonthSummary>> summary() {
        return summary.live();
    }

    public void previousMonth() {
        month.setValue(currentMonth().minusMonths(1));
        load();
    }

    /**
     * Paging past the current month is blocked: there is nothing to show, and an
     * empty calendar reads as "my attendance is missing" rather than "this has
     * not happened yet".
     */
    public boolean canGoForward() {
        return currentMonth().isBefore(YearMonth.now());
    }

    public void nextMonth() {
        if (!canGoForward()) {
            return;
        }
        month.setValue(currentMonth().plusMonths(1));
        load();
    }

    public void refresh() {
        load();
    }

    private void load() {
        summary.from(services.attendance().month(currentMonth()));
    }

    @NonNull
    private YearMonth currentMonth() {
        YearMonth value = month.getValue();
        return value == null ? YearMonth.now() : value;
    }
}
