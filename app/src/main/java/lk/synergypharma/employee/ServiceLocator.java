package lk.synergypharma.employee;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import lk.synergypharma.employee.data.api.MockSynergyApi;
import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.data.local.PrefsManager;
import lk.synergypharma.employee.data.repository.AbsenceRepository;
import lk.synergypharma.employee.data.repository.AttendanceRepository;
import lk.synergypharma.employee.data.repository.AuthRepository;
import lk.synergypharma.employee.data.repository.DirectoryRepository;
import lk.synergypharma.employee.data.repository.FeedRepository;
import lk.synergypharma.employee.data.repository.LeaveRepository;
import lk.synergypharma.employee.data.repository.OvertimeRepository;

/**
 * The app's wiring, in one readable file.
 *
 * <p><b>This is the switch.</b> Everything above it — every fragment, every
 * ViewModel — is written against {@code SynergyApi} and has no idea whether the
 * answers came from generated data or the HR server. Phase 9 is
 * {@link #createApi(Context)}: return the Retrofit-backed implementation instead
 * of {@link MockSynergyApi} and the whole app moves onto the real backend
 * without a screen changing.
 *
 * <p>A hand-written locator rather than Hilt on purpose: for an app this size it
 * is the same amount of code, it adds no annotation processor to the build, and
 * a maintainer can read the entire dependency graph in thirty seconds.
 */
public final class ServiceLocator {

    private static volatile ServiceLocator instance;

    @NonNull
    private final SynergyApi api;
    @NonNull
    private final PrefsManager prefs;

    @NonNull
    private final AuthRepository authRepository;
    @NonNull
    private final AttendanceRepository attendanceRepository;
    @NonNull
    private final LeaveRepository leaveRepository;
    @NonNull
    private final AbsenceRepository absenceRepository;
    @NonNull
    private final OvertimeRepository overtimeRepository;
    @NonNull
    private final FeedRepository feedRepository;
    @NonNull
    private final DirectoryRepository directoryRepository;

    private ServiceLocator(@NonNull Context context) {
        this.api = createApi(context);
        this.prefs = new PrefsManager(context);

        this.authRepository = new AuthRepository(api, prefs);
        this.attendanceRepository = new AttendanceRepository(api);
        this.leaveRepository = new LeaveRepository(api);
        this.absenceRepository = new AbsenceRepository(api);
        this.overtimeRepository = new OvertimeRepository(api);
        this.feedRepository = new FeedRepository(api);
        this.directoryRepository = new DirectoryRepository(api);
    }

    /**
     * Phase 9 changes exactly this method:
     *
     * <pre>
     * return BuildConfig.USE_MOCK_DATA
     *         ? new MockSynergyApi(context)
     *         : new RetrofitSynergyApi(ApiClient.create(prefs), context);
     * </pre>
     *
     * and the mock package can then be deleted outright.
     */
    @NonNull
    private static SynergyApi createApi(@NonNull Context context) {
        return new MockSynergyApi(context);
    }

    public static void init(@NonNull Context context) {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator(context.getApplicationContext());
                }
            }
        }
    }

    @NonNull
    public static ServiceLocator get() {
        ServiceLocator local = instance;
        if (local == null) {
            throw new IllegalStateException(
                    "ServiceLocator.init() must run in SynergyApp.onCreate()");
        }
        return local;
    }

    @VisibleForTesting
    public static void reset() {
        instance = null;
    }

    @NonNull
    public PrefsManager prefs() {
        return prefs;
    }

    @NonNull
    public AuthRepository auth() {
        return authRepository;
    }

    @NonNull
    public AttendanceRepository attendance() {
        return attendanceRepository;
    }

    @NonNull
    public LeaveRepository leave() {
        return leaveRepository;
    }

    @NonNull
    public AbsenceRepository absence() {
        return absenceRepository;
    }

    @NonNull
    public OvertimeRepository overtime() {
        return overtimeRepository;
    }

    @NonNull
    public FeedRepository feed() {
        return feedRepository;
    }

    @NonNull
    public DirectoryRepository directory() {
        return directoryRepository;
    }
}
