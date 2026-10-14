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
        public final long phoneDataId;
        public final String name;
        public final String number;
        /** All phone numbers belonging to this aggregated Android contact. */
        public final ArrayList<String> numbers = new ArrayList<>();
        public final ArrayList<Long> numberRawIds = new ArrayList<>();
        public final ArrayList<Long> numberDataIds = new ArrayList<>();

        void addNumber(String value, long rawId, long dataId) {
            if (value != null && !value.trim().isEmpty() && !numbers.contains(value)) {
                numbers.add(value);
                numberRawIds.add(rawId);
                numberDataIds.add(dataId);
            }
        }

        DeviceContact(long contactId, long rawContactId, long phoneDataId, String name, String number) {
            this.contactId = contactId;
            this.rawContactId = rawContactId;
            this.phoneDataId = phoneDataId;
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
            addNumber(this.number, rawContactId, phoneDataId);
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
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone._ID
        };
        Cursor c = null;
        try {
            c = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC");
            // Merge all phone rows by CONTACT_ID, even if their rows are not adjacent.
            java.util.HashMap<Long, DeviceContact> byId = new java.util.HashMap<>();
            while (c != null && c.moveToNext()) {
                long id = c.getLong(0);
                DeviceContact contact = byId.get(id);
                if (contact == null) {
                    contact = new DeviceContact(id, c.getLong(1), c.getLong(4),
                            c.getString(2), c.getString(3));
                    byId.put(id, contact);
                    out.add(contact);
                } else contact.addNumber(c.getString(3), c.getLong(1), c.getLong(4));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    /** Create the raw contact, name and phone atomically so a failed number
     *  insert cannot leave an orphaned/half-created contact in the device. */
    public boolean addContact(String name, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS) ||
                name == null || name.trim().isEmpty() ||
                number == null || number.trim().isEmpty()) return false;
        try {
            ArrayList<android.content.ContentProviderOperation> changes = new ArrayList<>();
            changes.add(android.content.ContentProviderOperation
                    .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());
            changes.add(android.content.ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim())
                    .build());
            changes.add(android.content.ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number.trim())
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
            android.content.ContentProviderResult[] result =
                    resolver.applyBatch(ContactsContract.AUTHORITY, changes);
            return result != null && result.length == 3 && result[0] != null &&
                    result[0].uri != null && result[1] != null && result[2] != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Rename the selected writable raw contact without unnecessarily updating
     * its unchanged phone-number row. Returns false if nothing was updated. */
    public boolean updateContactName(long rawContactId, String name) {
        if (!has(Manifest.permission.WRITE_CONTACTS) ||
                rawContactId <= 0 || name == null || name.trim().isEmpty()) return false;
        try {
            ContentValues values = new ContentValues();
            values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim());
            int count = resolver.update(ContactsContract.Data.CONTENT_URI, values,
                    ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                    ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{String.valueOf(rawContactId),
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});
            return count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Limit edits to the specifically selected phone-number data row. Contacts can
     * aggregate multiple raw contacts and each can contain multiple numbers.
     */
    public boolean updateContact(long contactId, long rawContactId, long phoneDataId,
                                 String name, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS) || rawContactId <= 0 || phoneDataId <= 0)
            return false;
        Cursor cursor = null;
        try {
            long nameId = -1;
            cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data._ID},
                    ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                            ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{String.valueOf(rawContactId),
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null);
            if (cursor != null && cursor.moveToFirst()) nameId = cursor.getLong(0);
            if (cursor != null) { cursor.close(); cursor = null; }

            ArrayList<android.content.ContentProviderOperation> edits = new ArrayList<>();
            ContentValues phone = new ContentValues();
            phone.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number);
            edits.add(android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withValues(phone)
                    .withSelection(ContactsContract.Data._ID + "=? AND " +
                                    ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                    ContactsContract.Data.MIMETYPE + "=?",
                            new String[]{String.valueOf(phoneDataId), String.valueOf(rawContactId),
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                    .withExpectedCount(1).build());
            ContentValues values = new ContentValues();
            values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
            if (nameId > 0) {
                edits.add(android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValues(values)
                        .withSelection(ContactsContract.Data._ID + "=? AND " +
                                        ContactsContract.Data.RAW_CONTACT_ID + "=?",
                                new String[]{String.valueOf(nameId), String.valueOf(rawContactId)})
                        .withExpectedCount(1).build());
            } else {
                values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
                edits.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValues(values).build());
            }
            resolver.applyBatch(ContactsContract.AUTHORITY, edits);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /** Append an additional phone-number row to an existing writable raw contact. */
    public boolean addNumberToContact(long rawContactId, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS) ||
                rawContactId <= 0 || number == null || number.trim().isEmpty()) return false;
        try {
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, number.trim());
            values.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
            Uri uri = resolver.insert(ContactsContract.Data.CONTENT_URI, values);
            return uri != null;
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
