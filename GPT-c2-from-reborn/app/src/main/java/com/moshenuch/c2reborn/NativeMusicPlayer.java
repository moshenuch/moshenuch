package com.moshenuch.c2reborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Plays explicitly user-selected audio through Android's MediaPlayer audio session. */
final class NativeMusicPlayer {
    static final class Track {
        final String uri;
        final String name;
        Track(String uri, String name) { this.uri = uri; this.name = name; }
    }

    private final Context context;
    private final SharedPreferences prefs;
    final ArrayList<Track> tracks = new ArrayList<>();
    private MediaPlayer player;
    private Equalizer equalizer;
    private int activeIndex = -1;
    private int eqPreset = -1; // -1 = Off
    private String[] availablePresets = new String[0];
    private String error = "";

    NativeMusicPlayer(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
        eqPreset = prefs.getInt("music_eq_preset_v1", -1);
        try {
            JSONArray a = new JSONArray(prefs.getString("music_library_v1", "[]"));
            for (int i = 0; i < a.length() && i < 200; i++) {
                JSONObject j = a.optJSONObject(i);
                if (j != null && j.optString("uri", "").startsWith("content://"))
                    tracks.add(new Track(j.optString("uri"), j.optString("name", "Audio file")));
            }
        } catch (Exception ignored) { }
    }

    int index() { return activeIndex; }
    String error() { return error; }
    boolean isPlaying() {
        try { return player != null && player.isPlaying(); }
        catch (IllegalStateException ignored) { return false; }
    }
    String title() {
        Track current = at(activeIndex);
        return current == null ? "No audio selected" : current.name;
    }
    int positionMs() {
        try { return player == null ? 0 : player.getCurrentPosition(); }
        catch (IllegalStateException ignored) { return 0; }
    }
    int durationMs() {
        try { return player == null ? 0 : player.getDuration(); }
        catch (IllegalStateException ignored) { return 0; }
    }
    Track at(int idx) { return idx >= 0 && idx < tracks.size() ? tracks.get(idx) : null; }

    private void save() {
        JSONArray result = new JSONArray();
        try {
            for (Track t : tracks) {
                JSONObject j = new JSONObject();
                j.put("uri", t.uri);
                j.put("name", t.name);
                result.put(j);
            }
        } catch (Exception ignored) { }
        prefs.edit().putString("music_library_v1", result.toString()).apply();
    }

    int add(List<Uri> selected) {
        int count = 0;
        for (Uri uri : selected) {
            if (tracks.size() >= 200 || uri == null || !"content".equals(uri.getScheme()))
                continue;
            String key = uri.toString();
            boolean duplicate = false;
            for (Track t : tracks) {
                if (t.uri.equals(key)) { duplicate = true; break; }
            }
            if (duplicate) continue;
            String name = uri.getLastPathSegment();
            try (Cursor c = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) name = c.getString(0);
            } catch (Exception ignored) { }
            if (name == null || name.isEmpty()) name = "Audio file";
            tracks.add(new Track(key, name));
            count++;
        }
        if (count != 0) save();
        return count;
    }

    boolean open(int index, boolean autoplay) {
        Track selected = at(index);
        if (selected == null) { error = "No audio selected"; return false; }
        releasePlayback();
        MediaPlayer next = new MediaPlayer();
        try {
            next.setDataSource(context, Uri.parse(selected.uri));
            next.setOnCompletionListener(done -> {
                // Keep the completed track loaded, but do not claim continuous play.
                try { done.seekTo(0); } catch (Exception ignored) { }
            });
            next.prepare();
            player = next;
            activeIndex = index;
            error = "";
            setupEqualizer();
            if (autoplay) next.start();
            return true;
        } catch (Exception ex) {
            try { next.release(); } catch (Exception ignored) { }
            player = null;
            activeIndex = -1;
            error = "Unable to open audio (access may be revoked)";
            return false;
        }
    }

    boolean toggle() {
        if (player == null) {
            if (tracks.isEmpty()) { error = "Select audio files first"; return false; }
            return open(activeIndex >= 0 ? activeIndex : 0, true);
        }
        try {
            if (player.isPlaying()) player.pause();
            else player.start();
            return true;
        } catch (Exception ignored) {
            error = "Playback failed";
            return false;
        }
    }

    boolean skip(int offset) {
        if (tracks.isEmpty()) { error = "No audio files selected"; return false; }
        int index = activeIndex < 0 ? 0 :
                ((activeIndex + offset) % tracks.size() + tracks.size()) % tracks.size();
        return open(index, true);
    }

    boolean remove(int index) {
        if (at(index) == null) return false;
        if (index == activeIndex) releasePlayback();
        else if (index < activeIndex) activeIndex--;
        tracks.remove(index);
        save();
        return true;
    }

    private void setupEqualizer() {
        availablePresets = new String[0];
        if (player == null) return;
        try {
            equalizer = new Equalizer(0, player.getAudioSessionId());
            short n = equalizer.getNumberOfPresets();
            availablePresets = new String[n];
            for (short i = 0; i < n; i++) availablePresets[i] = equalizer.getPresetName(i);
            if (eqPreset >= 0 && eqPreset < n) {
                equalizer.usePreset((short) eqPreset);
                equalizer.setEnabled(true);
            } else {
                eqPreset = -1;
                equalizer.setEnabled(false);
            }
        } catch (Exception ignored) {
            releaseEqualizer();
            error = "Equaliser unavailable on this Android device";
        }
    }

    boolean hasEqualizer() { return equalizer != null; }
    String[] presets() {
        String[] values = new String[availablePresets.length + 1];
        values[0] = "Off";
        System.arraycopy(availablePresets, 0, values, 1, availablePresets.length);
        return values;
    }
    int selectedPresetRow() { return Math.max(0, eqPreset + 1); }

    boolean choosePreset(int row) {
        if (row < 0 || row > availablePresets.length || equalizer == null) {
            error = "Equaliser requires supported, open audio";
            return false;
        }
        try {
            if (row == 0) equalizer.setEnabled(false);
            else {
                equalizer.usePreset((short)(row - 1));
                equalizer.setEnabled(true);
            }
            eqPreset = row - 1;
            prefs.edit().putInt("music_eq_preset_v1", eqPreset).apply();
            error = "";
            return true;
        } catch (Exception ignored) {
            error = "Unable to apply audio preset";
            return false;
        }
    }

    private void releaseEqualizer() {
        if (equalizer == null) return;
        try { equalizer.setEnabled(false); } catch (Exception ignored) { }
        try { equalizer.release(); } catch (Exception ignored) { }
        equalizer = null;
    }

    private void releasePlayback() {
        releaseEqualizer();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    void pause() {
        try { if (player != null && player.isPlaying()) player.pause(); }
        catch (Exception ignored) { }
    }
    void release() { releasePlayback(); }
}
