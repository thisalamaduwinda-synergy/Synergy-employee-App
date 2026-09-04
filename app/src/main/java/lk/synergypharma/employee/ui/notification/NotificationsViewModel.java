package lk.synergypharma.employee.ui.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/**
 * The pull side of the same payload Firebase will push in phase 9, so a push and
 * an open of this screen show identical rows.
 */
public final class NotificationsViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<List<AppNotification>> notifications = new Relay<>();
    private final Relay<Boolean> markAll = new Relay<>();

    public NotificationsViewModel() {
        refresh();
    }

    @NonNull
    public LiveData<Result<List<AppNotification>>> notifications() {
        return notifications.live();
    }

    @NonNull
    public LiveData<Result<Boolean>> markAll() {
        return markAll.live();
    }

    public void refresh() {
        notifications.from(services.feed().notifications());
    }

    public void markAllRead() {
        markAll.from(services.feed().markAllRead());
    }

    public void consumeMarkAll() {
        markAll.reset();
    }
}
