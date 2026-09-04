package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;

/**
 * Overtime for a month, derived entirely from gate OUT times against the shift
 * end. There is nothing here for an employee to type — only to watch and query.
 */
public final class Overtime {

    public static final class Entry {
        @NonNull
        public final LocalDate date;
        @NonNull
        public final LocalTime outTime;
        public final int minutes;
        @NonNull
        public final ApprovalStatus status;

        public Entry(@NonNull LocalDate date,
                     @NonNull LocalTime outTime,
                     int minutes,
                     @NonNull ApprovalStatus status) {
            this.date = date;
            this.outTime = outTime;
            this.minutes = minutes;
            this.status = status;
        }
    }

    @NonNull
    public final YearMonth month;
    public final int totalMinutes;
    public final int approvedMinutes;
    public final int pendingMinutes;
    /** Payroll multiplier, e.g. 1.5 for a weekday. */
    public final float rateMultiplier;
    /** Provisional — payroll locks it on the 25th. */
    @NonNull
    public final BigDecimal estimatedValue;
    @NonNull
    public final List<Entry> entries;

    public Overtime(@NonNull YearMonth month,
                    int totalMinutes,
                    int approvedMinutes,
                    int pendingMinutes,
                    float rateMultiplier,
                    @NonNull BigDecimal estimatedValue,
                    @NonNull List<Entry> entries) {
        this.month = month;
        this.totalMinutes = totalMinutes;
        this.approvedMinutes = approvedMinutes;
        this.pendingMinutes = pendingMinutes;
        this.rateMultiplier = rateMultiplier;
        this.estimatedValue = estimatedValue;
        this.entries = Collections.unmodifiableList(entries);
    }
}
