package com.moshenuch.c2reborn;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import java.io.File;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;

/** Embedded camera and video recording. No external capture activity or WebView. */
@SuppressWarnings("deprecation")
public final class C2CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera camera;
    private int cameraId = -1;
    private int sensorOrientation = 90;
    private int jpegOrientation = 90;
    private MediaRecorder recorder;
    private volatile boolean recording;
    // Remains true until the main thread releases the camera after the
    // worker has stopped and released MediaRecorder.
    private volatile boolean stopping;
    private volatile String lastStopFailure = "";
    private volatile StackTraceElement[] lastStopExceptionStack;
    private boolean started;
    C2CameraPreview(Context context) {
        super(context);
        getHolder().addCallback(this);
        setKeepScreenOn(true);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { start(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int height) {
        // MediaRecorder.stop() runs on a worker. During finalisation recording
        // becomes false before the recorder has released the camera.
        // Never restart preview until that worker has finished.
        if (camera != null && started && !recording && !stopping) {
            try { camera.stopPreview(); camera.setPreviewDisplay(h); camera.startPreview(); }
            catch (Exception ex) { Log.w("C2Camera", "Preview surface changed", ex); stop(); }
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        // Do not call MediaRecorder.stop() from a Surface callback: it may
        // block the UI thread and cause an ANR on emulators and real phones.
        if (recording && getContext() instanceof MainActivity)
            ((MainActivity) getContext()).finishNativeVideoCapture();
        if (!recording && !stopping) stop();
    }
    void start() {
        if (camera != null || recording || stopping ||
                getHolder().getSurface() == null ||
                !getHolder().getSurface().isValid()) return;
        try {
            cameraId = 0;
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int id = 0; id < Camera.getNumberOfCameras(); id++) {
                Camera.getCameraInfo(id, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            Camera.getCameraInfo(cameraId, info);
            camera = Camera.open(cameraId);
            sensorOrientation = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                    ? (360 - info.orientation) % 360 : info.orientation;
            // Preview orientation changes ONLY the SurfaceView, not JPEG bytes.
            // Capture orientation must also be supplied to Camera.Parameters.
            jpegOrientation = info.orientation;
            Camera.Parameters photoParameters = camera.getParameters();
            photoParameters.setRotation(jpegOrientation);
            camera.setParameters(photoParameters);
            camera.setDisplayOrientation(sensorOrientation);
            camera.setPreviewDisplay(getHolder());
            camera.startPreview();
            started = true;
        } catch (Exception ex) {
            Log.w("C2Camera", "Unable to start native preview", ex);
            stop();
        }
    }
    void stop() {
        // Callers including Activity.onPause and SurfaceHolder callbacks
        // must not synchronously stop the encoder on Android's UI thread.
        if (recording || stopping) {
            if (recording && getContext() instanceof MainActivity)
                ((MainActivity) getContext()).finishNativeVideoCapture();
            return;
        }
        started = false;
        if (recorder != null) {
            try { recorder.release(); } catch (RuntimeException ignored) { }
            recorder = null;
        }
        if (camera != null) {
            try { camera.stopPreview(); } catch (RuntimeException ignored) {}
            try { camera.release(); } catch (RuntimeException ignored) {}
            camera = null;
        }
        cameraId = -1;
    }
    /** Record camera video and microphone audio into a private temporary MP4. */
    boolean startRecording(File output) {
        if (recording || stopping || camera == null || !started || output == null) return false;
        try {
            // MediaRecorder may wait indefinitely for camera frames if its
            // encoded frame size does not match the active Camera1 preview.
            // Configure that matching preview BEFORE unlocking the camera.
            if (!CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW))
                throw new IllegalStateException("No supported video capture profile");
            CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
            Camera.Parameters params = camera.getParameters();
            boolean hasExactPreviewSize = false;
            java.util.List<Camera.Size> supported = params.getSupportedPreviewSizes();
            if (supported != null) {
                for (Camera.Size size : supported) {
                    if (size.width == profile.videoFrameWidth &&
                            size.height == profile.videoFrameHeight) {
                        hasExactPreviewSize = true;
                        break;
                    }
                }
            }
            if (hasExactPreviewSize) {
                camera.stopPreview();
                params.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
                params.setRecordingHint(true);
                camera.setParameters(params);
                camera.setPreviewDisplay(getHolder());
                camera.startPreview();
                Log.i("C2Camera", "VIDEO_PREVIEW matched " +
                        profile.videoFrameWidth + "x" + profile.videoFrameHeight);
            } else {
                Log.w("C2Camera", "VIDEO_PREVIEW unsupported " +
                        profile.videoFrameWidth + "x" + profile.videoFrameHeight +
                        "; active preview may differ");
            }
            camera.unlock();
            recorder = new MediaRecorder();
            recorder.setCamera(camera);
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // A low-quality profile may select 3GPP. Explicitly encode an MP4
            // to match the .mp4 filename and Android Gallery MIME type.
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
            recorder.setVideoFrameRate(profile.videoFrameRate);
            recorder.setVideoEncodingBitRate(profile.videoBitRate);
            recorder.setVideoEncoder(profile.videoCodec);
            recorder.setAudioEncodingBitRate(profile.audioBitRate);
            recorder.setAudioSamplingRate(profile.audioSampleRate);
            recorder.setAudioChannels(profile.audioChannels);
            recorder.setAudioEncoder(profile.audioCodec);
            recorder.setOrientationHint(sensorOrientation);
            recorder.setMaxDuration(120_000);
            // Match the private-media validation limit, avoiding unbounded writes.
            try {
                recorder.setMaxFileSize(150L * 1024L * 1024L);
            } catch (RuntimeException unsupported) {
                Log.w("C2Camera", "Recorder file-size limit unsupported", unsupported);
            }
            recorder.setOnInfoListener((mr, what, extra) -> {
                if ((what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) &&
                        getContext() instanceof MainActivity) {
                    MainActivity host = (MainActivity) getContext();
                    host.runOnUiThread(() -> {
                        if (recording) host.finishNativeVideoCapture();
                    });
                }
            });
            recorder.setOutputFile(output.getAbsolutePath());
            // Camera.setPreviewDisplay(getHolder()) already owns this surface.
            // Replacing it in MediaRecorder can interrupt incoming frames.
            recorder.prepare();
            recorder.start();
            recording = true;
            return true;
        } catch (Exception ex) {
            Log.e("C2Camera", "In-house video start failed", ex);
            if (recorder != null) {
                try { recorder.reset(); } catch (RuntimeException ignored) { }
                try { recorder.release(); } catch (RuntimeException ignored) { }
                recorder = null;
            }
            try { camera.lock(); } catch (RuntimeException ignored) { }
            stop();
            return false;
        }
    }

    /** Call ONLY on the video finaliser worker, never the main/UI thread. */
    boolean stopRecording() {
        if (!recording || stopping) return false;
        stopping = true;
        recording = false;
        lastStopFailure = "";
        lastStopExceptionStack = null;
        MediaRecorder active = recorder;
        boolean success = false;
        try {
            if (active != null) {
                active.stop();
                success = true;
            }
        } catch (RuntimeException ex) {
            lastStopFailure = ex.getClass().getSimpleName();
            lastStopExceptionStack = ex.getStackTrace();
            // Includes the complete MediaRecorder.stop Java exception/stack
            // in Appetize or an Oppo adb logcat capture.
            Log.e("C2Camera", "VIDEO_STOP_THROW " +
                    ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } finally {
            if (active != null) {
                try { active.reset(); } catch (RuntimeException ignored) { }
                try { active.release(); } catch (RuntimeException ignored) { }
            }
            recorder = null;
            // The Camera object was created on the UI thread. Do not lock
            // or release it here; finishVideoCamera() runs on the UI thread.
        }
        return success;
    }

    /** Call on the UI thread AFTER the recorder worker has returned. */
    void finishVideoCamera() {
        if (!stopping) return;
        if (camera != null) {
            try { camera.lock(); } catch (RuntimeException ignored) { }
        }
        stopping = false;
        stop();
    }

    String lastStopFailure() { return lastStopFailure; }

    StackTraceElement[] lastStopExceptionStack() {
        StackTraceElement[] frames = lastStopExceptionStack;
        return frames == null ? null : frames.clone();
    }

    boolean isStopping() { return stopping; }

    boolean isRecording() { return recording; }

    void capturePhoto(MainActivity host) {
        if (camera == null || !started) {
            host.onNativePhotoFailed("Camera preview unavailable");
            return;
        }
        started = false;
        try {
            camera.takePicture(null, null, (bytes, source) -> {
                stop();
                host.runOnUiThread(() -> host.saveNativePhoto(bytes));
            });
        } catch (RuntimeException ex) {
            Log.e("C2Camera", "In-house photo capture failed", ex);
            stop();
            host.onNativePhotoFailed("Photo capture failed");
        }
    }

    boolean isRunning() { return started; }
}
