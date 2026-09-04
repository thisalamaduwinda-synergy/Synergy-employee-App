package lk.synergypharma.employee.ui.absence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;
import lk.synergypharma.employee.ui.common.Relay;
import lk.synergypharma.employee.util.Result;

/**
 * Holds the half-finished form.
 *
 * <p>Worth keeping in a ViewModel rather than the fragment: taking the photo
 * leaves the app for the camera, and on a low-memory phone the fragment can be
 * destroyed while it is gone. Losing the typed explanation at that point would
 * mean starting over.
 */
public final class AbsenceViewModel extends ViewModel {

    private final ServiceLocator services = ServiceLocator.get();

    private final MutableLiveData<List<Attachment>> attachments =
            new MutableLiveData<>(new ArrayList<>());
    private final Relay<AbsenceReason> submission = new Relay<>();

    @Nullable
    private LocalDate date;
    @Nullable
    private AbsenceReasonType type;

    @NonNull
    public LiveData<List<Attachment>> attachments() {
        return attachments;
    }

    @NonNull
    public LiveData<Result<AbsenceReason>> submission() {
        return submission.live();
    }

    @NonNull
    public List<Attachment> currentAttachments() {
        List<Attachment> current = attachments.getValue();
        return current == null ? new ArrayList<>() : current;
    }

    public void addAttachment(@NonNull Attachment attachment) {
        List<Attachment> next = new ArrayList<>(currentAttachments());
        next.add(attachment);
        attachments.setValue(next);
    }

    public void removeAttachment(int index) {
        List<Attachment> next = new ArrayList<>(currentAttachments());
        if (index >= 0 && index < next.size()) {
            next.remove(index);
            attachments.setValue(next);
        }
    }

    @Nullable
    public LocalDate date() {
        return date;
    }

    public void setDate(@Nullable LocalDate date) {
        this.date = date;
    }

    @Nullable
    public AbsenceReasonType type() {
        return type;
    }

    public void setType(@Nullable AbsenceReasonType type) {
        this.type = type;
    }

    public void submit(@NonNull LocalDate date,
                       @NonNull AbsenceReasonType type,
                       @NonNull String explanation) {
        submission.from(services.absence().submit(
                AbsenceReason.draft(date, type, explanation, currentAttachments())));
    }

    public void consumeSubmission() {
        submission.reset();
    }
}
