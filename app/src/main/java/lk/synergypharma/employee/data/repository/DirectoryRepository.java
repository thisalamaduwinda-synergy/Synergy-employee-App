package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.util.Result;

/**
 * People lookup. In v1 this only feeds the covering-officer and approver
 * pickers on the leave form; the full directory screen lands in v1.1 on the same
 * call.
 */
public final class DirectoryRepository {

    @NonNull
    private final SynergyApi api;

    public DirectoryRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    @NonNull
    public LiveData<Result<List<Colleague>>> colleagues() {
        return Calls.live(api::colleagues);
    }
}
