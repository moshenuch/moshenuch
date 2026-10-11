package com.moshenuch.c2reborn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.HashMap;

/** Firmware PNG assets extracted from pinned HTML v6.342 upstream icon pool. */
final class RebornSelectionIcons {
    private static final HashMap<String, Bitmap> CACHE = new HashMap<>();

    static synchronized Bitmap get(Context context, String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        Bitmap image = null;
        if ("CHECK_OFF".equals(name) || "CHECK_ON".equals(name)
                || "RADIO_OFF".equals(name) || "RADIO_ON".equals(name)) {
            try (InputStream input = context.getAssets().open("nokia_fw/" + name + ".png")) {
                image = BitmapFactory.decodeStream(input);
            } catch (Exception ignored) { }
        }
        CACHE.put(name, image);
        return image;
    }

    private RebornSelectionIcons() { }
}
