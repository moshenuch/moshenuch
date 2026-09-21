package com.moshenuch.c2reborn;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;

import java.util.ArrayList;

public final class AndroidBackend {
    public static final class DeviceContact {
        public final long contactId;
        public final long rawContactId;
        public final String name;
        public final String number;

        DeviceContact(long contactId, long rawContactId, String name, String number) {
            this.contactId = contactId;
            this.rawContactId = rawContactId;
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
        }
    }

    public static final class DeviceSms {
        public final long id;
        public final long threadId;
        public final String address;
        public final String body;
        public final long date;
        public final int type;
        public final boolean read;

        DeviceSms(long id, long threadId, String address, String body, long date, int type, boolean read) {
            this.id = id;
            this.threadId = threadId;
            this.address = address == null ? "" : address;
            this.body = body == null ? "" : body;
            this.date = date;
            this.type = type;
            this.read = read;
        }
    }

    public static final class DeviceCall {
        public final String number;
        public final String name;
        public final int type;
        public final long date;
        public final long duration;

        DeviceCall(String number, String name, int type, long date, long duration) {
            this.number = number == null ? "" : number;
            this.name = name == null ? "" : name;
            this.type = type;
            this.date = date;
            this.duration = duration;
        }
    }

    private final Context context;
    private final ContentResolver resolver;

    public AndroidBackend(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();
    }

    public boolean has(String permission) {
        return Build.VERSION.SDK_INT < 23 ||
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public ArrayList<DeviceContact> loadContacts() {
        ArrayList<DeviceContact> out = new ArrayList<>();
        if (!has(Manifest.permission.READ_CONTACTS)) return out;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        Cursor c = null;
        try {
            c = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC");
            long lastContact = -1;
            while (c != null && c.moveToNext()) {
                long contactId = c.getLong(0);
                if (contactId == lastContact) continue;
                lastContact = contactId;
                out.add(new DeviceContact(contactId, c.getLong(1), c.getString(2), c.getString(3)));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public boolean addContact(String name, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS)) return false;
        try {
            ContentValues raw = new ContentValues();
            Uri rawUri = resolver.insert(ContactsContract.RawContacts.CONTENT_URI, raw);
            if (rawUri == null) return false;
            long rawId = ContentUris.parseId(rawUri);

            ContentValues n = new ContentValues();
            n.put(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            n.put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            n.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
            resolver.insert(ContactsContract.Data.CONTENT_URI, n);

            ContentValues p = new ContentValues();
            p.put(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            p.put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            p.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number);
            p.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
            resolver.insert(ContactsContract.Data.CONTENT_URI, p);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateContact(long contactId, long rawContactId, String name, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS)) return false;
        try {
            ContentValues n = new ContentValues();
            n.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
            int nameRows = resolver.update(
                    ContactsContract.Data.CONTENT_URI,
                    n,
                    ContactsContract.Data.CONTACT_ID + "=? AND " +
                            ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{String.valueOf(contactId),
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});

            ContentValues p = new ContentValues();
            p.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number);
            int phoneRows = resolver.update(
                    ContactsContract.Data.CONTENT_URI,
                    p,
                    ContactsContract.Data.CONTACT_ID + "=? AND " +
                            ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{String.valueOf(contactId),
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE});

            if (nameRows == 0 && rawContactId > 0) {
                ContentValues addName = new ContentValues();
                addName.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                addName.put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                addName.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
                resolver.insert(ContactsContract.Data.CONTENT_URI, addName);
            }
            if (phoneRows == 0 && rawContactId > 0) {
                ContentValues addPhone = new ContentValues();
                addPhone.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                addPhone.put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                addPhone.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number);
                addPhone.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                resolver.insert(ContactsContract.Data.CONTENT_URI, addPhone);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteContact(long contactId) {
        if (!has(Manifest.permission.WRITE_CONTACTS)) return false;
        try {
            int rows = resolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    ContactsContract.RawContacts.CONTACT_ID + "=?",
                    new String[]{String.valueOf(contactId)});
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public ArrayList<DeviceSms> loadSms(int max) {
        ArrayList<DeviceSms> out = new ArrayList<>();
        if (!has(Manifest.permission.READ_SMS)) return out;
        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
        };
        Cursor c = null;
        try {
            c = resolver.query(Telephony.Sms.CONTENT_URI, projection, null, null,
                    Telephony.Sms.DATE + " DESC");
            while (c != null && c.moveToNext() && out.size() < max) {
                out.add(new DeviceSms(
                        c.getLong(0), c.getLong(1), c.getString(2),
                        c.getString(3), c.getLong(4), c.getInt(5), c.getInt(6) != 0));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public boolean sendSms(String number, String body) {
        if (!has(Manifest.permission.SEND_SMS)) return false;
        try {
            SmsManager sm = SmsManager.getDefault();
            ArrayList<String> parts = sm.divideMessage(body);
            if (parts.size() <= 1) sm.sendTextMessage(number, null, body, null, null);
            else sm.sendMultipartTextMessage(number, null, parts, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean placeCall(String number) {
        try {
            Uri uri = Uri.parse("tel:" + Uri.encode(number));
            Intent intent;
            if (has(Manifest.permission.CALL_PHONE)) {
                intent = new Intent(Intent.ACTION_CALL, uri);
            } else {
                intent = new Intent(Intent.ACTION_DIAL, uri);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean endCall() {
        if (Build.VERSION.SDK_INT < 28 || !has(Manifest.permission.ANSWER_PHONE_CALLS)) return false;
        try {
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            return telecom != null && telecom.endCall();
        } catch (Exception e) {
            return false;
        }
    }

    public void setSpeakerphone(boolean on) {
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) audio.setSpeakerphoneOn(on);
        } catch (Exception ignored) {}
    }

    public ArrayList<DeviceCall> loadCalls(int max) {
        ArrayList<DeviceCall> out = new ArrayList<>();
        if (!has(Manifest.permission.READ_CALL_LOG)) return out;
        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };
        Cursor c = null;
        try {
            c = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null,
                    CallLog.Calls.DATE + " DESC");
            while (c != null && c.moveToNext() && out.size() < max) {
                out.add(new DeviceCall(
                        c.getString(0), c.getString(1), c.getInt(2),
                        c.getLong(3), c.getLong(4)));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }
}
