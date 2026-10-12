package com.moshenuch.c2reborn;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExifOrientationInstrumentedTest {
    @Test
    public void rotatedJpegIsCorrectedOnDevice() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File jpeg = new File(context.getCacheDir(), "exif-rotation-test.jpg");
        Bitmap original = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888);
        original.eraseColor(Color.RED);
        try (FileOutputStream output = new FileOutputStream(jpeg)) {
            assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 95, output));
        }
        ExifInterface exif = new ExifInterface(jpeg.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
        exif.saveAttributes();
        Bitmap sampled = android.graphics.BitmapFactory.decodeFile(jpeg.getAbsolutePath());
        C2PhoneView view = new C2PhoneView(context);
        Method method = C2PhoneView.class.getDeclaredMethod(
                "orientCapturedPhotoPreview", Uri.class, Bitmap.class);
        method.setAccessible(true);
        Bitmap corrected = (Bitmap) method.invoke(view, Uri.fromFile(jpeg), sampled);
        assertNotNull(corrected);
        assertEquals(20, corrected.getWidth());
        assertEquals(40, corrected.getHeight());
        assertEquals(Color.RED, corrected.getPixel(10, 20));
        corrected.recycle();
        original.recycle();
        assertTrue(jpeg.delete());
    }
}
