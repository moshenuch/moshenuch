package com.moshenuch.c2reborn;

import android.content.Context;
import android.graphics.Typeface;

/** Installed source Nokia Sans faces, with Android fallback when unavailable. */
final class NokiaFontFaces {
    private static Typeface normal;
    private static Typeface strong;
    private static boolean attempted;

    static synchronized Typeface get(Context context, boolean bold) {
        if (!attempted) {
            attempted = true;
            try {
                normal = Typeface.createFromAsset(context.getAssets(), "nokia_font/regular.ttf");
                strong = Typeface.createFromAsset(context.getAssets(), "nokia_font/bold.ttf");
            } catch (RuntimeException failure) {
                normal = Typeface.DEFAULT;
                strong = Typeface.DEFAULT_BOLD;
            }
        }
        return bold ? strong : normal;
    }

    private NokiaFontFaces() {}
}
