package com.moshenuch.c2reborn;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Quick-response send endpoint, present only because the default-SMS role
 * requires the component. Inert: binds nothing and sends nothing.
 */
public final class C2HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
