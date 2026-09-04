package lk.synergypharma.employee.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * A file the employee attached — almost always a photo of a medical certificate.
 *
 * <p>{@link #localUri} is set while the file is still on the phone;
 * {@link #remoteUrl} once HR has it. Both being non-null is normal right after
 * an upload succeeds.
 */
public final class Attachment {

    @NonNull
    public final String fileName;
    public final long sizeBytes;
    @NonNull
    public final String mimeType;
    @Nullable
    public final String localUri;
    @Nullable
    public final String remoteUrl;

    public Attachment(@NonNull String fileName,
                      long sizeBytes,
                      @NonNull String mimeType,
                      @Nullable String localUri,
                      @Nullable String remoteUrl) {
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
        this.localUri = localUri;
        this.remoteUrl = remoteUrl;
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    /** "1.4 MB" / "820 KB" */
    @NonNull
    public String readableSize() {
        if (sizeBytes >= 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f));
        }
        return String.format(Locale.US, "%d KB", Math.max(1, sizeBytes / 1024));
    }
}
