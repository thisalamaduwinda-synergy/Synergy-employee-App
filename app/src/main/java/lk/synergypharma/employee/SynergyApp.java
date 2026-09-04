package lk.synergypharma.employee;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import lk.synergypharma.employee.util.LocaleHelper;

/**
 * Application entry point: wire up the dependency graph and restore the
 * employee's language before the first screen is laid out.
 */
public final class SynergyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ServiceLocator.init(this);

        // Dark theme is not in v1. Colour tokens are already split so that
        // adding res/values-night is a resource change and nothing else — until
        // then, pin light so the app never renders half-inverted on a phone
        // that is in dark mode.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        LocaleHelper.apply(ServiceLocator.get().prefs().languageTag());
    }
}
