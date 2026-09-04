package lk.synergypharma.employee.data.mapper;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.data.remote.dto.AnnouncementDto;
import lk.synergypharma.employee.data.remote.dto.NotificationDto;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.enums.NotificationType;

/** DTO → domain for the announcement feed and the notifications list. */
public final class FeedMapper {

    private FeedMapper() {
    }

    @NonNull
    public static Announcement toDomain(@NonNull AnnouncementDto dto) {
        LocalDateTime published = Wire.dateTime(dto.publishedAt);
        return new Announcement(
                Wire.text(dto.id, ""),
                Wire.text(dto.title, ""),
                dto.body,
                Wire.text(dto.category, ""),
                published == null ? LocalDateTime.now() : published,
                dto.pinned,
                dto.actionNeeded,
                dto.read);
    }

    @NonNull
    public static List<Announcement> toAnnouncements(List<AnnouncementDto> dtos) {
        List<Announcement> out = new ArrayList<>();
        if (dtos == null) {
            return out;
        }
        for (AnnouncementDto d : dtos) {
            out.add(toDomain(d));
        }
        return out;
    }

    @NonNull
    public static AppNotification toDomain(@NonNull NotificationDto dto) {
        LocalDateTime at = Wire.dateTime(dto.timestamp);
        return new AppNotification(
                Wire.text(dto.id, ""),
                NotificationType.fromApi(dto.type),
                Wire.text(dto.title, ""),
                dto.body,
                at == null ? LocalDateTime.now() : at,
                dto.read,
                dto.targetDate);
    }

    @NonNull
    public static List<AppNotification> toNotifications(List<NotificationDto> dtos) {
        List<AppNotification> out = new ArrayList<>();
        if (dtos == null) {
            return out;
        }
        for (NotificationDto d : dtos) {
            out.add(toDomain(d));
        }
        return out;
    }
}
