package lk.synergypharma.employee.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Everything to do with the medical-certificate file between the camera and the
 * upload.
 *
 * <p>The compression step is not an optimisation, it is a requirement: plant and
 * warehouse staff are usually on mobile data with a weak signal, and a raw 12 MP
 * certificate photo will simply never finish uploading.
 */
public final class FileUtils {

    /** Long edge after downscaling. Enough to read a doctor's handwriting. */
    private static final int MAX_EDGE_PX = 1600;

    private static final int MIN_QUALITY = 55;

    private FileUtils() {
    }

    @NonNull
    public static String displayName(@NonNull Context context, @NonNull Uri uri) {
        String name = queryString(context, uri, OpenableColumns.DISPLAY_NAME);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        String path = uri.getLastPathSegment();
        return path == null ? "attachment" : path;
    }

    public static long sizeOf(@NonNull Context context, @NonNull Uri uri) {
        String size = queryString(context, uri, OpenableColumns.SIZE);
        if (size != null) {
            try {
                return Long.parseLong(size);
            } catch (NumberFormatException ignored) {
                // fall through to the stream count below
            }
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return in == null ? 0L : in.available();
        } catch (IOException e) {
            return 0L;
        }
    }

    @NonNull
    public static String mimeType(@NonNull Context context, @NonNull Uri uri) {
        String type = context.getContentResolver().getType(uri);
        return type == null ? "application/octet-stream" : type;
    }

    @Nullable
    private static String queryString(@NonNull Context context, @NonNull Uri uri, @NonNull String column) {
        ContentResolver resolver = context.getContentResolver();
        try (Cursor c = resolver.query(uri, new String[]{column}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(column);
                if (idx >= 0 && !c.isNull(idx)) {
                    return c.getString(idx);
                }
            }
        } catch (Exception ignored) {
            // Some providers reject the projection; the callers all have a fallback.
        }
        return null;
    }

    /** Destination for an {@code ACTION_IMAGE_CAPTURE} shot, shared via FileProvider. */
    @NonNull
    public static File createCaptureFile(@NonNull Context context) throws IOException {
        File dir = new File(context.getCacheDir(), "captures");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create the capture directory");
        }
        return new File(dir, "cert_" + System.currentTimeMillis() + ".jpg");
    }

    /**
     * Downscales, rotates by EXIF and re-encodes until the JPEG fits under
     * {@code targetBytes}. Returns a new file in the cache — the original is
     * never touched, so a gallery pick stays intact.
     *
     * @return the compressed file, or null if the source could not be decoded
     *         (a PDF, for instance — those are uploaded as they are)
     */
    @Nullable
    public static File compressImage(@NonNull Context context,
                                     @NonNull Uri source,
                                     long targetBytes) {
        Bitmap bitmap = decodeScaled(context, source);
        if (bitmap == null) {
            return null;
        }
        bitmap = applyExifRotation(context, source, bitmap);

        int quality = 92;
        byte[] encoded;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        do {
            buffer.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer);
            encoded = buffer.toByteArray();
            quality -= 8;
        } while (encoded.length > targetBytes && quality >= MIN_QUALITY);

        try {
            File out = createCaptureFile(context);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(encoded);
            }
            return out;
        } catch (IOException e) {
            return null;
        } finally {
            bitmap.recycle();
        }
    }

    @Nullable
    private static Bitmap decodeScaled(@NonNull Context context, @NonNull Uri uri) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (IOException e) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sample = 1;
        int longEdge = Math.max(bounds.outWidth, bounds.outHeight);
        while (longEdge / sample > MAX_EDGE_PX) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Phone cameras store the orientation in EXIF instead of rotating the pixels.
     * Skipping this is why scanned certificates so often arrive sideways at HR.
     */
    @NonNull
    private static Bitmap applyExifRotation(@NonNull Context context,
                                            @NonNull Uri uri,
                                            @NonNull Bitmap bitmap) {
        int degrees;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return bitmap;
            }
            int orientation = new ExifInterface(in)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
                default:
                    return bitmap;
            }
        } catch (IOException e) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }
}
