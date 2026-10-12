package com.moshenuch.c2reborn;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A user-selected, app-local media index. Never deletes or modifies device files. */
final class NativeGalleryStore {
    static final class Entry {
        final String uri;
        final String title;
        final long addedAt;
        Entry(String uri, String title, long addedAt) {
            this.uri = uri;
            this.title = title;
            this.addedAt = addedAt;
        }
    }

    private final Context context;
    private final SharedPreferences prefs;
    final ArrayList<Entry> images = new ArrayList<>();
    final ArrayList<Entry> videos = new ArrayList<>();

    NativeGalleryStore(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
        read("gallery_images_v1", images);
        read("gallery_videos_v1", videos);
    }

    private void read(String key, ArrayList<Entry> out) {
        try {
            JSONArray a = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject j = a.optJSONObject(i);
                if (j == null) continue;
                String uri = j.optString("uri", "");
                if (uri.startsWith("content://"))
                    out.add(new Entry(uri, j.optString("title", "File"),
                            j.optLong("addedAt", 0L)));
            }
        } catch (Exception ignored) { }
    }

    private void persist(boolean video) {
        JSONArray a = new JSONArray();
        try {
            for (Entry entry : entries(video)) {
                JSONObject j = new JSONObject();
                j.put("uri", entry.uri);
                j.put("title", entry.title);
                j.put("addedAt", entry.addedAt);
                a.put(j);
            }
        } catch (Exception ignored) { }
        prefs.edit().putString(video ? "gallery_videos_v1" : "gallery_images_v1",
                a.toString()).apply();
    }

    List<Entry> entries(boolean video) { return video ? videos : images; }

    int add(boolean video, List<Uri> chosen) {
        int count = 0;
        ArrayList<Entry> list = video ? videos : images;
        ContentResolver resolver = context.getContentResolver();
        for (Uri uri : chosen) {
            if (list.size() >= 150 || uri == null || !"content".equals(uri.getScheme())) continue;
            String value = uri.toString();
            boolean exists = false;
            for (Entry old : list) if (old.uri.equals(value)) { exists = true; break; }
            if (exists) continue;
            String name = uri.getLastPathSegment();
            try (Cursor c = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (c != null && c.moveToFirst()) name = c.getString(0);
            } catch (Exception ignored) { }
            if (name == null || name.trim().isEmpty()) name = "Selected media";
            list.add(new Entry(value, name, System.currentTimeMillis() + count));
            count++;
        }
        if (count > 0) sort(video, sortMode(video));
        return count;
    }

    /**
     * Camera captures are more important than the bounded list of manually
     * imported items. Once the 150-row C2 index is full, rotate out only
     * its oldest entry; NEVER delete the actual device photo/video file.
     * Duplicate captures remain indexed and still open their result view.
     */
    boolean addCaptured(boolean video, Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return false;
        String value = uri.toString();
        ArrayList<Entry> list = video ? videos : images;
        for (Entry entry : list)
            if (value.equals(entry.uri)) return true;
        if (list.size() >= 150) {
            int oldest = 0;
            for (int i = 1; i < list.size(); i++) {
                if (list.get(i).addedAt < list.get(oldest).addedAt)
                    oldest = i;
            }
            list.remove(oldest);
            persist(video);
        }
        return add(video, Collections.singletonList(uri)) > 0;
    }

    int sortMode(boolean video) {
        return prefs.getInt(video ? "gallery_sort_video_v1" : "gallery_sort_image_v1", 2);
    }

    void sort(boolean video, int mode) {
        if (mode < 0 || mode > 2) return;
        ArrayList<Entry> list = video ? videos : images;
        Comparator<Entry> name = (a, b) ->
                a.title.toLowerCase(Locale.ROOT).compareTo(b.title.toLowerCase(Locale.ROOT));
        if (mode == 0) Collections.sort(list, name);
        else if (mode == 1) Collections.sort(list, name.reversed());
        else Collections.sort(list, (a, b) -> Long.compare(b.addedAt, a.addedAt));
        prefs.edit().putInt(video ? "gallery_sort_video_v1" : "gallery_sort_image_v1", mode).apply();
        persist(video);
    }

    Entry at(boolean video, int index) {
        List<Entry> list = entries(video);
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    boolean remove(boolean video, int index) {
        List<Entry> list = entries(video);
        if (index < 0 || index >= list.size()) return false;
        list.remove(index);
        persist(video);
        return true;
    }

    /** Remove an exact deleted media URI without disturbing other Gallery entries. */
    boolean removeUri(boolean video, String uri) {
        if (uri == null || uri.isEmpty()) return false;
        List<Entry> list = entries(video);
        for (int i = 0; i < list.size(); i++) {
            if (uri.equals(list.get(i).uri)) {
                list.remove(i);
                persist(video);
                return true;
            }
        }
        return false;
    }

    void clear(boolean video) {
        entries(video).clear();
        persist(video);
    }

    Bitmap decodePreview(String imageUri, int targetWidth, int targetHeight) {
        // Decode within the LCD's resolution; never allocate the original full-size photo.
        try {
            Uri uri = Uri.parse(imageUri);
            if (!"content".equals(uri.getScheme())) return null;
            BitmapFactory.Options bound = new BitmapFactory.Options();
            bound.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                BitmapFactory.decodeStream(input, null, bound);
            }
            int step = 1;
            while (bound.outWidth / step > targetWidth * 2 ||
                   bound.outHeight / step > targetHeight * 2) step *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, step);
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                return BitmapFactory.decodeStream(input, null, options);
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
