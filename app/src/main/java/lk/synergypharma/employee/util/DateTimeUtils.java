package lk.synergypharma.employee.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Every date and time the employee reads passes through here.
 *
 * <p>Formatters are built per call against {@link Locale#getDefault()} rather
 * than cached as static finals, because the app can switch to Sinhala at
 * runtime and a cached formatter would keep printing English month names.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    @NonNull
    private static DateTimeFormatter fmt(@NonNull String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault());
    }

    /** "08:12" */
    @NonNull
    public static String time(@Nullable LocalTime t) {
        return t == null ? "—:—" : t.format(fmt("HH:mm"));
    }

    /** "08:12:04" — the gate log shows seconds, because the gate records them. */
    @NonNull
    public static String timeWithSeconds(@NonNull LocalDateTime t) {
        return t.format(fmt("HH:mm:ss"));
    }

    /** "Tue, 12 August" */
    @NonNull
    public static String dayAndMonth(@NonNull LocalDate d) {
        return d.format(fmt("EEE, dd MMMM"));
    }

    /** "12 Aug" */
    @NonNull
    public static String shortDate(@NonNull LocalDate d) {
        return d.format(fmt("dd MMM"));
    }

    /** "Thu 13 Aug" — the home card's TODAY line. */
    @NonNull
    public static String weekdayShortDate(@NonNull LocalDate d) {
        return d.format(fmt("EEE dd MMM"));
    }

    /** "11 August 2026 (Mon)" */
    @NonNull
    public static String fullDate(@NonNull LocalDate d) {
        return d.format(fmt("dd MMMM yyyy (EEE)"));
    }

    /** "August 2026" */
    @NonNull
    public static String month(@NonNull YearMonth m) {
        return m.format(fmt("MMMM yyyy"));
    }

    /** "Aug" */
    @NonNull
    public static String monthShort(@NonNull YearMonth m) {
        return m.format(fmt("MMM"));
    }

    /**
     * "8h 42m", "48m", "0m". Used for every duration in the app so inside time,
     * OT and break deductions all read the same way.
     */
    @NonNull
    public static String duration(int minutes) {
        if (minutes <= 0) {
            return "0m";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + "m";
        }
        if (m == 0) {
            return h + "h";
        }
        return h + "h " + m + "m";
    }

    /** "6.5" — OT hours as a decimal, for the month summary tile. */
    @NonNull
    public static String hoursDecimal(int minutes) {
        return String.format(Locale.getDefault(), "%.1f", minutes / 60f);
    }

    /** "2 h ago", "3 days ago", "just now". */
    @NonNull
    public static String relative(@NonNull LocalDateTime then, @NonNull LocalDateTime now) {
        long mins = Duration.between(then, now).toMinutes();
        if (mins < 1) {
            return "just now";
        }
        if (mins < 60) {
            return mins + " min ago";
        }
        long hours = mins / 60;
        if (hours < 24) {
            return hours + " h ago";
        }
        long days = hours / 24;
        if (days == 1) {
            return "1 day ago";
        }
        if (days < 7) {
            return days + " days ago";
        }
        return shortDate(then.toLocalDate());
    }

    /** ISO {@code 2026-08-13}, the wire format for every date the API sees. */
    @NonNull
    public static String toIso(@NonNull LocalDate d) {
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @NonNull
    public static LocalDate fromIso(@NonNull String iso) {
        return LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
