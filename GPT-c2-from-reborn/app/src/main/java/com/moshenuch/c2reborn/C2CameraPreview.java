package com.moshenuch.c2reborn;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;

/** Live in-house viewfinder surface, not a stock camera Activity.
 * Photo/video capture remains delegated until direct media capture is implemented.
 */
@SuppressWarnings("deprecation")
public final class C2CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera camera;
    private boolean started;
    C2CameraPreview(Context context) {
        super(context);
        getHolder().addCallback(this);
        setKeepScreenOn(true);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { start(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int height) {
        if (camera != null) {
            try { camera.stopPreview(); camera.setPreviewDisplay(h); camera.startPreview(); }
            catch (Exception ex) { Log.w("C2Camera", "Preview surface changed", ex); stop(); }
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { stop(); }
    void start() {
        if (camera != null || getHolder().getSurface() == null ||
                !getHolder().getSurface().isValid()) return;
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
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
        if (camera != null) {
            try { camera.stopPreview(); } catch (RuntimeException ignored) {}
            try { camera.release(); } catch (RuntimeException ignored) {}
            camera = null;
        }
    }
    boolean isRunning() { return started; }
}
