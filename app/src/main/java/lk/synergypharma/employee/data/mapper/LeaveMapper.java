package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lk.synergypharma.employee.data.remote.dto.HolidayDto;
import lk.synergypharma.employee.data.remote.dto.LeaveBalanceDto;
import lk.synergypharma.employee.data.remote.dto.LeaveRequestDto;
import lk.synergypharma.employee.domain.model.LeaveBalance;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import lk.synergypharma.employee.util.DateTimeUtils;

/** DTO ⇄ domain for leave. The one area that maps in both directions. */
public final class LeaveMapper {

    private LeaveMapper() {
    }

    /**
     * The backend keeps one row per leave type; the app wants them as one
     * balance object.
     *
     * <p>{@code carried_forward} is folded into the entitlement rather than
     * dropped: an employee with 21 days granted and 4 carried over genuinely has
     * 25, and showing 21 would have them believing they are out of leave a week
     * before they are.
     */
    @NonNull
    public static LeaveBalance toDomain(@Nullable List<LeaveBalanceDto> dtos) {
        List<LeaveBalance.Bucket> buckets = new ArrayList<>();
        int year = LocalDate.now().getYear();

        if (dtos != null) {
            for (LeaveBalanceDto b : dtos) {
                if (b.year > 0) {
                    year = b.year;
                }
                buckets.add(new LeaveBalance.Bucket(
                        LeaveType.fromApi(b.leaveType),
                        b.usedDays,
                        b.entitledDays + b.carriedForward));
            }
        }
        return new LeaveBalance(year, buckets);
    }

    /** Poya and mercantile days, so the day count on the form matches payroll. */
    @NonNull
    public static Set<LocalDate> toHolidays(@Nullable List<HolidayDto> dtos) {
        Set<LocalDate> out = new HashSet<>();
        if (dtos == null) {
            return out;
        }
        for (HolidayDto h : dtos) {
            LocalDate d = Wire.date(h.holidayDate);
            if (d != null) {
                out.add(d);
            }
        }
        return out;
    }

    @NonNull
    public static LeaveRequest toDomain(@NonNull LeaveRequestDto dto) {
        LocalDate from = Wire.dateOr(dto.startDate, LocalDate.now());
        LocalDate to = Wire.dateOr(dto.endDate, from);

        return new LeaveRequest(
                Wire.text(dto.id, from + "_" + dto.leaveType),
                LeaveType.fromApi(dto.leaveType),
                from,
                to,
                portionOf(dto),
                dto.totalDays,
                Wire.text(dto.reason, ""),
                dto.coveringOfficer,
                // Before a decision, show who it is with; after one, who signed it.
                dto.approvedBy != null ? dto.approvedBy : dto.approver,
                ApprovalStatus.fromApi(dto.status),
                dto.rejectionReason,
                Wire.dateTime(dto.approvedAt));
    }

    /**
     * The backend stores only {@code is_half_day}; which half is a field it does
     * not have a column for yet. Default to the morning when it says nothing —
     * guessing is better than showing "Full day" for a request that is not one.
     */
    @NonNull
    private static LeaveRequest.DayPortion portionOf(@NonNull LeaveRequestDto dto) {
        if (!dto.isHalfDay) {
            return LeaveRequest.DayPortion.FULL_DAY;
        }
        return "PM".equalsIgnoreCase(dto.halfDayPeriod)
                ? LeaveRequest.DayPortion.HALF_PM
                : LeaveRequest.DayPortion.HALF_AM;
    }

    @NonNull
    public static List<LeaveRequest> toRequests(@Nullable List<LeaveRequestDto> dtos) {
        List<LeaveRequest> out = new ArrayList<>();
        if (dtos == null) {
            return out;
        }
        for (LeaveRequestDto d : dtos) {
            out.add(toDomain(d));
        }
        return out;
    }

    /** Domain → DTO for {@code POST /me/leave}. */
    @NonNull
    public static LeaveRequestDto toDto(@NonNull LeaveType type,
                                        @NonNull LocalDate from,
                                        @NonNull LocalDate to,
                                        @NonNull LeaveRequest.DayPortion portion,
                                        float days,
                                        @NonNull String reason,
                                        @Nullable String coveringOfficer,
                                        @Nullable String approver) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.leaveType = type.toApi();
        dto.startDate = DateTimeUtils.toIso(from);
        dto.endDate = DateTimeUtils.toIso(to);
        dto.isHalfDay = portion != LeaveRequest.DayPortion.FULL_DAY;
        dto.halfDayPeriod = portion == LeaveRequest.DayPortion.HALF_PM ? "PM"
                : portion == LeaveRequest.DayPortion.HALF_AM ? "AM" : null;
        dto.totalDays = days;
        dto.reason = reason;
        dto.coveringOfficer = coveringOfficer;
        dto.approver = approver;
        return dto;
    }
}
