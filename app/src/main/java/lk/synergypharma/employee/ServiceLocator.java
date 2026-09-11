package lk.synergypharma.employee;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import lk.synergypharma.employee.data.api.MockSynergyApi;
import lk.synergypharma.employee.data.api.RemoteSynergyApi;
import lk.synergypharma.employee.data.api.SynergyApi;
import lk.synergypharma.employee.data.local.PrefsManager;
import lk.synergypharma.employee.data.remote.ApiClient;
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
 * answers came from generated data or the HR server. {@code createApi} picks
 * {@link RemoteSynergyApi} or {@link MockSynergyApi} from {@code USE_MOCK_DATA}
 * and the whole app moves between them without a screen changing.
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
        this.prefs = new PrefsManager(context);
        this.api = createApi(context, prefs);

        this.authRepository = new AuthRepository(api, prefs);
        this.attendanceRepository = new AttendanceRepository(api);
        this.leaveRepository = new LeaveRepository(api);
        this.absenceRepository = new AbsenceRepository(api);
        this.overtimeRepository = new OvertimeRepository(api);
        this.feedRepository = new FeedRepository(api);
        this.directoryRepository = new DirectoryRepository(api);
    }

    /**
     * The only place that decides mock vs. real. {@code USE_MOCK_DATA} is set
     * per build type in {@code app/build.gradle.kts}; once every call in
     * {@link RemoteSynergyApi} is real the mock package can be deleted outright.
     */
    @NonNull
    private static SynergyApi createApi(@NonNull Context context, @NonNull PrefsManager prefs) {
        return BuildConfig.USE_MOCK_DATA
                ? new MockSynergyApi(context)
                : new RemoteSynergyApi(ApiClient.create(prefs), context);
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
