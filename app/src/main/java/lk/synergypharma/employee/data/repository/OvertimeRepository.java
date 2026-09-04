package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.time.YearMonth;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.util.Result;

/**
 * Overtime, derived on the server from gate OUT times against the shift end.
 *
 * <p>Read-only by design: there is nothing here for the employee to type, only
 * to watch and — if it looks wrong — query through an attendance correction.
 */
public final class OvertimeRepository {

    @NonNull
    private final SynergyApi api;

    public OvertimeRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    @NonNull
    public LiveData<Result<Overtime>> month(@NonNull YearMonth month) {
        return Calls.live(cb -> api.overtime(month, cb));
    }
}
