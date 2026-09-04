package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.enums.GateDirection;

/**
 * Turns a raw stream of gate events into "minutes inside the plant".
 *
 * <p>Deliberately tolerant, because face recognition drops events in the real
 * world (mask, cap, camera down, power cut):
 * <ul>
 *   <li>a second IN with no OUT between them is ignored — the person never left;</li>
 *   <li>an OUT with no matching IN is ignored rather than counted as negative;</li>
 *   <li>a trailing IN with no OUT is left open, and only counted up to
 *       {@code now} if the caller asks for it.</li>
 * </ul>
 *
 * <p>This is a display aid only. Payroll runs its own calculation on the server;
 * if the two ever disagree, payroll wins and the employee files a correction.
 */
public final class CalculateWorkedTime {

    private CalculateWorkedTime() {
    }

    /**
     * @param events sorted ascending by timestamp
     * @param openUntil if non-null, an unclosed final IN is counted up to this
     *                  moment — that is what makes the home card's "Inside 4h 48m"
     *                  tick up during the day
     * @return whole minutes spent inside, never negative
     */
    public static int insideMinutes(@NonNull List<GateEvent> events,
                                    @Nullable LocalDateTime openUntil) {
        long minutes = 0;
        LocalDateTime openedAt = null;

        for (GateEvent e : events) {
            if (e.direction == GateDirection.IN) {
                // Duplicate IN: keep the first one. They never left.
                if (openedAt == null) {
                    openedAt = e.timestamp;
                }
            } else {
                if (openedAt != null) {
                    minutes += Duration.between(openedAt, e.timestamp).toMinutes();
                    openedAt = null;
                }
                // OUT with no matching IN: the IN was missed. Nothing to add.
            }
        }

        if (openedAt != null && openUntil != null && openUntil.isAfter(openedAt)) {
            minutes += Duration.between(openedAt, openUntil).toMinutes();
        }
        return (int) Math.max(0, minutes);
    }

    /** True when the last event of the day was an IN — still on site. */
    public static boolean isOpen(@NonNull List<GateEvent> events) {
        return !events.isEmpty()
                && events.get(events.size() - 1).direction == GateDirection.IN;
    }
}
