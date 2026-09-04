package lk.synergypharma.employee.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * English ⇄ සිංහල switching.
 *
 * <p>Uses AndroidX per-app locales rather than the old
 * {@code Configuration}-wrapping trick: on Android 13+ the choice is stored by
 * the system and shows up in Settings → Apps → Synergy Employee → Language, and
 * on older versions AppCompat persists it for us. Either way the activity
 * recreates itself and every {@code strings.xml} lookup swaps over — which is
 * why no layout in this project contains hard-coded text.
 */
public final class LocaleHelper {

    public static final String EN = "en";
    public static final String SI = "si";

    private LocaleHelper() {
    }

    /** @param tag {@link #EN}, {@link #SI}, or null to follow the phone. */
    public static void apply(@Nullable String tag) {
        LocaleListCompat locales = (tag == null || tag.isEmpty())
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(tag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    /** Language tag currently in force, or null when following the phone. */
    @Nullable
    public static String current() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return null;
        }
        String tag = locales.toLanguageTags();
        int comma = tag.indexOf(',');
        return comma > 0 ? tag.substring(0, comma) : tag;
    }

    @NonNull
    public static String currentOrDefault() {
        String tag = current();
        return tag == null ? EN : tag;
    }

    public static boolean isSinhala() {
        return SI.equalsIgnoreCase(currentOrDefault());
    }
}
