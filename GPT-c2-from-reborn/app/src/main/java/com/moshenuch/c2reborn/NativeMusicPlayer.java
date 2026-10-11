package com.moshenuch.c2reborn;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.os.Build;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Plays explicitly user-selected audio through Android's MediaPlayer audio session. */
final class NativeMusicPlayer {
    static final class Track {
        final String uri;
        final String name;
        Track(String uri, String name) { this.uri = uri; this.name = name; }
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Do not automatically resume after another app starts audio.
            pause();
            notifyChanged();
        }
    };
    private boolean focusHeld;
    private boolean shuffle;
    private int repeatMode; // 0 = Off, 1 = One, 2 = All
    private final Random random = new Random();
    private Runnable onChanged;
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
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        shuffle = prefs.getBoolean("music_shuffle_v1", false);
        repeatMode = Math.min(2, Math.max(0, prefs.getInt("music_repeat_v1", 0)));
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

    void setOnChanged(Runnable listener) { onChanged = listener; }
    private void notifyChanged() {
        if (onChanged != null) onChanged.run();
    }

    boolean shuffleEnabled() { return shuffle; }
    String repeatLabel() {
        return repeatMode == 1 ? "One" : repeatMode == 2 ? "All" : "Off";
    }
    boolean toggleShuffle() {
        shuffle = !shuffle;
        prefs.edit().putBoolean("music_shuffle_v1", shuffle).apply();
        notifyChanged();
        return shuffle;
    }
    String nextRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        prefs.edit().putInt("music_repeat_v1", repeatMode).apply();
        notifyChanged();
        return repeatLabel();
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

    private boolean requestPlaybackFocus() {
        if (focusHeld) return true;
        if (audioManager == null) return true;
        try {
            int result;
            if (Build.VERSION.SDK_INT >= 26) {
                if (focusRequest == null) {
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(attrs)
                            .setOnAudioFocusChangeListener(focusListener)
                            .setWillPauseWhenDucked(true)
                            .build();
                }
                result = audioManager.requestAudioFocus(focusRequest);
            } else {
                result = audioManager.requestAudioFocus(focusListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            return focusHeld;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void abandonPlaybackFocus() {
        if (!focusHeld || audioManager == null) return;
        focusHeld = false;
        try {
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null)
                audioManager.abandonAudioFocusRequest(focusRequest);
            else audioManager.abandonAudioFocus(focusListener);
        } catch (RuntimeException ignored) { }
    }

    private void afterCompletion(MediaPlayer done) {
        if (player != done) return;
        try {
            if (repeatMode == 1) {
                done.seekTo(0);
                done.start();
            } else if (repeatMode == 2 && !tracks.isEmpty()) {
                int index = chooseNextIndex(1);
                if (!open(index, true)) {
                    pause();
                }
            } else {
                done.seekTo(0);
                abandonPlaybackFocus();
            }
        } catch (RuntimeException failure) {
            error = "Playback completion failed";
            pause();
        }
        notifyChanged();
    }

    private int chooseNextIndex(int offset) {
        if (tracks.isEmpty()) return -1;
        if (shuffle && offset > 0 && tracks.size() > 1) {
            int old = activeIndex < 0 ? 0 : activeIndex;
            int randomOffset = 1 + random.nextInt(tracks.size() - 1);
            return (old + randomOffset) % tracks.size();
        }
        return activeIndex < 0 ? 0 :
                ((activeIndex + offset) % tracks.size() + tracks.size()) % tracks.size();
    }

    boolean open(int index, boolean autoplay) {
        Track selected = at(index);
        if (selected == null) { error = "No audio selected"; return false; }
        releasePlayback();
        MediaPlayer next = new MediaPlayer();
        try {
            next.setDataSource(context, Uri.parse(selected.uri));
            next.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            next.setOnCompletionListener(this::afterCompletion);
            next.prepare();
            player = next;
            activeIndex = index;
            error = "";
            setupEqualizer();
            if (autoplay) {
                if (!requestPlaybackFocus()) {
                    releasePlayback();
                    activeIndex = -1;
                    error = "Audio focus unavailable";
                    return false;
                }
                next.start();
            }
            notifyChanged();
            return true;
        } catch (Exception ex) {
            if (player == next) releaseEqualizer();
            try { next.release(); } catch (Exception ignored) { }
            player = null;
            abandonPlaybackFocus();
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
            if (player.isPlaying()) {
                player.pause();
                abandonPlaybackFocus();
            } else {
                if (!requestPlaybackFocus()) {
                    error = "Audio focus unavailable";
                    return false;
                }
                player.start();
            }
            notifyChanged();
            return true;
        } catch (Exception ignored) {
            error = "Playback failed";
            return false;
        }
    }

    boolean skip(int offset) {
        if (tracks.isEmpty()) { error = "No audio files selected"; return false; }
        return open(chooseNextIndex(offset), true);
    }

    boolean remove(int index) {
        if (at(index) == null) return false;
        if (index == activeIndex) {
            releasePlayback();
            activeIndex = -1;
        } else if (index < activeIndex) activeIndex--;
        tracks.remove(index);
        save();
        notifyChanged();
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
        abandonPlaybackFocus();
        releaseEqualizer();
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    void pause() {
        try {
            if (player != null && player.isPlaying()) player.pause();
        } catch (Exception ignored) { }
        abandonPlaybackFocus();
        notifyChanged();
    }
    void release() {
        releasePlayback();
        onChanged = null;
    }
}
