package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

import lk.synergypharma.employee.util.Constants;

/**
 * Wire-format parsing, shared by every mapper.
 *
 * <p>All of it is null- and garbage-tolerant on purpose. A single malformed
 * timestamp from the gate system should cost the employee one row on a timeline,
 * not crash the screen that tells them whether they are being paid for the day.
 */
final class Wire {

    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("yyyy-MM", Locale.US);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private Wire() {
    }

    @Nullable
    static LocalDate date(@Nullable String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @NonNull
    static LocalDate dateOr(@Nullable String iso, @NonNull LocalDate fallback) {
        LocalDate d = date(iso);
        return d == null ? fallback : d;
    }

    /**
     * Space-separated naive timestamp with microseconds, which is what the
     * recognition backend actually stores:
     * {@code 2026-08-11 13:17:43.957009}.
     */
    private static final DateTimeFormatter SQL_TIMESTAMP = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter(Locale.US);

    /**
     * Parses a gate timestamp into plant local time.
     *
     * <p>Three shapes are accepted, in order of how much they can be trusted:
     * <ol>
     *   <li>{@code 2026-08-12T08:12:04+05:30} — carries its offset, so a phone
     *       roaming on another time zone still reads "08:12";</li>
     *   <li>{@code 2026-08-12T08:12:04} — ISO, no offset;</li>
     *   <li>{@code 2026-08-11 13:17:43.957009} — a space separator and
     *       microseconds. This is the format the recognition backend writes
     *       today, and neither of the first two parsers will touch it.</li>
     * </ol>
     *
     * <p>The last two are read as already being plant local time, which is true
     * on site and the only sane assumption for a naive timestamp.
     */
    @Nullable
    static LocalDateTime dateTime(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw)
                    .atZoneSameInstant(Constants.PLANT_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException notOffset) {
            // fall through
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException notIsoLocal) {
            // fall through
        }
        try {
            return LocalDateTime.parse(raw, SQL_TIMESTAMP);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Nullable
    static LocalTime time(@Nullable String hhmm) {
        if (hhmm == null || hhmm.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(hhmm, TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(hhmm);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    @NonNull
    static LocalTime timeOr(@Nullable String hhmm, @NonNull LocalTime fallback) {
        LocalTime t = time(hhmm);
        return t == null ? fallback : t;
    }

    @Nullable
    static YearMonth month(@Nullable String yyyyMM) {
        if (yyyyMM == null || yyyyMM.isEmpty()) {
            return null;
        }
        try {
            return YearMonth.parse(yyyyMM, MONTH);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @NonNull
    static String monthToApi(@NonNull YearMonth m) {
        return m.format(MONTH);
    }

    @NonNull
    static String timeToApi(@NonNull LocalTime t) {
        return t.format(TIME);
    }

    @NonNull
    static String text(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
