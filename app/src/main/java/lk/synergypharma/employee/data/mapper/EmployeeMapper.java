package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.data.remote.dto.ColleagueDto;
import lk.synergypharma.employee.data.remote.dto.EmployeeDto;
import lk.synergypharma.employee.data.remote.dto.LoginDto;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.domain.model.enums.UserRole;

/** DTO → domain for people. */
public final class EmployeeMapper {

    /** The "General Shift" the recognition backend seeds: 08:00–17:00. */
    private static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(17, 0);

    private EmployeeMapper() {
    }

    @NonNull
    public static Employee toDomain(@NonNull EmployeeDto dto) {
        // At Synergy the employee code *is* the EPF number — the enrolled face
        // images on the recognition server are named after it (1213.jpg) — so
        // both fields resolve to it rather than inventing a second identifier.
        String code = Wire.text(dto.employeeCode, "—");

        return new Employee(
                code,
                code,
                Wire.text(dto.fullName, "—"),
                Wire.text(dto.designation, ""),
                Wire.text(dto.department, ""),
                Wire.text(dto.branch, ""),
                UserRole.fromApi(dto.role),
                Wire.timeOr(dto.shiftStart, DEFAULT_SHIFT_START),
                Wire.timeOr(dto.shiftEnd, DEFAULT_SHIFT_END),
                dto.phone,
                dto.email);
    }

    @NonNull
    public static Session toSession(@NonNull LoginDto.Response dto) {
        return new Session(
                Wire.text(dto.accessToken, ""),
                Wire.text(dto.refreshToken, ""),
                toDomain(dto.employee));
    }

    /**
     * Domain → DTO, used only to cache the record on the phone. The domain model
     * carries {@code LocalTime} fields that Gson cannot serialise on its own, so
     * everything persisted goes through the wire shape.
     */
    @NonNull
    public static EmployeeDto toDto(@NonNull Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.employeeCode = employee.employeeId;
        dto.fullName = employee.fullName;
        dto.designation = employee.designation;
        dto.department = employee.department;
        dto.branch = employee.location;
        dto.role = employee.role.name().toLowerCase();
        dto.shiftStart = Wire.timeToApi(employee.shiftStart);
        dto.shiftEnd = Wire.timeToApi(employee.shiftEnd);
        dto.phone = employee.phone;
        dto.email = employee.email;
        return dto;
    }

    @NonNull
    public static Colleague toDomain(@NonNull ColleagueDto dto) {
        return new Colleague(
                Wire.text(dto.employeeCode, "—"),
                Wire.text(dto.fullName, "—"),
                Wire.text(dto.designation, ""),
                Wire.text(dto.department, ""),
                dto.phone,
                dto.canApprove);
    }

    @NonNull
    public static List<Colleague> toColleagues(@NonNull List<ColleagueDto> dtos) {
        List<Colleague> out = new ArrayList<>(dtos.size());
        for (ColleagueDto d : dtos) {
            out.add(toDomain(d));
        }
        return out;
    }
}
