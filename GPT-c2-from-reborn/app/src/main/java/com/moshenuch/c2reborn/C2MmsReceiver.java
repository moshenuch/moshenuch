package com.moshenuch.c2reborn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Log;

/**
 * MMS delivery endpoint, present because the default-SMS role requires one.
 * MMS is not supported: inbound MMS are logged and NOT persisted, and the
 * release notes say so plainly. Kept deliberately inert.
 */
public final class C2MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Log.w("C2Messages", "WAP_PUSH_DELIVER received; MMS unsupported, message not persisted (action="
                + intent.getAction() + ")");
    }
}
