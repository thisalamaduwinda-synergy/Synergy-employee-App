package lk.synergypharma.employee.ui.overtime;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.time.YearMonth;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/** This month's overtime. */
public final class OvertimeViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<Overtime> overtime = new Relay<>();

    public OvertimeViewModel() {
        overtime.from(services.overtime().month(YearMonth.now()));
    }

    @NonNull
    public LiveData<Result<Overtime>> overtime() {
        return overtime.live();
    }
}
