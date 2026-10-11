package com.moshenuch.c2reborn;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.content.Intent;
import android.os.Build;

/** Telecom owns network calls; C2 owns presentation when chosen as default dialer. */
public final class C2InCallService extends InCallService {
    private static C2InCallService service;
    private static Call current;
    private static final Call.Callback callback = new Call.Callback() {
        @Override public void onStateChanged(Call call, int state) {
            if (state == Call.STATE_DISCONNECTED) {
                current = null;
                MainActivity.notifyCallEnded();
            } else MainActivity.notifyCallUpdated();
        }
    };

    static boolean hangup() {
        if (current == null) return false;
        current.disconnect();
        return true;
    }

    static boolean answer() {
        if (current == null || current.getState() != Call.STATE_RINGING) return false;
        current.answer(VideoProfile.STATE_AUDIO_ONLY);
        return true;
    }
    static boolean isRinging() {
        return current != null && current.getState() == Call.STATE_RINGING;
    }
    static boolean hasCall() { return current != null; }
    static int state() { return current == null ? Call.STATE_DISCONNECTED : current.getState(); }
    static String number() {
        if (current == null || current.getDetails() == null ||
                current.getDetails().getHandle() == null) return "";
        return current.getDetails().getHandle().getSchemeSpecificPart();
    }
    static long connectTime() {
        return current == null || current.getDetails() == null ? 0 :
                current.getDetails().getConnectTimeMillis();
    }
    static void speaker(boolean enabled) {
        if (service != null) service.setAudioRoute(enabled ?
                android.telecom.CallAudioState.ROUTE_SPEAKER :
                android.telecom.CallAudioState.ROUTE_EARPIECE);
    }
    @Override public void onCallAdded(Call call) {
        super.onCallAdded(call);
        service = this;
        if (current != null) current.unregisterCallback(callback);
        current = call;
        call.registerCallback(callback);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("c2_show_call", true);
        startActivity(i);
        MainActivity.notifyCallUpdated();
    }
    @Override public void onCallRemoved(Call call) {
        call.unregisterCallback(callback);
        if (current == call) current = null;
        MainActivity.notifyCallEnded();
        super.onCallRemoved(call);
    }
    @Override public void onDestroy() {
        service = null;
        current = null;
        super.onDestroy();
    }
}
