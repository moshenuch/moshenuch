package com.moshenuch.c2reborn;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Inbound SMS delivery while C2 holds the default SMS role.
 *
 * The system delivers every inbound SMS to this receiver and to no one
 * else: while C2 is default, nothing persists a message unless this
 * receiver writes it to the provider, so each delivery is stored here.
 * Runs whether or not the app UI is open; the UI picks the row up through
 * its existing provider observer.
 *
 * Every inbound payload is treated as untrusted: null or empty part
 * arrays, null parts, and missing senders or bodies are logged and
 * skipped, and nothing in onReceive may throw. A redelivery (same sender,
 * sent timestamp and body) is stored only once.
 */
public final class C2SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (context == null || intent == null
                    || !Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
            SmsMessage[] parts;
            try {
                parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            } catch (RuntimeException ex) {
                Log.e("C2Messages", "SMS_DELIVER skipped reason=unparsable");
                return;
            }
            if (parts == null || parts.length == 0) {
                Log.i("C2Messages", "SMS_DELIVER skipped reason=no_parts");
                return;
            }
            String address = null;
            long timestamp = 0;
            StringBuilder body = new StringBuilder();
            for (SmsMessage part : parts) {
                if (part == null) {
                    // A missing segment would store a truncated text; drop
                    // the whole delivery instead.
                    Log.i("C2Messages", "SMS_DELIVER skipped reason=partial parts=" + parts.length);
                    return;
                }
                if (address == null) address = part.getDisplayOriginatingAddress();
                if (timestamp == 0) timestamp = part.getTimestampMillis();
                String text = part.getDisplayMessageBody();
                if (text != null) body.append(text);
            }
            if (address == null || address.length() == 0 || body.length() == 0) {
                Log.i("C2Messages", "SMS_DELIVER skipped reason=empty parts=" + parts.length);
                return;
            }
            if (timestamp == 0) timestamp = System.currentTimeMillis();
            String text = body.toString();
            // A true redelivery arrives immediately, so dedupe only applies
            // to a message whose sent timestamp is seconds old. Residual:
            // a genuinely distinct identical text sent by the same sender
            // inside the same second is stored once (documented).
            boolean fresh = Math.abs(System.currentTimeMillis() - timestamp) <= 30_000L;
            if (fresh && alreadyStored(context, address, timestamp, text)) {
                Log.i("C2Messages", "SMS_DELIVER skipped reason=duplicate addr=" + address);
                return;
            }
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.BODY, text);
            values.put(Telephony.Sms.DATE, timestamp);
            values.put(Telephony.Sms.READ, 0);
            values.put(Telephony.Sms.SEEN, 0);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            try {
                Uri row = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
                Log.i("C2Messages", "SMS_DELIVER stored parts=" + parts.length
                        + " id=" + (row == null ? "none" : row.getLastPathSegment()));
            } catch (RuntimeException ex) {
                Log.e("C2Messages", "SMS_DELIVER store failed: " + ex.getClass().getSimpleName());
            }
        } catch (RuntimeException ex) {
            Log.e("C2Messages", "SMS_DELIVER skipped reason=unexpected");
        }
    }

    /** True when the identical message (sender + sent timestamp + body)
     *  already has a provider row; an unreadable provider stores anyway,
     *  since a duplicate is recoverable and a lost message is not. */
    private static boolean alreadyStored(Context context, String address, long timestamp, String body) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                    new String[]{Telephony.Sms._ID},
                    Telephony.Sms.ADDRESS + "=? AND " + Telephony.Sms.DATE + "=? AND "
                            + Telephony.Sms.BODY + "=? AND " + Telephony.Sms.TYPE + "="
                            + Telephony.Sms.MESSAGE_TYPE_INBOX,
                    new String[]{address, String.valueOf(timestamp), body}, null);
            return c != null && c.moveToFirst();
        } catch (RuntimeException ex) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }
}
