package lk.synergypharma.employee.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDateTime;

/**
 * Timestamp parsing, guarded because the recognition backend does not send
 * ISO-8601.
 *
 * <p>It writes {@code 2026-08-11 13:17:43.957009} — a space separator, no
 * offset, microsecond precision. A parser that only handled ISO would drop every
 * scan on the floor and show the employee an empty gate log for a day they
 * worked, which is worse than an error.
 */
public class WireTest {

    @Test
    public void readsTheBackendsNaiveTimestamp() {
        LocalDateTime parsed = Wire.dateTime("2026-08-11 13:17:43.957009");

        assertEquals(LocalDateTime.of(2026, 8, 11, 13, 17, 43, 957_009_000), parsed);
    }

    @Test
    public void readsANaiveTimestampWithNoFraction() {
        assertEquals(LocalDateTime.of(2026, 8, 11, 13, 17, 43),
                Wire.dateTime("2026-08-11 13:17:43"));
    }

    @Test
    public void readsIsoWithNoOffset() {
        assertEquals(LocalDateTime.of(2026, 8, 12, 8, 12, 4),
                Wire.dateTime("2026-08-12T08:12:04"));
    }

    @Test
    public void convertsAnOffsetTimestampToPlantLocalTime() {
        // 02:42 UTC is 08:12 in Colombo — the employee must read 08:12 even if
        // the phone is roaming on another time zone.
        assertEquals(LocalDateTime.of(2026, 8, 12, 8, 12, 4),
                Wire.dateTime("2026-08-12T02:42:04Z"));
    }

    @Test
    public void keepsAnAlreadyLocalOffsetUnchanged() {
        assertEquals(LocalDateTime.of(2026, 8, 12, 8, 12, 4),
                Wire.dateTime("2026-08-12T08:12:04+05:30"));
    }

    @Test
    public void returnsNullForRubbishRatherThanThrowing() {
        // One bad row must cost a single line on a timeline, not the screen.
        assertNull(Wire.dateTime("not a date"));
        assertNull(Wire.dateTime(""));
        assertNull(Wire.dateTime(null));
    }

    @Test
    public void readsIsoDates() {
        assertEquals(java.time.LocalDate.of(2026, 8, 12), Wire.date("2026-08-12"));
        assertNull(Wire.date("12/08/2026"));
    }

    @Test
    public void readsShiftTimes() {
        assertEquals(java.time.LocalTime.of(8, 0), Wire.time("08:00"));
        // SQLite hands times back with microseconds attached.
        assertEquals(java.time.LocalTime.of(17, 0), Wire.time("17:00:00.000000"));
    }
}
