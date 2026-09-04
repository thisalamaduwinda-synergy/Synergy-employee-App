package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.data.remote.dto.AbsenceReasonDto;
import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;
import lk.synergypharma.employee.domain.model.enums.ApprovalStatus;
import lk.synergypharma.employee.util.DateTimeUtils;

/** DTO ⇄ domain for absence reasons and their certificates. */
public final class AbsenceMapper {

    private AbsenceMapper() {
    }

    @NonNull
    public static AbsenceReason toDomain(@NonNull AbsenceReasonDto dto) {
        List<Attachment> attachments = new ArrayList<>();
        if (dto.attachments != null) {
            for (AbsenceReasonDto.AttachmentDto a : dto.attachments) {
                attachments.add(new Attachment(
                        Wire.text(a.fileName, "attachment"),
                        a.sizeBytes,
                        Wire.text(a.mimeType, "application/octet-stream"),
                        null,
                        a.url));
            }
        }
        return new AbsenceReason(
                dto.id,
                Wire.dateOr(dto.workDate, LocalDate.now()),
                AbsenceReasonType.fromApi(dto.reasonType),
                Wire.text(dto.explanation, ""),
                attachments,
                ApprovalStatus.fromApi(dto.status),
                Wire.dateTime(dto.submittedAt));
    }

    @NonNull
    public static List<AbsenceReason> toReasons(@Nullable List<AbsenceReasonDto> dtos) {
        List<AbsenceReason> out = new ArrayList<>();
        if (dtos == null) {
            return out;
        }
        for (AbsenceReasonDto d : dtos) {
            out.add(toDomain(d));
        }
        return out;
    }

    /**
     * Domain → DTO for the {@code reason} part of the multipart upload. The
     * files themselves go up as separate parts, not inside this object.
     */
    @NonNull
    public static AbsenceReasonDto toDto(@NonNull AbsenceReason reason) {
        AbsenceReasonDto dto = new AbsenceReasonDto();
        dto.id = reason.id;
        dto.workDate = DateTimeUtils.toIso(reason.date);
        dto.reasonType = reason.type.toApi();
        dto.explanation = reason.explanation;
        return dto;
    }
}
