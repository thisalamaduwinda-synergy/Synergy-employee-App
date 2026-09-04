package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.enums.DayState;

/** A month of attendance: the four counters plus every day behind them. */
public final class MonthSummary {

    @NonNull
    public final YearMonth month;
    public final int workedDays;
    public final int overtimeMinutes;
    public final int leaveDays;
    public final int absentDays;
    /** Every day in the month, in date order. */
    @NonNull
    public final List<AttendanceDay> days;

    public MonthSummary(@NonNull YearMonth month,
                        int workedDays,
                        int overtimeMinutes,
                        int leaveDays,
                        int absentDays,
                        @NonNull List<AttendanceDay> days) {
        this.month = month;
        this.workedDays = workedDays;
        this.overtimeMinutes = overtimeMinutes;
        this.leaveDays = leaveDays;
        this.absentDays = absentDays;
        this.days = Collections.unmodifiableList(days);
    }

    @Nullable
    public AttendanceDay dayOf(@NonNull LocalDate date) {
        for (AttendanceDay d : days) {
            if (d.date.equals(date)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Days the employee still owes HR a reason for. This drives the home-screen
     * alert — the highest-value thing in the whole app, because a missed face
     * scan otherwise silently costs someone a day's pay.
     */
    @NonNull
    public List<AttendanceDay> daysNeedingExplanation() {
        List<AttendanceDay> out = new ArrayList<>();
        for (AttendanceDay d : days) {
            if (d.awaitingExplanation()) {
                out.add(d);
            }
        }
        return out;
    }

    /** Newest first — the "Daily record" list only shows days worth reading. */
    @NonNull
    public List<AttendanceDay> notableDays() {
        List<AttendanceDay> out = new ArrayList<>();
        for (AttendanceDay d : days) {
            if (d.state != DayState.OFF && d.state != DayState.HOLIDAY
                    && !d.date.isAfter(LocalDate.now())) {
                out.add(d);
            }
        }
        Collections.reverse(out);
        return out;
    }
}
