package lk.synergypharma.employee.ui.leave;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/** Balance, holidays and history for the leave screen. */
public final class LeaveViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<LeaveSnapshot> snapshot = new Relay<>();

    public LeaveViewModel() {
        refresh();
    }

    @NonNull
    public LiveData<Result<LeaveSnapshot>> snapshot() {
        return snapshot.live();
    }

    public void refresh() {
        snapshot.from(services.leave().snapshot());
    }
}
