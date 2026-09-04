package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.enums.DayState;

/**
 * Decides the one colour a calendar day gets.
 *
 * <p>Order matters. Leave beats everything, because an employee on approved
 * leave must never be shown as absent — that is the mistake that generates the
 * angry phone call to HR. Only after leave and holidays are ruled out does a
 * missing gate record become ABSENT.
 */
public final class ResolveDayState {

    private ResolveDayState() {
    }

    public static DayState resolve(@NonNull LocalDate date,
                                   @NonNull List<GateEvent> events,
                                   boolean onApprovedLeave,
                                   @NonNull Set<LocalDate> holidays,
                                   @NonNull Set<DayOfWeek> restDays) {
        if (onApprovedLeave) {
            return DayState.LEAVE;
        }
        if (holidays.contains(date)) {
            return DayState.HOLIDAY;
        }
        if (restDays.contains(date.getDayOfWeek())) {
            return DayState.OFF;
        }
        if (!events.isEmpty()) {
            return DayState.WORKED;
        }
        // A future working day has simply not happened yet — not an absence.
        if (date.isAfter(LocalDate.now())) {
            return DayState.OFF;
        }
        return DayState.ABSENT;
    }
}
