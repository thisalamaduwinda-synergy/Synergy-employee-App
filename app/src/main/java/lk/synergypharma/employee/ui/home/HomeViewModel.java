package lk.synergypharma.employee.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.time.YearMonth;
import java.util.List;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/**
 * Home pulls the current month rather than just today, because the alert that
 * matters — "you have a working day with no gate record" — can only be found by
 * looking at the whole month.
 */
public final class HomeViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();

    private final Relay<MonthSummary> month = new Relay<>();
    private final Relay<List<Announcement>> announcements = new Relay<>();
    private final Relay<List<AppNotification>> notifications = new Relay<>();

    public HomeViewModel() {
        refresh();
    }

    @NonNull
    public LiveData<Employee> employee() {
        return services.auth().employee();
    }

    @NonNull
    public LiveData<Result<MonthSummary>> month() {
        return month.live();
    }

    @NonNull
    public LiveData<Result<List<Announcement>>> announcements() {
        return announcements.live();
    }

    @NonNull
    public LiveData<Result<List<AppNotification>>> notifications() {
        return notifications.live();
    }

    /** Also called after submitting an absence reason, to clear the alert. */
    public void refresh() {
        month.from(services.attendance().month(YearMonth.now()));
        announcements.from(services.feed().announcements());
        notifications.from(services.feed().notifications());
    }
}
