package lk.synergypharma.employee.ui.absence;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.usecase.ValidateAbsenceReason;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.FileUtils;

/**
 * Gets a medical certificate off the phone and into an {@link Attachment}.
 *
 * <p>Camera first, deliberately: almost nobody in the plant has the certificate
 * as a PDF — they have a piece of paper from the OPD and a phone.
 *
 * <p>Images are downscaled and re-encoded before they are handed back, on a
 * worker thread. A raw 12 MP photo over plant mobile data does not finish
 * uploading, and an upload that silently never completes is the same as never
 * having submitted the reason at all.
 */
public final class DocumentPickerHelper {

    public interface Listener {
        void onPicked(@NonNull Attachment attachment);

        void onPickFailed(@StringRes int messageRes);

        /** Shown while compression runs; the file can be several megabytes. */
        void onPickBusy(boolean busy);
    }

    private final Fragment fragment;
    private final Listener listener;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Uri> cameraLauncher;
    private final ActivityResultLauncher<String> contentLauncher;

    @Nullable
    private Uri pendingCameraUri;

    /** Must be constructed from {@code Fragment.onCreate}, before STARTED. */
    public DocumentPickerHelper(@NonNull Fragment fragment, @NonNull Listener listener) {
        this.fragment = fragment;
        this.listener = listener;

        this.cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        ingest(pendingCameraUri, true);
                    }
                    pendingCameraUri = null;
                });

        this.contentLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ingest(uri, false);
                    }
                });
    }

    public void chooseSource() {
        Context context = fragment.requireContext();
        CharSequence[] options = {
                context.getString(R.string.absence_source_camera),
                context.getString(R.string.absence_source_gallery),
                context.getString(R.string.absence_source_file)
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.absence_supporting_doc)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            launchCamera();
                            break;
                        case 1:
                            contentLauncher.launch(Constants.MIME_IMAGE);
                            break;
                        default:
                            contentLauncher.launch(Constants.MIME_PDF);
                            break;
                    }
                })
                .show();
    }

    private void launchCamera() {
        Context context = fragment.requireContext();
        try {
            File target = FileUtils.createCaptureFile(context);
            pendingCameraUri = FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", target);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | IllegalArgumentException e) {
            listener.onPickFailed(R.string.error_generic);
        }
    }

    /**
     * @param fromCamera true when the file is our own capture, which means it is
     *                   safe to replace rather than copy
     */
    private void ingest(@NonNull Uri source, boolean fromCamera) {
        Context context = fragment.requireContext().getApplicationContext();
        listener.onPickBusy(true);

        worker.execute(() -> {
            String mime = fromCamera ? "image/jpeg" : FileUtils.mimeType(context, source);
            String originalName = fromCamera
                    ? "certificate.jpg"
                    : FileUtils.displayName(context, source);

            Attachment result;
            if (mime.startsWith("image/")) {
                File compressed = FileUtils.compressImage(
                        context, source, Constants.UPLOAD_TARGET_BYTES);
                if (compressed == null) {
                    main.post(() -> {
                        listener.onPickBusy(false);
                        listener.onPickFailed(R.string.error_generic);
                    });
                    return;
                }
                result = new Attachment(
                        originalName,
                        compressed.length(),
                        "image/jpeg",
                        Uri.fromFile(compressed).toString(),
                        null);
            } else {
                long size = FileUtils.sizeOf(context, source);
                if (size > ValidateAbsenceReason.MAX_FILE_BYTES) {
                    main.post(() -> {
                        listener.onPickBusy(false);
                        listener.onPickFailed(R.string.absence_error_file_too_large);
                    });
                    return;
                }
                // PDFs are uploaded as they are — re-encoding a scan would only
                // make the doctor's handwriting harder to read.
                result = new Attachment(originalName, size, mime, source.toString(), null);
            }

            final Attachment picked = result;
            main.post(() -> {
                listener.onPickBusy(false);
                listener.onPicked(picked);
            });
        });
    }
}
