package com.moshenuch.c2reborn;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Native app-private audio recording. No system-wide media writes or background mic use. */
final class NativeVoiceRecorder {
    private final File folder;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File pending;
    private long startedAt;

    NativeVoiceRecorder(Context context) {
        folder = new File(context.getFilesDir(), "voice_notes");
        if (!folder.exists() && !folder.mkdirs()) {
            // start() reports an error if the directory is still unavailable.
        }
    }

    synchronized boolean isRecording() { return recorder != null; }

    synchronized long elapsedSeconds() {
        return recorder == null ? 0 : Math.max(0, (SystemClock.elapsedRealtime() - startedAt) / 1000);
    }

    synchronized boolean start() {
        if (recorder != null) return false;
        stopPlayback();
        if (!folder.exists() && !folder.mkdirs()) return false;
        File destination = new File(folder, "rec_" + System.currentTimeMillis() + ".m4a");
        MediaRecorder next = null;
        try {
            next = new MediaRecorder();
            next.setAudioSource(MediaRecorder.AudioSource.MIC);
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            next.setAudioChannels(1);
            next.setAudioEncodingBitRate(64000);
            next.setAudioSamplingRate(22050);
            // The native LCD UI stops and saves after five foreground minutes.
            // Avoid MediaRecorder auto-stop changing state behind the UI.
            next.setOutputFile(destination.getAbsolutePath());
            next.prepare();
            next.start();
            recorder = next;
            pending = destination;
            startedAt = SystemClock.elapsedRealtime();
            return true;
        } catch (Exception ignored) {
            if (next != null) {
                try { next.reset(); } catch (Exception ignoredReset) { }
                next.release();
            }
            if (destination.exists()) destination.delete();
            return false;
        }
    }

    /** Returns saved file, or null when stop failed / clip was too short. */
    synchronized File stop() {
        if (recorder == null) return null;
        MediaRecorder finishing = recorder;
        File result = pending;
        recorder = null;
        pending = null;
        boolean valid = false;
        try {
            finishing.stop();
            valid = result != null && result.isFile() && result.length() > 128;
        } catch (RuntimeException ignored) {
            valid = false;
        } finally {
            try { finishing.reset(); } catch (RuntimeException ignored) { }
            finishing.release();
        }
        if (!valid && result != null) result.delete();
        return valid ? result : null;
    }

    synchronized ArrayList<File> entries() {
        File[] files = folder.listFiles((dir, name) ->
                name.startsWith("rec_") && name.endsWith(".m4a"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return new ArrayList<>(Arrays.asList(files));
    }

    synchronized File at(int index) {
        ArrayList<File> records = entries();
        return index >= 0 && index < records.size() ? records.get(index) : null;
    }

    synchronized boolean delete(int index) {
        File selected = at(index);
        if (selected == null || isRecording()) return false;
        stopPlayback();
        return selected.delete();
    }

    synchronized boolean togglePlayback(int index, Runnable onComplete) {
        File selected = at(index);
        if (selected == null || recorder != null) return false;
        if (player != null) {
            stopPlayback();
            if (onComplete != null) onComplete.run();
            return true;
        }
        MediaPlayer next = new MediaPlayer();
        try {
            next.setDataSource(selected.getAbsolutePath());
            next.prepare();
            next.setOnCompletionListener(completed -> {
                stopPlayback();
                if (onComplete != null) onComplete.run();
            });
            next.start();
            player = next;
            return true;
        } catch (Exception ignored) {
            try { next.release(); } catch (Exception ignoredRelease) { }
            return false;
        }
    }

    synchronized boolean isPlaying() { return player != null && player.isPlaying(); }

    synchronized void stopPlayback() {
        if (player == null) return;
        MediaPlayer old = player;
        player = null;
        try { old.stop(); } catch (Exception ignored) { }
        try { old.release(); } catch (Exception ignored) { }
    }

    synchronized void release() {
        if (recorder != null) stop();
        stopPlayback();
    }

    static String title(File file) {
        java.text.SimpleDateFormat date = new java.text.SimpleDateFormat("dd MMM HH:mm", Locale.UK);
        return file == null ? "No recording" : "Rec " + date.format(new java.util.Date(file.lastModified()));
    }
}
