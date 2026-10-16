package com.moshenuch.c2reborn;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

/**
 * Send-result callbacks for outgoing SMS. The provider row is written here,
 * on the radio's result - SENT when every part reports OK, FAILED otherwise -
 * never at submit time (audit #31 item 4).
 */
public class C2SentResultReceiver extends BroadcastReceiver {
    static final String ACTION_SMS_SENT_RESULT = "com.moshenuch.c2reborn.SMS_SENT_RESULT";
    static final String EXTRA_NUMBER = "number";
    static final String EXTRA_BODY = "body";
    static final String EXTRA_SEND_ID = "sendId";
    static final String EXTRA_PART_INDEX = "partIndex";
    static final String EXTRA_PART_COUNT = "partCount";

    // sendId -> per-part states: 0 pending, 1 ok, -1 failed.
    private static final HashMap<Long, int[]> PART_STATES = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_SMS_SENT_RESULT.equals(intent.getAction())) return;
        String number = intent.getStringExtra(EXTRA_NUMBER);
        String body = intent.getStringExtra(EXTRA_BODY);
        long sendId = intent.getLongExtra(EXTRA_SEND_ID, -1);
        int partIndex = intent.getIntExtra(EXTRA_PART_INDEX, 0);
        int partCount = Math.max(1, intent.getIntExtra(EXTRA_PART_COUNT, 1));
        if (sendId < 0 || body == null) return;
        boolean ok = getResultCode() == Activity.RESULT_OK;
        boolean allIn;
        boolean allOk = true;
        synchronized (PART_STATES) {
            int[] states = PART_STATES.get(sendId);
            if (states == null || states.length != partCount) {
                states = new int[partCount];
                PART_STATES.put(sendId, states);
            }
            if (partIndex >= 0 && partIndex < partCount)
                states[partIndex] = ok ? 1 : -1;
            allIn = true;
            for (int s : states) {
                if (s == 0) allIn = false;
                if (s < 0) allOk = false;
            }
            if (allIn) PART_STATES.remove(sendId);
        }
        if (!allIn) return;
        AndroidBackend backend = new AndroidBackend(context);
        if (allOk) {
            long id = backend.insertSentSms(number, body);
            Log.i("C2Messages", id >= 0 ? "SENT_ROW stored id=" + id
                    : "SENT_ROW skipped reason=not_default");
        } else {
            long id = backend.insertFailedSms(number, body);
            Log.i("C2Messages", id >= 0 ? "SENT_ROW failed stored id=" + id
                    : "SENT_ROW skipped reason=not_default");
        }
    }
}
