package com.moshenuch.c2shareprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Separate-package CI-only receiver. Opening the actual C2-granted
 * attachment verifies cross-app read access, not merely a Sharesheet popup.
 * No photo bytes are logged, retained or uploaded by this app.
 */
public final class ShareReceiverActivity extends Activity {
    private static final String TAG = "C2ShareProbe";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String message;
        Intent send = getIntent();
        try {
            if (send == null || !Intent.ACTION_SEND.equals(send.getAction()))
                throw new IllegalArgumentException("Not an ACTION_SEND");
            Uri stream = send.getParcelableExtra(Intent.EXTRA_STREAM);
            ClipData clips = send.getClipData();
            Uri clip = clips != null && clips.getItemCount() > 0
                    ? clips.getItemAt(0).getUri() : null;
            if (stream == null || !stream.equals(clip))
                throw new IllegalArgumentException("EXTRA_STREAM / ClipData mismatch");
            if ((send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0)
                throw new SecurityException("Temporary read permission flag missing");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (InputStream in = getContentResolver().openInputStream(stream)) {
                if (in == null) throw new IllegalArgumentException("Unable to open shared stream");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) >= 0) {
                    if (count == 0) continue;
                    if (data.size() + count > 24 * 1024 * 1024)
                        throw new IllegalArgumentException("Image too large");
                    data.write(buffer, 0, count);
                }
            }
            byte[] content = data.toByteArray();
            if (content.length < 128 ||
                    (content[0] & 255) != 0xff || (content[1] & 255) != 0xd8 ||
                    (content[content.length - 2] & 255) != 0xff ||
                    (content[content.length - 1] & 255) != 0xd9)
                throw new IllegalArgumentException("Recipient did not receive a valid JPEG");
            message = "PASS opened C2 JPEG from separate application (" +
                    content.length + " bytes)";
            Log.i(TAG, message);
        } catch (Exception ex) {
            message = "FAIL receiver unable to read shared JPEG: " +
                    ex.getClass().getSimpleName();
            Log.e(TAG, message, ex);
        }
        TextView result = new TextView(this);
        result.setTextSize(21);
        result.setPadding(30, 60, 30, 30);
        result.setText(message);
        setContentView(result);
    }
}
