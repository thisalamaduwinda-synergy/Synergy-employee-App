package lk.synergypharma.employee.data.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lk.synergypharma.employee.data.remote.dto.AttendanceDayDto;
import lk.synergypharma.employee.data.remote.dto.GateEventDto;
import lk.synergypharma.employee.data.remote.dto.HolidayDto;
import lk.synergypharma.employee.data.remote.dto.LeaveRequestDto;
import lk.synergypharma.employee.data.remote.dto.MonthSummaryDto;
import lk.synergypharma.employee.data.remote.dto.NotificationDto;
import lk.synergypharma.employee.domain.usecase.CalculateOvertime;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.DateTimeUtils;

/**
 * Builds believable gate data for whatever date the app happens to be opened on.
 *
 * <p>Fixtures pinned to fixed dates go stale within a week and the calendar ends
 * up empty in a demo, so everything here is generated relative to <em>today</em>
 * and seeded off the date — the same day always produces the same times, which
 * means a screenshot taken now still matches the app tomorrow.
 *
 * <p>It emits <b>DTOs</b>, not domain objects, on purpose: the mock therefore
 * exercises the same mappers the real API will use, and a mistake in the wire
 * contract shows up now rather than at integration time.
 *
 * <p>The shapes and values here are taken from the live recognition database
 * rather than invented — the naive space-separated timestamps, the camera names,
 * the lowercase status vocabulary, and above all the confidence range. Real
 * accepted scans sit around 0.53–0.68 against a 0.45 accept threshold, not the
 * 0.98 a demo would flatter itself with, and the UI has to be honest about that.
 */
final class MockDataFactory {

    /** Rough OT rate used to fill the "estimated value" line. */
    private static final BigDecimal OT_RATE_PER_HOUR = new BigDecimal("900.00");

