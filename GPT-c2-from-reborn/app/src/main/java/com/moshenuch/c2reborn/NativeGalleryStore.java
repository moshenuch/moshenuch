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
import java.util.List;

/** A user-selected, app-local media index. Never deletes or modifies device files. */
final class NativeGalleryStore {
    static final class Entry {
        final String uri;
        final String title;
        Entry(String uri, String title) {
            this.uri = uri;
            this.title = title;
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
                    out.add(new Entry(uri, j.optString("title", "File")));
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
            list.add(new Entry(value, name));
            count++;
        }
        persist(video);
        return count;
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
