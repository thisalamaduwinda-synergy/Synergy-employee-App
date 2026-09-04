package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.data.remote.dto.OvertimeDto;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;

/** DTO → domain for overtime. */
public final class OvertimeMapper {

    private OvertimeMapper() {
    }

    @NonNull
    public static Overtime toDomain(@NonNull OvertimeDto dto) {
        YearMonth month = Wire.month(dto.month);
        if (month == null) {
            month = YearMonth.now();
        }

        List<Overtime.Entry> entries = new ArrayList<>();
        if (dto.entries != null) {
            for (OvertimeDto.Entry e : dto.entries) {
                entries.add(new Overtime.Entry(
                        Wire.dateOr(e.date, LocalDate.now()),
                        Wire.timeOr(e.outTime, LocalTime.MIDNIGHT),
                        e.minutes,
                        ApprovalStatus.fromApi(e.status)));
            }
        }

        return new Overtime(
                month,
                dto.totalMinutes,
                dto.approvedMinutes,
                dto.pendingMinutes,
                dto.rateMultiplier <= 0f ? 1.5f : dto.rateMultiplier,
                money(dto.estimatedValue),
                entries);
    }

    /**
     * Money crosses the wire as a string, not a double — a float here would
     * eventually show an employee a rupee value that does not match their
     * payslip, and that is the kind of bug nobody forgives.
     */
    @NonNull
    private static BigDecimal money(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
