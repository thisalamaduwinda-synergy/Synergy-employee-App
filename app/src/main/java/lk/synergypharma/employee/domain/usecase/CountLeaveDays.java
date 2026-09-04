package lk.synergypharma.employee.domain.usecase;

import androidx.annotation.NonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import lk.synergypharma.employee.domain.model.LeaveRequest;

/**
 * How many leave days a date range actually costs.
 *
 * <p>Poya days, mercantile holidays and rest days are excluded — an employee
 * should not burn annual leave on a day the plant was shut anyway. The holiday
 * set comes from HR, never from a table baked into the APK, because the Poya
 * calendar shifts every year.
 *
 * <p>This runs live as the employee picks dates so the total updates before they
 * submit. The server recalculates it too; this is for the screen.
 */
public final class CountLeaveDays {

    private CountLeaveDays() {
    }

    public static float count(@NonNull LocalDate from,
                              @NonNull LocalDate to,
                              @NonNull LeaveRequest.DayPortion portion,
                              @NonNull Set<LocalDate> holidays,
                              @NonNull Set<DayOfWeek> restDays) {
        if (to.isBefore(from)) {
            return 0f;
        }

        int workingDays = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (restDays.contains(d.getDayOfWeek()) || holidays.contains(d)) {
                continue;
            }
            workingDays++;
        }

        if (workingDays == 0) {
            return 0f;
        }
        // A half day only makes sense on a single-day request. Over a range the
        // employee is away for whole days regardless of which half they picked.
        if (workingDays == 1) {
            return portion.factor;
        }
        return workingDays;
    }
}
