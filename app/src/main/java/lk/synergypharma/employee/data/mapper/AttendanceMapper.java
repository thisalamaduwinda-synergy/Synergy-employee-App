package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lk.synergypharma.employee.data.remote.dto.AttendanceDayDto;
import lk.synergypharma.employee.data.remote.dto.GateEventDto;
import lk.synergypharma.employee.data.remote.dto.MonthSummaryDto;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.domain.model.enums.DayState;
import lk.synergypharma.employee.domain.model.enums.GateDirection;
import lk.synergypharma.employee.domain.usecase.CalculateWorkedTime;

/** DTO → domain for everything that comes out of the gate system. */
public final class AttendanceMapper {

    /** Matches the "General Shift" the recognition backend seeds. */
    private static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(17, 0);

    private AttendanceMapper() {
    }

    /**
     * @return the scan, or null if its timestamp was unusable — a single bad row
     *         must not take the whole day's timeline down with it
     */
    @Nullable
    static GateEvent toDomainOrNull(@NonNull GateEventDto dto) {
        LocalDateTime at = Wire.dateTime(dto.scannedAt);
        if (at == null) {
            return null;
        }
        return new GateEvent(
                String.valueOf(dto.id),
                at,
                GateDirection.fromApi(dto.direction),
                gateNameOf(dto),
                dto.cameraId == null ? "—" : String.valueOf(dto.cameraId),
                dto.confidence,
                dto.imagePath,
                dto.isDuplicate);
    }

    /**
     * The camera's own name is what an employee recognises ("Main Entrance -
     * OUT"); a bare id means nothing to them.
     */
    @NonNull
    private static String gateNameOf(@NonNull GateEventDto dto) {
        if (dto.cameraName != null && !dto.cameraName.trim().isEmpty()) {
            return dto.cameraName;
        }
        return dto.cameraId == null ? "Gate" : "Camera " + dto.cameraId;
    }

    /**
     * Duplicate scans are dropped here rather than in the UI. They exist for the
     * audit trail — a second reading inside the cooldown window that moved
     * nothing — and showing them would make a normal day look like the employee
     * walked in and out four times.
     */
    @NonNull
    public static List<GateEvent> toEvents(@Nullable List<GateEventDto> dtos) {
        List<GateEvent> out = new ArrayList<>();
        if (dtos == null) {
            return out;
        }
        for (GateEventDto d : dtos) {
            GateEvent e = toDomainOrNull(d);
            if (e != null && !e.duplicate) {
                out.add(e);
            }
        }
        // The timeline only makes sense in order, and the server may not sort.
        Collections.sort(out, Comparator.comparing(e -> e.timestamp));
        return out;
    }

    @NonNull
    public static AttendanceDay toDomain(@NonNull AttendanceDayDto dto) {
        LocalDate date = Wire.dateOr(dto.workDate, LocalDate.now());
        List<GateEvent> events = toEvents(dto.scans);

        return new AttendanceDay.Builder(date)
                .state(DayState.fromApi(dto.status))
                .events(events)
                .shift(Wire.timeOr(dto.shiftStart, DEFAULT_SHIFT_START),
                        Wire.timeOr(dto.shiftEnd, DEFAULT_SHIFT_END))
                // Overtime shows the *counted* figure, because that is the one
                // that reaches the payslip. Falls back to the raw measurement
                // while the backend migration adding the column is outstanding.
                .minutes(insideMinutesOf(dto, events),
                        dto.breakMinutes,
                        dto.overtimeCountedMinutes > 0
                                ? dto.overtimeCountedMinutes : dto.overtimeMinutes,
                        dto.lateMinutes)
                .note(dto.notes)
                .reasonSubmitted(dto.reasonSubmitted)
                .missingCheckout(dto.missingCheckout)
                .adjustment(dto.manuallyAdjusted, dto.adjustedBy, dto.adjustmentReason)
                .build();
    }

    /**
     * The backend stores {@code worked_minutes} net of the break; the app's
     * "total inside" is the gross figure, so the break is added back.
     *
     * <p>A day still in progress has {@code worked_minutes = 0} because there is
     * no check-out yet — so the running total is derived from the scans instead,
     * which is what makes today's card tick up during the shift.
     */
    private static int insideMinutesOf(@NonNull AttendanceDayDto dto,
                                       @NonNull List<GateEvent> events) {
        if (dto.workedMinutes > 0) {
            return dto.workedMinutes + dto.breakMinutes;
        }
        if (events.isEmpty()) {
            return 0;
        }
        boolean isToday = LocalDate.now().equals(Wire.date(dto.workDate));
        return CalculateWorkedTime.insideMinutes(
                events, isToday ? LocalDateTime.now() : null);
    }

    @NonNull
    public static MonthSummary toDomain(@NonNull MonthSummaryDto dto) {
        YearMonth month = Wire.month(dto.month);
        if (month == null) {
            month = YearMonth.now();
        }

        List<AttendanceDay> days = new ArrayList<>();
        if (dto.days != null) {
            for (AttendanceDayDto d : dto.days) {
                days.add(toDomain(d));
            }
            Collections.sort(days, Comparator.comparing(d -> d.date));
        }

        return new MonthSummary(
                month,
                dto.workedDays,
                dto.overtimeMinutes,
                dto.leaveDays,
                dto.absentDays,
                days);
    }
}
