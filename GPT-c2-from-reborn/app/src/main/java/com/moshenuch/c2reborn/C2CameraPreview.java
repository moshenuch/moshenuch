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
    private MediaRecorder recorder;
    private boolean recording;
    private boolean started;
    C2CameraPreview(Context context) {
        super(context);
        getHolder().addCallback(this);
        setKeepScreenOn(true);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { start(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int height) {
        if (camera != null && !recording) {
            try { camera.stopPreview(); camera.setPreviewDisplay(h); camera.startPreview(); }
            catch (Exception ex) { Log.w("C2Camera", "Preview surface changed", ex); stop(); }
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording && getContext() instanceof MainActivity)
            ((MainActivity) getContext()).finishNativeVideoCapture();
        stop();
    }
    void start() {
        if (camera != null || getHolder().getSurface() == null ||
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
        started = false;
        if (recording) stopRecording();
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
        if (recording || camera == null || !started || output == null) return false;
        try {
            camera.unlock();
            recorder = new MediaRecorder();
            recorder.setCamera(camera);
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // Match the selected camera's hardware-supported capture profile.
            if (!CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW))
                throw new IllegalStateException("No supported video capture profile");
            CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
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
            recorder.setOutputFile(output.getAbsolutePath());
            recorder.setPreviewDisplay(getHolder().getSurface());
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

    /** Recorder.stop() can fail if recording was too short; caller discards invalid MP4. */
    boolean stopRecording() {
        if (!recording) return false;
        recording = false;
        boolean success = false;
        try {
            recorder.stop();
            success = true;
        } catch (RuntimeException ex) {
            Log.w("C2Camera", "Video was not finalised", ex);
        } finally {
            if (recorder != null) {
                try { recorder.reset(); } catch (RuntimeException ignored) { }
                try { recorder.release(); } catch (RuntimeException ignored) { }
                recorder = null;
            }
            if (camera != null) {
                try { camera.lock(); } catch (RuntimeException ignored) { }
            }
        }
        return success;
    }

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
