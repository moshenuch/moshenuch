package com.moshenuch.c2reborn;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
 */
public final class C2SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null
                || !Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts;
        try {
            parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (RuntimeException ex) {
            Log.e("C2Messages", "SMS_DELIVER unparsable: " + ex.getClass().getSimpleName());
            return;
        }
        if (parts == null || parts.length == 0) return;
        String address = parts[0].getDisplayOriginatingAddress();
        StringBuilder body = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part == null) continue;
            String text = part.getDisplayMessageBody();
            if (text != null) body.append(text);
        }
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, address == null ? "" : address);
        values.put(Telephony.Sms.BODY, body.toString());
        values.put(Telephony.Sms.DATE, System.currentTimeMillis());
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
    }
}
