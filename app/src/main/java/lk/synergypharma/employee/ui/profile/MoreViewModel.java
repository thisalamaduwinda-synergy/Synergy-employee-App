package lk.synergypharma.employee.ui.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.LocaleHelper;
import lk.synergypharma.employee.util.Result;

/** Profile, settings and logout. */
public final class MoreViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();
    private final Relay<Boolean> logout = new Relay<>();

    @NonNull
    public LiveData<Employee> employee() {
        return services.auth().employee();
    }

    @NonNull
    public LiveData<Result<Boolean>> logout() {
        return logout.live();
    }

    public void logOut() {
        logout.from(services.auth().logout());
    }

    public void consumeLogout() {
        logout.reset();
    }

    @Nullable
    public String languageTag() {
        return services.prefs().languageTag();
    }

    /**
     * Persist first, then apply: applying recreates the activity, and a choice
     * that is not on disk by then would be lost on the way back up.
     */
    public void setLanguage(@Nullable String tag) {
        services.prefs().setLanguageTag(tag);
        LocaleHelper.apply(tag);
    }
}
