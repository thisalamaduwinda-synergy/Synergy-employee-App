package lk.synergypharma.employee.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.util.Result;

/**
 * The employee's explanations for days the gate has no record of.
 *
 * <p>Face recognition misses people — a mask, a cap, a camera down, a power cut.
 * Without this path an employee silently loses a day's pay and HR fields a phone
 * call, so it is deliberately in v1 rather than v2.
 */
public final class AbsenceRepository {

    @NonNull
    private final SynergyApi api;

    public AbsenceRepository(@NonNull SynergyApi api) {
        this.api = api;
    }

    @NonNull
    public LiveData<Result<List<AbsenceReason>>> reasons() {
        return Calls.live(api::absenceReasons);
    }

    /**
     * Sends the reason and its certificate to HR.
     *
     * <p>Attachments must already be compressed — see
     * {@code FileUtils#compressImage}. A raw camera photo will not finish
     * uploading on plant mobile data.
     */
    @NonNull
    public LiveData<Result<AbsenceReason>> submit(@NonNull AbsenceReason reason) {
        return Calls.live(cb -> api.submitAbsenceReason(reason, cb));
    }
}
