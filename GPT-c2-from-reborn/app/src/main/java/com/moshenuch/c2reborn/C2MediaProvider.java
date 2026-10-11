package com.moshenuch.c2reborn;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Per-URI-granted private camera output and gallery media provider.
 * A captured file never becomes accessible to other apps without an
 * explicit Android URI read/write grant.
 */
public final class C2MediaProvider extends ContentProvider {
    static final String AUTHORITY = "com.moshenuch.c2reborn.privatefiles";
    private static final int MAX_VIDEO_BYTES = 150 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 24 * 1024 * 1024;

    static Uri pendingUri(Context context, boolean video) {
        String extension = video ? ".mp4" : ".jpg";
        String name = (video ? "video_" : "photo_") +
                System.currentTimeMillis() + "_" + (System.nanoTime() & 0xffff) + extension;
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("capture").appendPath(name).build();
    }

    static void discard(Context context, Uri pending) {
        File file = resolve(context, pending, true);
        if (file != null && file.isFile()) file.delete();
    }

    /**
     * Saves external camera output into the app's private library.
     * Camera applications that ignore EXTRA_OUTPUT can return a read-granted
     * content URI. JPEG thumbnail-only fallback is supported for photo capture.
     */
    static Uri complete(Context context, Uri pending, boolean video, Intent result) {
        File temp = resolve(context, pending, true);
        if (temp == null) return null;
        File target = resolve(context, buildUri(video ? "video" : "image",
                pending.getLastPathSegment()), true);
        if (target == null) return null;
        boolean success = false;
        try {
            if (temp.isFile() && temp.length() > 128) {
                if (!copyFile(temp, target, video ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES))
                    return null;
            } else {
                Uri source = result == null ? null : result.getData();
                if (source != null && "content".equals(source.getScheme())) {
                    try (InputStream stream = context.getContentResolver().openInputStream(source)) {
                        if (stream == null || !copyStream(stream, target,
                                video ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES)) return null;
                    }
                } else if (!video && result != null && result.getExtras() != null &&
                        result.getExtras().get("data") instanceof Bitmap) {
                    Bitmap thumbnail = (Bitmap) result.getExtras().get("data");
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        if (!thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, out)) return null;
                    }
                } else return null;
            }
            if (target.isFile() && target.length() > 128) {
                success = true;
                return buildUri(video ? "video" : "image", target.getName());
            }
            return null;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (temp.isFile()) temp.delete();
            if (!success && target.isFile()) target.delete();
        }
    }

    private static boolean copyFile(File from, File to, long limit) throws IOException {
        try (InputStream in = new FileInputStream(from)) {
            return copyStream(in, to, limit);
        }
    }

    private static boolean copyStream(InputStream in, File to, long limit) throws IOException {
        long total = 0;
        try (OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[32 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > limit) return false;
                out.write(buffer, 0, n);
            }
        }
        return total > 128;
    }

    private static Uri buildUri(String category, String name) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath(category).appendPath(name).build();
    }

    private static File resolve(Context context, Uri uri, boolean createDirectory) {
        if (context == null || uri == null || !"content".equals(uri.getScheme()) ||
                !AUTHORITY.equals(uri.getAuthority())) return null;
        List<String> parts = uri.getPathSegments();
        if (parts.size() != 2) return null;
        String category = parts.get(0), name = parts.get(1);
        if (!name.matches("(photo_|video_)[a-zA-Z0-9_.-]+\\.(jpg|mp4)") ||
                name.contains("..")) return null;
        boolean video = name.startsWith("video_");
        if ("image".equals(category) && video) return null;
        if ("video".equals(category) && !video) return null;
        if (!"capture".equals(category) && !"image".equals(category)
                && !"video".equals(category)) return null;
        File dir = "capture".equals(category)
                ? new File(context.getCacheDir(), "camera_pending")
                : new File(context.getFilesDir(), video ? "camera_videos" : "camera_photos");
        if (createDirectory && !dir.exists() && !dir.mkdirs()) return null;
        return new File(dir, name);
    }

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        File file = resolve(getContext(), uri, false);
        if (file == null) return null;
        return file.getName().endsWith(".mp4") ? "video/mp4" : "image/jpeg";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        File file = resolve(getContext(), uri, false);
        if (file == null) return null;
        if (!file.isFile() && !"capture".equals(uri.getPathSegments().get(0))) return null;
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor result = new MatrixCursor(columns, 1);
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = file.getName();
            else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
        }
        result.addRow(values);
        return result;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        File file = resolve(getContext(), uri, false);
        if (file == null) throw new java.io.FileNotFoundException("Unsupported capture URI");
        boolean writing = mode.contains("w") || mode.contains("+");
        if (writing && !uri.getPathSegments().get(0).equals("capture"))
            throw new java.io.FileNotFoundException("Library assets are read-only");
        if (writing) {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs())
                throw new java.io.FileNotFoundException("Cannot create capture directory");
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE |
                    ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No public writes");
    }

    @Override public int delete(Uri uri, String where, String[] args) {
        throw new UnsupportedOperationException("No public deletes");
    }

    @Override public int update(Uri uri, ContentValues values, String where, String[] args) {
        throw new UnsupportedOperationException("No public updates");
    }
}