    /**
     * How the recognition backend actually writes timestamps:
     * {@code 2026-08-11 13:17:43.957009}. Naive, space separator, microseconds.
     */
    private static final DateTimeFormatter GATE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US);

    /** What a well-behaved new endpoint should send. Both are parsed. */
    private static final DateTimeFormatter OFFSET_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM", Locale.US);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    /** Mirrors the seeded cameras: "Main Entrance - IN" / "- OUT". */
    private static final String GATE_IN = "Main Entrance - IN";
    private static final String GATE_OUT = "Main Entrance - OUT";
    private static final String GATE_BLOCK = "OSD block entrance";

    private MockDataFactory() {
    }

    // ------------------------------------------------------------ calendars

    /**
     * Turns the {@code --MM-dd} tokens in the fixture into real dates for the
     * year being viewed, so the Poya markers do not vanish next January.
     */
    @NonNull
    static Set<LocalDate> holidaysFor(int year, @Nullable List<HolidayDto> holidays) {
        Set<LocalDate> out = new java.util.HashSet<>();
        if (holidays == null) {
            return out;
        }
        for (HolidayDto h : holidays) {
            if (h.holidayDate == null) {
                continue;
            }
            try {
                out.add(h.holidayDate.startsWith("--")
                        ? MonthDay.parse(h.holidayDate).atYear(year)
                        : LocalDate.parse(h.holidayDate));
            } catch (RuntimeException ignored) {
                // A fixture typo should not take the calendar down.
            }
        }
        return out;
    }

    private static boolean isWorkingDay(@NonNull LocalDate d, @NonNull Set<LocalDate> holidays) {
        return !Constants.DEFAULT_REST_DAYS.contains(d.getDayOfWeek()) && !holidays.contains(d);
    }

    /**
     * The one day in the month with no gate record. Every demo needs one,
     * because the "missing record → submit a reason" flow is the whole point of
     * the app. Returns null for a month that has not happened yet.
     */
    @Nullable
    static LocalDate absentDay(@NonNull YearMonth month, @NonNull Set<LocalDate> holidays) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        if (month.isAfter(thisMonth)) {
            return null;
        }

        LocalDate candidate = month.equals(thisMonth)
                ? today.minusDays(2)
                : month.atDay(Math.min(11, month.lengthOfMonth()));

        for (int i = 0; i < 12; i++) {
            if (candidate.isBefore(month.atDay(1))) {
                return null;
            }
            if (isWorkingDay(candidate, holidays)) {
                return candidate;
            }
            candidate = candidate.minusDays(1);
        }
        return null;
    }

    /** A day of approved leave earlier in the month, for calendar colour. */
    @NonNull
    static Set<LocalDate> leaveDays(@NonNull YearMonth month, @NonNull Set<LocalDate> holidays) {
        Set<LocalDate> out = new java.util.HashSet<>();
        LocalDate today = LocalDate.now();
        LocalDate candidate = month.atDay(Math.min(6, month.lengthOfMonth()));
        for (int i = 0; i < 6 && candidate.getMonthValue() == month.getMonthValue(); i++) {
            if (isWorkingDay(candidate, holidays) && candidate.isBefore(today)) {
                out.add(candidate);
                break;
            }
            candidate = candidate.plusDays(1);
        }
        return out;
    }

    // ----------------------------------------------------------- attendance

    @NonNull
    static MonthSummaryDto month(@NonNull YearMonth month,
                                 @NonNull LocalTime shiftStart,
                                 @NonNull LocalTime shiftEnd,
                                 @NonNull Set<LocalDate> holidays,
                                 @NonNull Set<LocalDate> explainedDays) {
        Set<LocalDate> leave = leaveDays(month, holidays);
        LocalDate absent = absentDay(month, holidays);

        MonthSummaryDto dto = new MonthSummaryDto();
        dto.month = month.format(MONTH_FMT);
        dto.days = new ArrayList<>();

        LocalDate today = LocalDate.now();
        int worked = 0, leaveCount = 0, absentCount = 0, otMinutes = 0;

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            AttendanceDayDto d = day(date, shiftStart, shiftEnd, holidays,
                    leave.contains(date),
                    date.equals(absent),
                    explainedDays.contains(date),
                    today);
            dto.days.add(d);

            switch (d.status) {
                case "present":
                case "late":
                    worked++;
                    otMinutes += d.overtimeCountedMinutes;
                    break;
                case "on_leave":
                    leaveCount++;
                    break;
                case "absent":
                    absentCount++;
                    break;
                default:
                    break;
            }
        }

        dto.workedDays = worked;
        dto.leaveDays = leaveCount;
        dto.absentDays = absentCount;
        dto.overtimeMinutes = otMinutes;
        return dto;
    }

    @NonNull
    static AttendanceDayDto day(@NonNull LocalDate date,
                                @NonNull LocalTime shiftStart,
                                @NonNull LocalTime shiftEnd,
                                @NonNull Set<LocalDate> holidays,
                                boolean onLeave,
                                boolean forcedAbsent,
                                boolean explained,
                                @NonNull LocalDate today) {
        AttendanceDayDto dto = new AttendanceDayDto();
        dto.workDate = DateTimeUtils.toIso(date);
        dto.shiftStart = shiftStart.format(TIME_FMT);
        dto.shiftEnd = shiftEnd.format(TIME_FMT);
        dto.scans = new ArrayList<>();

        if (onLeave) {
            dto.status = "on_leave";
            dto.notes = "Annual leave — approved";
            return dto;
        }
        if (holidays.contains(date)) {
            dto.status = "holiday";
            return dto;
        }
        if (Constants.DEFAULT_REST_DAYS.contains(date.getDayOfWeek()) || date.isAfter(today)) {
            // The recognition backend simply has no row for these; the app's
            // DayState.fromApi lands anything it does not recognise on OFF.
            dto.status = "off";
            return dto;
        }
        if (forcedAbsent) {
            dto.status = "absent";
            dto.reasonSubmitted = explained;
            dto.notes = explained ? "Reason submitted — with HR" : null;
            return dto;
        }

        List<Stamp> stamps = stamps(date, shiftStart, shiftEnd, today);
        if (stamps.isEmpty()) {
            // Today, before the employee has reached the gate.
            dto.status = date.equals(today) ? "present" : "absent";
            return dto;
        }

        for (Stamp s : stamps) {
            dto.scans.add(scan(s, date));
        }

        boolean stillInside = stamps.get(stamps.size() - 1).in;
        LocalDateTime openUntil = date.equals(today) ? LocalDateTime.now() : null;
        int inside = insideMinutes(stamps, openUntil);
        int breakMinutes = breakMinutes(stamps);

        dto.breakMinutes = breakMinutes;
        // The backend stores worked_minutes net of the break, and leaves it at
        // zero while the day is still open.
        dto.workedMinutes = stillInside ? 0 : Math.max(0, inside - breakMinutes);
        dto.checkInAt = stamps.get(0).at.format(GATE_TIMESTAMP);
        dto.checkOutAt = stillInside ? null : lastOut(stamps).format(GATE_TIMESTAMP);

        dto.overtimeMinutes = rawOvertimeMinutes(stamps, shiftEnd);
        dto.overtimeCountedMinutes = CalculateOvertime.minutes(
                shiftEnd, stillInside ? null : lastOut(stamps).toLocalTime());
        dto.lateMinutes = lateMinutes(stamps.get(0).at.toLocalTime(), shiftStart);

        // Somebody leaving by a door with no exit camera. Rare, but it is the
        // single most disputed record there is, so the demo has to contain one.
        dto.missingCheckout = !stillInside && new Rng(date.toEpochDay() * 7).next(14) == 0;

        dto.status = dto.lateMinutes > 0 ? "late" : "present";
        return dto;
    }

    // ------------------------------------------------------- gate simulation

    /** One recorded crossing, before it becomes a DTO. */
    private static final class Stamp {
        final LocalDateTime at;
        final boolean in;
        final String gate;
        final int cameraId;
        final float confidence;

        Stamp(LocalDateTime at, boolean in, String gate, int cameraId, float confidence) {
            this.at = at;
            this.in = in;
            this.gate = gate;
            this.cameraId = cameraId;
            this.confidence = confidence;
        }
    }

    private static List<Stamp> stamps(@NonNull LocalDate date,
                                      @NonNull LocalTime shiftStart,
                                      @NonNull LocalTime shiftEnd,
                                      @NonNull LocalDate today) {
        Rng rng = new Rng(date.toEpochDay());
        List<Stamp> out = new ArrayList<>();

        LocalDateTime cutoff = date.equals(today) ? LocalDateTime.now() : null;

        LocalDateTime arrive = LocalDateTime.of(date,
                shiftStart.minusMinutes(20 - rng.next(32)).withSecond(rng.next(60)));
        if (cutoff != null && arrive.isAfter(cutoff)) {
            return out;
        }
        out.add(new Stamp(arrive, true, GATE_IN, 1, confidence(rng)));

        // Roughly six days in ten, the employee steps out for lunch and the
        // second camera picks them up both ways.
        if (rng.next(10) < 6) {
            LocalDateTime lunchOut = LocalDateTime.of(date,
                    LocalTime.of(12, 20 + rng.next(25), rng.next(60)));
            LocalDateTime lunchIn = lunchOut.plusMinutes(28 + rng.next(20));
            if (cutoff == null || lunchOut.isBefore(cutoff)) {
                out.add(new Stamp(lunchOut, false, GATE_OUT, 2, confidence(rng)));
                if (cutoff == null || lunchIn.isBefore(cutoff)) {
                    out.add(new Stamp(lunchIn, true, GATE_BLOCK, 3, confidence(rng)));
                }
            }
        }

        LocalDateTime leave = LocalDateTime.of(date,
                shiftEnd.plusMinutes(rng.next(150) - 8).withSecond(rng.next(60)));
        if (cutoff == null || leave.isBefore(cutoff)) {
            out.add(new Stamp(leave, false, GATE_OUT, 2, confidence(rng)));
        }
        return out;
    }

    /**
     * 0.48–0.72 cosine similarity.
     *
     * <p>Taken from the live database, where accepted scans read 0.53, 0.63,
     * 0.68 against a {@code FACE_MATCH_THRESHOLD} of 0.45. Generating 0.98 here
     * would have hidden the fact that the UI was calling every genuine scan
     * "low confidence".
     */
    private static float confidence(@NonNull Rng rng) {
        return 0.48f + rng.next(24) / 100f;
    }

    private static GateEventDto scan(@NonNull Stamp s, @NonNull LocalDate date) {
        GateEventDto dto = new GateEventDto();
        dto.id = date.toEpochDay() * 1000L + s.at.toLocalTime().toSecondOfDay() % 1000L;
        dto.scannedAt = s.at.format(GATE_TIMESTAMP);
        dto.direction = s.in ? "in" : "out";
        dto.confidence = s.confidence;
        dto.liveness = "real";
        dto.cameraId = s.cameraId;
        dto.cameraName = s.gate;
        dto.imagePath = String.format(Locale.US, "captures/%s/emp_%s_%s.jpg",
                DateTimeUtils.toIso(date), s.in ? "in" : "out",
                s.at.toLocalTime().toString().replace(":", ""));
        dto.source = "face_recognition";
        dto.isDuplicate = false;
        return dto;
    }

    private static LocalDateTime lastOut(@NonNull List<Stamp> stamps) {
        for (int i = stamps.size() - 1; i >= 0; i--) {
            if (!stamps.get(i).in) {
                return stamps.get(i).at;
            }
        }
        return stamps.get(stamps.size() - 1).at;
    }

    private static int insideMinutes(@NonNull List<Stamp> stamps, @Nullable LocalDateTime openUntil) {
        long total = 0;
        LocalDateTime openedAt = null;
        for (Stamp s : stamps) {
            if (s.in) {
                if (openedAt == null) {
                    openedAt = s.at;
                }
            } else if (openedAt != null) {
                total += Duration.between(openedAt, s.at).toMinutes();
                openedAt = null;
            }
        }
        if (openedAt != null && openUntil != null && openUntil.isAfter(openedAt)) {
            total += Duration.between(openedAt, openUntil).toMinutes();
        }
        return (int) Math.max(0, total);
    }

    /**
     * If they physically left the site for half an hour or more, the time is
     * already excluded and nothing further is deducted. If they never left, the
     * shift's unpaid break comes off.
     */
    private static int breakMinutes(@NonNull List<Stamp> stamps) {
        for (int i = 0; i < stamps.size() - 1; i++) {
            Stamp a = stamps.get(i);
            Stamp b = stamps.get(i + 1);
            if (!a.in && b.in && Duration.between(a.at, b.at).toMinutes() >= 30) {
                return 0;
            }
        }
        return 60;
    }

    /** Every minute past the shift end, before the payable rounding. */
    private static int rawOvertimeMinutes(@NonNull List<Stamp> stamps,
                                          @NonNull LocalTime shiftEnd) {
        LocalTime out = lastOut(stamps).toLocalTime();
        if (!out.isAfter(shiftEnd)) {
            return 0;
        }
        return (int) Duration.between(shiftEnd, out).toMinutes();
    }

    /** Past the shift start and its grace window. Matches the seeded 15 minutes. */
    private static int lateMinutes(@NonNull LocalTime arrival, @NonNull LocalTime shiftStart) {
        LocalTime graceEnd = shiftStart.plusMinutes(15);
        if (!arrival.isAfter(graceEnd)) {
            return 0;
        }
        return (int) Duration.between(shiftStart, arrival).toMinutes();
    }

    // -------------------------------------------------------------- overtime

    @NonNull
    static lk.synergypharma.employee.data.remote.dto.OvertimeDto overtime(
            @NonNull YearMonth month, @NonNull MonthSummaryDto summary) {
        lk.synergypharma.employee.data.remote.dto.OvertimeDto dto =
                new lk.synergypharma.employee.data.remote.dto.OvertimeDto();
        dto.month = month.format(MONTH_FMT);
        dto.entries = new ArrayList<>();
        dto.rateMultiplier = 1.5f;

        int total = 0, approved = 0, pending = 0;
        LocalDate cutoff = LocalDate.now().minusDays(5);

        for (AttendanceDayDto d : summary.days) {
            if (d.overtimeCountedMinutes <= 0 || d.checkOutAt == null) {
                continue;
            }
            LocalDate date = DateTimeUtils.fromIso(d.workDate);

            // Supervisors sign OT off in weekly batches, so anything recent is
            // still sitting in someone's queue.
            boolean isApproved = date.isBefore(cutoff);

            lk.synergypharma.employee.data.remote.dto.OvertimeDto.Entry e =
                    new lk.synergypharma.employee.data.remote.dto.OvertimeDto.Entry();
            e.date = d.workDate;
            e.outTime = d.checkOutAt.substring(11, 16);
            e.minutes = d.overtimeCountedMinutes;
            e.status = isApproved ? "approved" : "pending";
            dto.entries.add(e);

            total += e.minutes;
            if (isApproved) {
                approved += e.minutes;
            } else {
                pending += e.minutes;
            }
        }

        java.util.Collections.reverse(dto.entries);
        dto.totalMinutes = total;
        dto.approvedMinutes = approved;
        dto.pendingMinutes = pending;
        dto.estimatedValue = OT_RATE_PER_HOUR
                .multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .toPlainString();
        return dto;
    }

    // ----------------------------------------------------------------- leave

    @NonNull
    static List<LeaveRequestDto> leaveRequests(@NonNull Set<LocalDate> holidays) {
        LocalDate today = LocalDate.now();
        List<LeaveRequestDto> out = new ArrayList<>();

        LocalDate upcoming = nextWorkingDay(today.plusDays(12), holidays);
        out.add(request("lv-101", "annual", upcoming, nextWorkingDay(upcoming.plusDays(1), holidays),
                2f, "Family function in Kandy.", "Kasun Jayasinghe", "Nuwan Perera",
                "pending", null));

        LocalDate approved = previousWorkingDay(today.minusDays(9), holidays);
        out.add(request("lv-102", "sick", approved, approved, 1f,
                "Fever — advised one day rest.", "Sandarekha Gunasiri", "Ruwani Silva",
                "approved", null));

        LocalDate rejected = previousWorkingDay(today.minusDays(24), holidays);
        out.add(request("lv-103", "casual", rejected, rejected, 1f,
                "Personal matter in Colombo.", "Sandarekha Gunasiri", "Ruwani Silva",
                "rejected", "Clashes with audit week."));

        return out;
    }

    private static LeaveRequestDto request(String id, String type, LocalDate from, LocalDate to,
                                           float days, String reason, String covering,
                                           String approver, String status, String rejection) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.id = id;
        dto.leaveType = type;
        dto.startDate = DateTimeUtils.toIso(from);
        dto.endDate = DateTimeUtils.toIso(to);
        dto.isHalfDay = false;
        dto.totalDays = days;
        dto.reason = reason;
        dto.coveringOfficer = covering;
        dto.approver = approver;
        dto.approvedBy = "approved".equals(status) ? approver : null;
        dto.status = status;
        dto.rejectionReason = rejection;
        return dto;
    }

    @NonNull
    static LocalDate nextWorkingDay(@NonNull LocalDate from, @NonNull Set<LocalDate> holidays) {
        LocalDate d = from;
        for (int i = 0; i < 10 && !isWorkingDay(d, holidays); i++) {
            d = d.plusDays(1);
        }
        return d;
    }

    @NonNull
    static LocalDate previousWorkingDay(@NonNull LocalDate from, @NonNull Set<LocalDate> holidays) {
        LocalDate d = from;
        for (int i = 0; i < 10 && !isWorkingDay(d, holidays); i++) {
            d = d.minusDays(1);
        }
        return d;
    }

    // --------------------------------------------------------- notifications

    /**
     * Built from the same generated month the rest of the app is showing, so
     * tapping "No gate record for 11 Aug" lands on a day that really is missing.
     */
    @NonNull
    static List<NotificationDto> notifications(@Nullable LocalDate absentDay,
                                               @NonNull MonthSummaryDto month,
                                               boolean absenceExplained) {
        List<NotificationDto> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (absentDay != null && !absenceExplained) {
            out.add(notification("n-1", "MISSING_GATE_RECORD",
                    "No gate record for " + DateTimeUtils.shortDate(absentDay),
                    "Submit a reason within 7 days",
                    now.minusHours(3), false, DateTimeUtils.toIso(absentDay)));
        }
        out.add(notification("n-2", "LEAVE_DECISION",
                "Leave approved",
                "Approved by R. Silva",
                now.minusHours(9), absentDay == null, null));

        if (month.overtimeMinutes > 0) {
            out.add(notification("n-3", "OVERTIME",
                    "OT " + DateTimeUtils.duration(Math.min(month.overtimeMinutes, 195)) + " recorded",
                    "Awaiting supervisor approval",
                    now.minusDays(1).minusHours(2), true, null));
        }
        out.add(notification("n-4", "PAYSLIP",
                "Payslip released",
                "Net LKR 186,450.00",
                now.minusDays(2), true, null));
        out.add(notification("n-5", "ANNOUNCEMENT",
                "GMP refresher training",
                "Register before Friday",
                now.minusDays(4), true, null));

        return out;
    }

    private static NotificationDto notification(String id, String type, String title,
                                                String body, LocalDateTime at,
                                                boolean read, String targetDate) {
        NotificationDto dto = new NotificationDto();
        dto.id = id;
        dto.type = type;
        dto.title = title;
        dto.body = body;
        dto.timestamp = at.atZone(Constants.PLANT_ZONE).format(OFFSET_TIMESTAMP);
        dto.read = read;
        dto.targetDate = targetDate;
        return dto;
    }

    // ------------------------------------------------------------- feed time

    /**
     * Fixtures store {@code publishedAt} as {@code -2h} / {@code -3d} so the
     * announcement feed never reads "published 8 months ago" in a demo.
     */
    @NonNull
    static String resolveRelativeTimestamp(@Nullable String token) {
        LocalDateTime now = LocalDateTime.now();
        if (token == null || token.isEmpty() || !token.startsWith("-")) {
            return token == null ? isoNow() : token;
        }
        try {
            char unit = token.charAt(token.length() - 1);
            long amount = Long.parseLong(token.substring(1, token.length() - 1));
            LocalDateTime then = unit == 'h' ? now.minusHours(amount) : now.minusDays(amount);
            return then.atZone(Constants.PLANT_ZONE).format(OFFSET_TIMESTAMP);
        } catch (RuntimeException e) {
            return isoNow();
        }
    }

    @NonNull
    static String isoNow() {
        return LocalDateTime.now().atZone(Constants.PLANT_ZONE).format(OFFSET_TIMESTAMP);
    }

    // ------------------------------------------------------------------ rng

    /**
     * Seeded so the same date always yields the same gate times. A plain
     * {@code Random} would reshuffle the whole month on every screen rotation.
     */
    private static final class Rng {
        private long state;

        Rng(long seed) {
            this.state = seed * 6364136223846793005L + 1442695040888963407L;
        }

        int next(int bound) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            return (int) ((state >>> 33) % bound);
        }
    }
}
