package com.moshenuch.c2reborn;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Send-result callbacks for outgoing SMS. The provider row is written here,
 * on the radio's result - SENT when every part reports OK, FAILED otherwise -
 * never at submit time (audit #31 item 4).
 *
 * Hardened (audit #32 item 1): the whole entry point is guarded, extras are
 * validated and partCount clamped before allocation, and per-send aggregates
 * are persisted so a process death between parts still completes. A result
 * arriving after completion writes no second row (short persisted
 * completed-sendId list). An aggregate with no full result after 10 minutes
 * expires into a FAILED row - never left silent: the submit-time notice
 * already said delivery is unconfirmed, and the failed row in Sent items
 * closes it.
 */
public class C2SentResultReceiver extends BroadcastReceiver {
    static final String ACTION_SMS_SENT_RESULT = "com.moshenuch.c2reborn.SMS_SENT_RESULT";
    static final String EXTRA_NUMBER = "number";
    static final String EXTRA_BODY = "body";
    static final String EXTRA_SEND_ID = "sendId";
    static final String EXTRA_PART_INDEX = "partIndex";
    static final String EXTRA_PART_COUNT = "partCount";

    private static final String PREFS = "c2_sent_results";
    private static final String KEY_COMPLETED = "completed";
    private static final long EXPIRY_MS = 10 * 60 * 1000;
    private static final int MAX_PARTS = 50;
    private static final int MAX_COMPLETED = 256;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            sweepExpired(context);
            if (intent == null || !ACTION_SMS_SENT_RESULT.equals(intent.getAction())) return;
            String number = intent.getStringExtra(EXTRA_NUMBER);
            String body = intent.getStringExtra(EXTRA_BODY);
            long sendId = intent.getLongExtra(EXTRA_SEND_ID, -1);
            int partIndex = intent.getIntExtra(EXTRA_PART_INDEX, -1);
            int partCount = intent.getIntExtra(EXTRA_PART_COUNT, 1);
            if (partCount < 1) partCount = 1;
            if (partCount > MAX_PARTS) partCount = MAX_PARTS;
            if (sendId < 0 || body == null || number == null
                    || partIndex < 0 || partIndex >= partCount) {
                Log.i("C2Messages", "SENT_ROW skipped reason=bad_extras");
                return;
            }
            boolean ok = getResultCode() == Activity.RESULT_OK;
            onPartResult(context, sendId, number, body, partIndex, partCount, ok);
        } catch (RuntimeException e) {
            Log.e("C2Messages", "SENT_ROW receiver error: " + e);
        }
    }

    private static void onPartResult(Context context, long sendId, String number,
            String body, int partIndex, int partCount, boolean ok) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> completed = new HashSet<>(
                prefs.getStringSet(KEY_COMPLETED, new HashSet<String>()));
        if (completed.contains(String.valueOf(sendId))) {
            Log.i("C2Messages", "SENT_ROW duplicate skipped sendId=" + sendId);
            return;
        }
        Aggregate agg = Aggregate.load(prefs, sendId);
        if (agg == null || agg.partCount != partCount) {
            agg = new Aggregate(sendId, number, body, partCount);
        }
        agg.states[partIndex] = ok ? 1 : -1;
        boolean allIn = true, allOk = true;
        for (int s : agg.states) {
            if (s == 0) allIn = false;
            if (s < 0) allOk = false;
        }
        if (!allIn) {
            agg.save(prefs);
            return;
        }
        finish(context, prefs, agg, allOk, completed);
    }

    private static void finish(Context context, SharedPreferences prefs, Aggregate agg,
            boolean allOk, Set<String> completed) {
        AndroidBackend backend = new AndroidBackend(context);
        if (allOk) {
            long id = backend.insertSentSms(agg.number, agg.body);
            Log.i("C2Messages", id >= 0 ? "SENT_ROW stored id=" + id
                    : "SENT_ROW skipped reason=not_default");
        } else {
            long id = backend.insertFailedSms(agg.number, agg.body);
            Log.i("C2Messages", id >= 0 ? "SENT_ROW failed stored id=" + id
                    : "SENT_ROW skipped reason=not_default");
        }
        agg.clear(prefs);
        rememberCompleted(prefs, completed, agg.sendId);
    }

    /** Expire aggregates with no full result after 10 minutes into a FAILED
     *  row (audit #32 item 1: write FAILED, never leave silent). Runs on
     *  every result callback and on app start. */
    static void sweepExpired(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            Set<String> completed = new HashSet<>(
                    prefs.getStringSet(KEY_COMPLETED, new HashSet<String>()));
            for (String key : new ArrayList<>(prefs.getAll().keySet())) {
                if (!key.startsWith("agg_")) continue;
                Aggregate agg = Aggregate.load(prefs, parseId(key));
                if (agg == null) {
                    prefs.edit().remove(key).apply();
                    continue;
                }
                if (now - agg.createdAt <= EXPIRY_MS) continue;
                AndroidBackend backend = new AndroidBackend(context);
                long id = backend.insertFailedSms(agg.number, agg.body);
                Log.i("C2Messages", id >= 0
                        ? "SENT_ROW expired failed stored id=" + id
                        : "SENT_ROW skipped reason=not_default");
                agg.clear(prefs);
                rememberCompleted(prefs, completed, agg.sendId);
            }
        } catch (RuntimeException e) {
            Log.e("C2Messages", "SENT_ROW sweep error: " + e);
        }
    }

    private static long parseId(String key) {
        try { return Long.parseLong(key.substring(4)); }
        catch (RuntimeException e) { return -1; }
    }

    private static void rememberCompleted(SharedPreferences prefs, Set<String> completed,
            long sendId) {
        completed.add(String.valueOf(sendId));
        while (completed.size() > MAX_COMPLETED) {
            completed.remove(completed.iterator().next());
        }
        prefs.edit().putStringSet(KEY_COMPLETED, completed).apply();
    }

    private static class Aggregate {
        final long sendId;
        final String number;
        final String body;
        final int partCount;
        final long createdAt;
        final int[] states;

        Aggregate(long sendId, String number, String body, int partCount) {
            this(sendId, number, body, partCount, System.currentTimeMillis(),
                    new int[partCount]);
        }
        private Aggregate(long sendId, String number, String body, int partCount,
                long createdAt, int[] states) {
            this.sendId = sendId;
            this.number = number;
            this.body = body;
            this.partCount = partCount;
            this.createdAt = createdAt;
            this.states = states;
        }

        void save(SharedPreferences prefs) {
            StringBuilder sb = new StringBuilder();
            sb.append(createdAt).append('|').append(partCount).append('|')
                    .append(Base64.encodeToString(number.getBytes(), Base64.NO_WRAP)).append('|')
                    .append(Base64.encodeToString(body.getBytes(), Base64.NO_WRAP));
            for (int s : states) sb.append('|').append(s);
            prefs.edit().putString("agg_" + sendId, sb.toString()).apply();
        }

        void clear(SharedPreferences prefs) {
            prefs.edit().remove("agg_" + sendId).apply();
        }

        static Aggregate load(SharedPreferences prefs, long sendId) {
            if (sendId < 0) return null;
            String raw = prefs.getString("agg_" + sendId, null);
            if (raw == null) return null;
            try {
                String[] f = raw.split("\\|", -1);
                long createdAt = Long.parseLong(f[0]);
                int partCount = Integer.parseInt(f[1]);
                if (partCount < 1 || partCount > MAX_PARTS) return null;
                String number = new String(Base64.decode(f[2], Base64.NO_WRAP));
                String body = new String(Base64.decode(f[3], Base64.NO_WRAP));
                Aggregate agg = new Aggregate(sendId, number, body, partCount, createdAt,
                        new int[partCount]);
                for (int i = 0; i < partCount && i + 4 < f.length; i++)
                    agg.states[i] = Integer.parseInt(f[i + 4]);
                return agg;
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
}
