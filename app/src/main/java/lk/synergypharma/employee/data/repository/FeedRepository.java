package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.util.Result;

/**
 * Announcements and notifications.
 *
 * <p>Notifications are what keep the app installed: without a reason to open it,
 * staff stop doing so around week two. The list here is the pull side of the
 * same payload Firebase Cloud Messaging will push in phase 9.
 */
public final class FeedRepository {

    @NonNull
    private final SynergyApi api;

    public FeedRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    @NonNull
    public LiveData<Result<List<Announcement>>> announcements() {
        return Calls.live(api::announcements);
    }

    @NonNull
    public LiveData<Result<List<AppNotification>>> notifications() {
        return Calls.live(api::notifications);
    }

    @NonNull
    public LiveData<Result<Boolean>> markAllRead() {
        return Calls.live(api::markAllNotificationsRead);
    }
}
