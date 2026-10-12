package com.moshenuch.c2reborn;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.provider.MediaStore;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private static final int REQ_PHONE_DATA = 201;
    private static final int REQ_PICK_IMAGES = 301;
    private static final int REQ_PICK_VIDEOS = 302;
    private static final int REQ_MICROPHONE = 303;
    private static final int REQ_CAPTURE_IMAGE = 304;
    private static final int REQ_CAPTURE_VIDEO = 305;
    private static final int REQ_PICK_AUDIO = 306;
    private Uri pendingCameraOutput;
    private boolean pendingCameraVideo;
    private C2PhoneView phoneView;
    private android.widget.FrameLayout phoneFrame;
    private C2CameraPreview cameraPreview;
    private static final int REQ_CAMERA_PREVIEW = 408;
    private static final int REQ_VIDEO_MIC = 409;
    private Uri pendingNativeVideo;
    private static MainActivity foreground;
    static void notifyCallUpdated() {
        MainActivity a = foreground;
        if (a != null) a.runOnUiThread(() -> { if (a.phoneView != null) a.phoneView.onTelecomCallUpdated(); });
    }
    static void notifyCallEnded() {
        MainActivity a = foreground;
        if (a != null) a.runOnUiThread(() -> { if (a.phoneView != null) a.phoneView.onTelecomCallEnded(); });
    }
    boolean hasDialerRole() {
        if (Build.VERSION.SDK_INT < 29) return false;
        android.app.role.RoleManager role =
                (android.app.role.RoleManager) getSystemService(ROLE_SERVICE);
        return role != null && role.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER);
    }
    void requestDialerRole() {
        if (Build.VERSION.SDK_INT < 29) {
            if (phoneView != null) phoneView.onDialerRoleUnavailable();
            return;
        }
        android.app.role.RoleManager role =
                (android.app.role.RoleManager) getSystemService(ROLE_SERVICE);
        if (role != null && role.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
            try {
                startActivityForResult(role.createRequestRoleIntent(
                        android.app.role.RoleManager.ROLE_DIALER), 407);
            } catch (RuntimeException ex) {
                requestDialerRoleLegacy();
            }
        } else requestDialerRoleLegacy();
    }

    // Observe the Android SMS provider only while this screen is visible.
    // Coalescing provider notifications avoids repeatedly querying the
    // 800-message snapshot when one SMS creates several database changes.
    private final Handler smsRefreshHandler = new Handler(Looper.getMainLooper());
    private ContentObserver smsObserver;
    private final Runnable smsRefreshRunnable = () -> {
        if (phoneView != null && hasSmsReadPermission())
            phoneView.onSmsProviderChanged();
    };

    private boolean hasSmsReadPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.READ_SMS)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void observeSmsWhileVisible() {
        stopSmsObservation();
        if (!hasSmsReadPermission()) return;
        smsObserver = new ContentObserver(smsRefreshHandler) {
            @Override
            public void onChange(boolean selfChange) {
                smsRefreshHandler.removeCallbacks(smsRefreshRunnable);
                smsRefreshHandler.postDelayed(smsRefreshRunnable, 300L);
            }

            @Override
            public void onChange(boolean selfChange, Uri changedUri) {
                onChange(selfChange);
            }
        };
        try {
            getContentResolver().registerContentObserver(
                    Telephony.Sms.CONTENT_URI, true, smsObserver);
        } catch (RuntimeException ex) {
            smsObserver = null; // Permission/provider unavailable: app still opens.
        }
    }

    private void stopSmsObservation() {
        smsRefreshHandler.removeCallbacks(smsRefreshRunnable);
        if (smsObserver != null) {
            try { getContentResolver().unregisterContentObserver(smsObserver); }
            catch (RuntimeException ignored) { }
            smsObserver = null;
        }
    }

    private void requestDialerRoleLegacy() {
        try {
            Intent intent = new Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    getPackageName());
            startActivityForResult(intent, 407);
        } catch (RuntimeException ex) {
            if (phoneView != null) phoneView.onDialerRoleUnavailable();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        foreground = this;
        phoneView = new C2PhoneView(this);
        phoneFrame = new android.widget.FrameLayout(this);
        phoneFrame.addView(phoneView, new android.widget.FrameLayout.LayoutParams(-1, -1));
        cameraPreview = new C2CameraPreview(this);
        cameraPreview.setVisibility(View.GONE);
        phoneFrame.addView(cameraPreview, new android.widget.FrameLayout.LayoutParams(1, 1));
        setContentView(phoneFrame);
        phoneFrame.addOnLayoutChangeListener((v,l,t,rr,b,ol,ot,orr,ob) -> layoutCameraPreview());
        if (savedInstanceState != null) {
            String captured = savedInstanceState.getString("pendingCameraOutput");
            if (captured != null) pendingCameraOutput = Uri.parse(captured);
            pendingCameraVideo = savedInstanceState.getBoolean("pendingCameraVideo", false);
        }
        requestPhonePermissions();
    }

    void showNativeCameraPreview(boolean show) {
        if (cameraPreview == null) return;
        if (!show) {
            cameraPreview.stop();
            cameraPreview.setVisibility(View.GONE);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PREVIEW);
            return;
        }
        layoutCameraPreview();
        cameraPreview.setVisibility(View.VISIBLE);
        cameraPreview.start();
    }

    boolean isNativeVideoRecording() {
        return cameraPreview != null && cameraPreview.isRecording();
    }

    void toggleNativeVideoCapture() {
        if (isNativeVideoRecording()) finishNativeVideoCapture();
        else startNativeVideoCapture();
    }

    private void startNativeVideoCapture() {
        if (phoneView == null || !phoneView.isInVideoViewfinder()) return;
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_VIDEO_MIC);
            return;
        }
        if (cameraPreview == null || !cameraPreview.isRunning()) {
            onNativePhotoFailed("Video camera preview unavailable");
            return;
        }
        Uri pending = C2MediaProvider.pendingUri(this, true);
        java.io.File directory = new java.io.File(getCacheDir(), "camera_pending");
        if ((!directory.exists() && !directory.mkdirs()) || pending.getLastPathSegment() == null) {
            onNativePhotoFailed("Unable to prepare video storage");
            return;
        }
        java.io.File output = new java.io.File(directory, pending.getLastPathSegment());
        if (!cameraPreview.startRecording(output)) {
            C2MediaProvider.discard(this, pending);
            onNativePhotoFailed("Video recorder could not start");
            return;
        }
        pendingNativeVideo = pending;
        phoneView.onNativeVideoRecordingChanged(true);
    }

    /** Finalise MP4 before releasing the camera; preserve a private copy if publish fails. */
    void finishNativeVideoCapture() {
        if (!isNativeVideoRecording()) return;
        Uri pending = pendingNativeVideo;
        pendingNativeVideo = null;
        boolean valid = cameraPreview.stopRecording();
        cameraPreview.stop();
        cameraPreview.setVisibility(View.GONE);
        if (pending == null || !valid) {
            if (pending != null) C2MediaProvider.discard(this, pending);
            onNativePhotoFailed("Video recording too short or interrupted");
            return;
        }
        Uri saved = C2MediaProvider.complete(this, pending, true, null);
        if (saved == null) {
            onNativePhotoFailed("Unable to save recorded video");
            return;
        }
        Uri shared = publishVideoToMovies(saved);
        if (shared != null) deleteCapturedPhoto(saved);
        if (phoneView != null) {
            phoneView.onCameraCaptureCompleted(true, shared != null ? shared : saved);
            if (shared == null)
                phoneView.onCameraCaptureFailed("Video saved privately; Movies unavailable");
        }
    }

    /** Share an MP4 through scoped storage, without requesting global storage access. */
    private Uri publishVideoToMovies(Uri privateUri) {
        if (Build.VERSION.SDK_INT < 29) return null;
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME,
                "C2_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_MOVIES + "/C2 Reborn");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        Uri shared = null;
        try {
            shared = getContentResolver().insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (shared == null) return null;
            try (java.io.InputStream in = getContentResolver().openInputStream(privateUri);
                 java.io.OutputStream out = getContentResolver().openOutputStream(shared)) {
                if (in == null || out == null) throw new java.io.IOException("Video stream unavailable");
                byte[] buf = new byte[32 * 1024];
                int read;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            }
            android.content.ContentValues ready = new android.content.ContentValues();
            ready.put(MediaStore.Video.Media.IS_PENDING, 0);
            if (getContentResolver().update(shared, ready, null, null) <= 0)
                throw new java.io.IOException("Video publication failed");
            return shared;
        } catch (Exception ex) {
            android.util.Log.e("C2Camera", "Unable to publish video to Movies", ex);
            if (shared != null) try { getContentResolver().delete(shared, null, null); }
            catch (Exception ignored) { }
            return null;
        }
    }

    void captureNativePhoto() {
        if (cameraPreview == null || !cameraPreview.isRunning()) {
            onNativePhotoFailed("Camera preview unavailable");
            return;
        }
        cameraPreview.capturePhoto(this);
    }

    void onNativePhotoFailed(String message) {
        if (phoneView != null) phoneView.onCameraCaptureFailed(message);
    }

    void saveNativePhoto(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 128 || jpeg.length > 24 * 1024 * 1024) {
            onNativePhotoFailed("Camera returned invalid photo");
            return;
        }
        Uri pending = C2MediaProvider.pendingUri(this, false);
        try {
            try (java.io.OutputStream out = getContentResolver().openOutputStream(pending)) {
                if (out == null) throw new java.io.IOException("No photo output stream");
                out.write(jpeg);
            }
            Uri result = C2MediaProvider.complete(this, pending, false, null);
            if (result == null) throw new java.io.IOException("Photo could not be saved");
            Uri shared = publishPhotoToPictures(jpeg);
            // Once published to Gallery, retain only the shared copy.
            // If publication failed, keep the private file for recovery.
            if (shared != null) deleteCapturedPhoto(result);
            showNativeCameraPreview(false);
            if (phoneView != null)
                phoneView.onCameraCaptureCompleted(false, shared != null ? shared : result);
            if (shared == null && phoneView != null)
                phoneView.onCameraCaptureFailed("Photo saved privately; shared Pictures unavailable");
        } catch (Exception ex) {
            android.util.Log.e("C2Camera", "In-house photo save failed", ex);
            C2MediaProvider.discard(this, pending);
            onNativePhotoFailed("Unable to save photo");
        }
    }

    /** Android Q+ scoped-storage photo visible in other Gallery applications. */
    private Uri publishPhotoToPictures(byte[] jpeg) {
        if (Build.VERSION.SDK_INT < 29) return null;
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "C2_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/C2 Reborn");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = null;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new java.io.IOException("MediaStore stream unavailable");
                out.write(jpeg);
            }
            android.content.ContentValues published = new android.content.ContentValues();
            published.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (getContentResolver().update(uri, published, null, null) <= 0)
                throw new java.io.IOException("Photo publication failed");
            return uri;
        } catch (Exception ex) {
            android.util.Log.e("C2Camera", "Unable to publish photo to Pictures", ex);
            if (uri != null) try { getContentResolver().delete(uri, null, null); }
            catch (Exception ignored) { }
            return null;
        }
    }

    void shareCapturedPhoto(Uri uri) {
        if (uri == null) return;
        android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
        send.setType("image/jpeg");
        send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(android.content.Intent.createChooser(send, "Send photo"));
        } catch (Exception ex) {
            if (phoneView != null) phoneView.onCameraCaptureFailed("No sharing app available");
        }
    }

    boolean deleteCapturedPhoto(Uri uri) {
        if (uri == null) return false;
        try { return getContentResolver().delete(uri, null, null) > 0; }
        catch (Exception ex) {
            android.util.Log.e("C2Camera", "Unable to delete photo", ex);
            return false;
        }
    }

    private void layoutCameraPreview() {
        if (phoneView == null || cameraPreview == null) return;
        float scale = Math.min(phoneView.getWidth() / 360f, phoneView.getHeight() / 780f);
        if (scale <= 0f) return;
        float ox = (phoneView.getWidth() - 360f * scale) / 2f;
        float oy = (phoneView.getHeight() - 780f * scale) / 2f;
        float lcd = 214f / 240f;
        int x = Math.round(ox + (73f + 6f * lcd) * scale);
        int y = Math.round(oy + (96f + 45f * lcd) * scale);
        int w = Math.max(1, Math.round(228f * lcd * scale));
        int h = Math.max(1, Math.round(208f * lcd * scale));
        android.widget.FrameLayout.LayoutParams lp =
                new android.widget.FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        cameraPreview.setLayoutParams(lp);
    }

    private void requestPhonePermissions() {
        if (Build.VERSION.SDK_INT < 23) return;
        String[] permissions = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_CALL_LOG
        };
        boolean missing = false;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missing = true;
                break;
            }
        }
        if (missing) requestPermissions(permissions, REQ_PHONE_DATA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_PHONE_DATA && phoneView != null) {
            phoneView.onPermissionsChanged();
            observeSmsWhileVisible();
        }
        if (requestCode == REQ_CAMERA_PREVIEW && results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED && phoneView != null &&
                phoneView.isInCameraViewfinder()) showNativeCameraPreview(true);
        if (requestCode == REQ_VIDEO_MIC && phoneView != null) {
            // The runtime permission sheet may destroy the SurfaceView.
            // Do not attempt recorder.prepare() until the user retries from
            // the restored viewfinder after onResume().
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
                phoneView.onCameraCaptureFailed("Microphone allowed - press Record again");
            else phoneView.onCameraCaptureFailed("Microphone permission is required for video");
        }
        if (requestCode == REQ_MICROPHONE && phoneView != null)
            phoneView.onMicrophonePermissionResult(results.length > 0 &&
                    results[0] == PackageManager.PERMISSION_GRANTED);
    }

    /** Log only fixed capture state in debug builds; never URI or media bytes. */
    private void auditCapture(String action) {
        if ((getApplicationInfo().flags &
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            android.util.Log.i("C2GoToAudit", action);
    }

    /** Request microphone only when the user explicitly selects Record. */
    public boolean ensureRecorderPermission() {
        if (Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            return true;
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICROPHONE);
        return false;
    }

    /** Uses Android's scoped Storage Access Framework. No broad storage permission. */
    public void requestGalleryFiles(boolean video) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(video ? "video/*" : "image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, video ? REQ_PICK_VIDEOS : REQ_PICK_IMAGES);
        } catch (android.content.ActivityNotFoundException ex) {
            if (phoneView != null) phoneView.onGalleryPickerError();
        }
    }

    /**
     * Deliberately delegates acquisition to an installed Android camera app.
     * Uses a read/write URI grant for a temporary private file, not CAMERA
     * permission, global storage permission or a WebView.
     */
    public void launchCameraCapture(boolean video) {
        auditCapture(video ? "Android capture REQUEST VIDEO" : "Android capture REQUEST PHOTO");
        Uri output = C2MediaProvider.pendingUri(this, video);
        Intent intent = new Intent(video
                ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        if (video) intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 120);
        intent.setClipData(ClipData.newRawUri("C2 camera output", output));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            pendingCameraOutput = output;
            pendingCameraVideo = video;
            startActivityForResult(intent, video ? REQ_CAPTURE_VIDEO : REQ_CAPTURE_IMAGE);
            auditCapture(video ? "Android VIDEO intent accepted" : "Android PHOTO intent accepted");
        } catch (Exception ex) {
            auditCapture(video ? "Android VIDEO intent unavailable" : "Android PHOTO intent unavailable");
            C2MediaProvider.discard(this, output);
            pendingCameraOutput = null;
            if (phoneView != null)
                phoneView.onCameraCaptureFailed("No compatible camera available");
        }
    }

    /** Scoped explicit selection. No READ_MEDIA_AUDIO or broad storage permission. */
    public void requestAudioFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_PICK_AUDIO);
        } catch (Exception ex) {
            if (phoneView != null) phoneView.onAudioPickerUnavailable();
        }
    }

    /** Delegate playback to an installed Android video player. No WebView used. */
    public boolean viewGalleryVideo(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if (!"content".equals(uri.getScheme())) return false;
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "video/*");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(view);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 407) {
            if (phoneView != null) phoneView.onDialerRoleResult(hasDialerRole());
            return;
        }
        if (requestCode == REQ_CAPTURE_IMAGE || requestCode == REQ_CAPTURE_VIDEO) {
            auditCapture("Android capture result " +
                    (requestCode == REQ_CAPTURE_VIDEO ? "VIDEO" : "PHOTO") +
                    (resultCode == RESULT_OK ? " OK" : " CANCEL/ERROR"));
            Uri pending = pendingCameraOutput;
            pendingCameraOutput = null;
            boolean video = requestCode == REQ_CAPTURE_VIDEO;
            if (pending == null) {
                if (phoneView != null) phoneView.onCameraCaptureFailed("Camera output unavailable");
                return;
            }
            if (resultCode != RESULT_OK) {
                C2MediaProvider.discard(this, pending);
                if (phoneView != null) phoneView.onCameraCaptureFailed("Capture cancelled");
                return;
            }
            Uri saved = C2MediaProvider.complete(this, pending, video, data);
            if (phoneView != null) {
                if (saved != null) phoneView.onCameraCaptureCompleted(video, saved);
                else phoneView.onCameraCaptureFailed("Camera did not save a usable file");
            }
            return;
        }
        if (requestCode != REQ_PICK_IMAGES && requestCode != REQ_PICK_VIDEOS &&
                requestCode != REQ_PICK_AUDIO) return;
        if (resultCode != RESULT_OK || data == null || phoneView == null) return;
        ArrayList<Uri> chosen = new ArrayList<>();
        ClipData clips = data.getClipData();
        if (clips != null) {
            for (int i = 0; i < clips.getItemCount() && i < 150; i++)
                chosen.add(clips.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            chosen.add(data.getData());
        }
        for (Uri uri : chosen) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
        }
        if (requestCode == REQ_PICK_AUDIO) phoneView.onAudioPicked(chosen);
        else phoneView.onGalleryPicked(requestCode == REQ_PICK_VIDEOS, chosen);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCameraOutput != null)
            outState.putString("pendingCameraOutput", pendingCameraOutput.toString());
        outState.putBoolean("pendingCameraVideo", pendingCameraVideo);
    }

    @Override
    protected void onPause() {
        if (isNativeVideoRecording()) finishNativeVideoCapture();
        if (cameraPreview != null) cameraPreview.stop();
        stopSmsObservation();
        if (phoneView != null) phoneView.onAppPaused();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopSmsObservation();
        if (phoneView != null) phoneView.onAppDestroyed();
        if (foreground == this) foreground = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = this;
        hideSystemUi();
        if (phoneView != null && C2InCallService.hasCall()) phoneView.onTelecomCallUpdated();
        if (phoneView != null) phoneView.onPermissionsChanged();
        observeSmsWhileVisible();
        if (phoneView != null && phoneView.isInCameraViewfinder())
            showNativeCameraPreview(true);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (phoneView != null && C2InCallService.hasCall()) phoneView.onTelecomCallUpdated();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (phoneView == null || !phoneView.handleAndroidBack()) {
            super.onBackPressed();
        }
    }
}
