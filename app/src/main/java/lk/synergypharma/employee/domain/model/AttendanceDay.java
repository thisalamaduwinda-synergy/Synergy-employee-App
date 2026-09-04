package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.synergypharma.employee.domain.model.enums.DayState;
import lk.synergypharma.employee.domain.model.enums.GateDirection;

/**
 * One calendar day, resolved from the gate scans plus the leave and holiday
 * tables.
 *
 * <p>Built through {@link Builder} rather than a fifteen-argument constructor —
 * most of these fields are optional provenance, and positional arguments would
 * make it far too easy to swap {@code lateMinutes} for {@code breakMinutes}
 * without the compiler noticing.
 */
public final class AttendanceDay {

    @NonNull
    public final LocalDate date;
    @NonNull
    public final DayState state;
    @NonNull
    public final List<GateEvent> events;
    @NonNull
    public final LocalTime shiftStart;
    @NonNull
    public final LocalTime shiftEnd;
    /** Sum of the IN→OUT spans, before the break deduction. */
    public final int insideMinutes;
    public final int breakMinutes;
    /** Minutes past shift end, as recorded. Approval is a separate step. */
    public final int overtimeMinutes;
    /** Minutes after the shift start (and its grace window) that the gate saw them. */
    public final int lateMinutes;
    /** Free text from HR, e.g. "Annual leave — approved". */
    @Nullable
    public final String note;
    /** An explanation for this day is already with HR. */
    public final boolean reasonSubmitted;
    /**
     * The gate recorded an arrival but never a departure — usually somebody
     * leaving by a door with no exit camera. The day is closed automatically at
     * the shift end, which is exactly the kind of number worth disputing.
     */
    public final boolean missingCheckout;
    /** HR edited this row by hand rather than the camera producing it. */
    public final boolean manuallyAdjusted;
    @Nullable
    public final String adjustedBy;
    @Nullable
    public final String adjustmentReason;

    private AttendanceDay(@NonNull Builder builder) {
        this.date = builder.date;
        this.state = builder.state;
        this.events = Collections.unmodifiableList(new ArrayList<>(builder.events));
        this.shiftStart = builder.shiftStart;
        this.shiftEnd = builder.shiftEnd;
        this.insideMinutes = builder.insideMinutes;
        this.breakMinutes = builder.breakMinutes;
        this.overtimeMinutes = builder.overtimeMinutes;
        this.lateMinutes = builder.lateMinutes;
        this.note = builder.note;
        this.reasonSubmitted = builder.reasonSubmitted;
        this.missingCheckout = builder.missingCheckout;
        this.manuallyAdjusted = builder.manuallyAdjusted;
        this.adjustedBy = builder.adjustedBy;
        this.adjustmentReason = builder.adjustmentReason;
    }

    @Nullable
    public LocalTime firstIn() {
        for (GateEvent e : events) {
            if (e.direction == GateDirection.IN) {
                return e.timestamp.toLocalTime();
            }
        }
        return null;
    }

    @Nullable
    public LocalTime lastOut() {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).direction == GateDirection.OUT) {
                return events.get(i).timestamp.toLocalTime();
            }
        }
        return null;
    }

    /** Walked in and has not walked back out — the home card shows "—:—" for OUT. */
    public boolean isStillInside() {
        return !events.isEmpty()
                && events.get(events.size() - 1).direction == GateDirection.IN;
    }

    public boolean hasGateRecord() {
        return !events.isEmpty();
    }

    /** Net worked minutes after the break deduction. Never negative. */
    public int workedMinutes() {
        return Math.max(0, insideMinutes - breakMinutes);
    }

    /** Absent, and the employee has not told HR why yet. */
    public boolean awaitingExplanation() {
        return state.needsExplanation() && !reasonSubmitted;
    }

    /**
     * True when something other than a clean pair of camera readings shaped this
     * day, so the screen can say why the numbers look the way they do.
     */
    public boolean hasProvenanceNote() {
        return missingCheckout || manuallyAdjusted;
    }

    public static final class Builder {

        private final LocalDate date;
        private DayState state = DayState.OFF;
        private List<GateEvent> events = Collections.emptyList();
        private LocalTime shiftStart = LocalTime.of(8, 0);
        private LocalTime shiftEnd = LocalTime.of(17, 0);
        private int insideMinutes;
        private int breakMinutes;
        private int overtimeMinutes;
        private int lateMinutes;
        private String note;
        private boolean reasonSubmitted;
        private boolean missingCheckout;
        private boolean manuallyAdjusted;
        private String adjustedBy;
        private String adjustmentReason;

        public Builder(@NonNull LocalDate date) {
            this.date = date;
        }

        public Builder state(@NonNull DayState value) {
            this.state = value;
            return this;
        }

        public Builder events(@NonNull List<GateEvent> value) {
            this.events = value;
            return this;
        }

        public Builder shift(@NonNull LocalTime start, @NonNull LocalTime end) {
            this.shiftStart = start;
            this.shiftEnd = end;
            return this;
        }

        public Builder minutes(int inside, int breakTime, int overtime, int late) {
            this.insideMinutes = inside;
            this.breakMinutes = breakTime;
            this.overtimeMinutes = overtime;
            this.lateMinutes = late;
            return this;
        }

        public Builder note(@Nullable String value) {
            this.note = value;
            return this;
        }

        public Builder reasonSubmitted(boolean value) {
            this.reasonSubmitted = value;
            return this;
        }

        public Builder missingCheckout(boolean value) {
            this.missingCheckout = value;
            return this;
        }

        public Builder adjustment(boolean adjusted,
                                  @Nullable String by,
                                  @Nullable String reason) {
            this.manuallyAdjusted = adjusted;
            this.adjustedBy = by;
            this.adjustmentReason = reason;
            return this;
        }

        @NonNull
        public AttendanceDay build() {
            return new AttendanceDay(this);
        }
    }
}
