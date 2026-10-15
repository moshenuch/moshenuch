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

    /** Aggregate contact ids carrying data a SIM name+number entry cannot
     *  hold: other MIME rows (email, address, note, photo, groups, ...),
     *  more than one raw contact, or a phone row with a custom type/label
     *  (the label text and is_primary would be lost). Move skips these;
     *  Copy only duplicates name + one number and leaves the original, so
     *  it stays allowed (audit #16/#17).
     *  Returns null when the scan cannot complete (permission gone or a
     *  provider error) so the caller fails CLOSED instead of moving
     *  unscanned contacts (audit #17). */
    public java.util.HashSet<Long> loadContactsWithExtraData() {
        if (!has(Manifest.permission.READ_CONTACTS)) return null;
        java.util.HashSet<Long> out = new java.util.HashSet<>();
        Cursor c = null;
        try {
            c = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data.CONTACT_ID},
                    ContactsContract.Data.MIMETYPE + " NOT IN (?, ?)",
                    new String[]{
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                    null);
            while (c != null && c.moveToNext()) out.add(c.getLong(0));
        } catch (Exception ignored) {
            return null;
        } finally {
            if (c != null) c.close();
        }
        java.util.HashMap<Long, Integer> rawCounts = new java.util.HashMap<>();
        c = null;
        try {
            c = resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.RawContacts.CONTACT_ID,
                            ContactsContract.RawContacts.ACCOUNT_TYPE},
                    ContactsContract.RawContacts.DELETED + "=0", null, null);
            while (c != null && c.moveToNext()) {
                long id = c.getLong(0);
                Integer n = rawCounts.get(id);
                rawCounts.put(id, n == null ? 1 : n + 1);
                // Account-backed (synced) raws: a local delete propagates
                // through sync and moving back would recreate the contact
                // account-less, so Move must skip these (audit #17).
                if (c.getString(1) != null) out.add(id);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (c != null) c.close();
        }
        for (java.util.Map.Entry<Long, Integer> e : rawCounts.entrySet())
            if (e.getValue() != null && e.getValue() > 1) out.add(e.getKey());
        c = null;
        try {
            c = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.LABEL},
                    null, null, null);
            while (c != null && c.moveToNext()) {
                String label = c.getString(2);
                if (c.getInt(1) == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM
                        || (label != null && !label.trim().isEmpty()))
                    out.add(c.getLong(0));
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    /** Create the raw contact, name and phone atomically so a failed number
     *  insert cannot leave an orphaned/half-created contact in the device. */
    public boolean addContact(String name, String number) {
        return addContactReturningRawId(name, number) > 0;
    }

    /** Same insert, returning the new raw contact id (or -1) so a batch can
     *  later undo exactly the row it inserted (audit #16). */
    public long addContactReturningRawId(String name, String number) {
        if (!has(Manifest.permission.WRITE_CONTACTS) ||
                name == null || name.trim().isEmpty() ||
                number == null || number.trim().isEmpty()) return -1;
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
            if (result == null || result.length != 3 || result[0] == null ||
                    result[0].uri == null || result[1] == null || result[2] == null)
                return -1;
            try {
                return android.content.ContentUris.parseId(result[0].uri);
            } catch (Exception ignored) {
                return -1;
            }
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Delete exactly one raw contact by its raw _id - the row a batch op
     *  inserted - never an aggregate-wide delete matched by name (audit #16). */
    public boolean deleteRawContact(long rawContactId) {
        if (!has(Manifest.permission.WRITE_CONTACTS) || rawContactId <= 0) return false;
        try {
            int rows = resolver.delete(
                    ContactsContract.RawContacts.CONTENT_URI,
                    ContactsContract.RawContacts._ID + "=?",
                    new String[]{String.valueOf(rawContactId)});
            return rows > 0;
        } catch (Exception e) {
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

    /** First writable raw contact of an aggregate, skipping read-only and
     *  deleted rows; -1 when the aggregate has nothing writable. An aggregate
     *  can merge raw contacts from read-only accounts, and its first raw id
     *  is not necessarily one this app may write to. */
    public long writableRawContactId(long contactId) {
        if (!has(Manifest.permission.WRITE_CONTACTS) || contactId <= 0) return -1;
        Cursor c = null;
        try {
            c = resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.CONTACT_ID + "=? AND "
                            + ContactsContract.RawContacts.DELETED + "=0 AND "
                            + ContactsContract.RawContacts.RAW_CONTACT_IS_READ_ONLY + "=0",
                    new String[]{String.valueOf(contactId)}, null);
            if (c != null && c.moveToFirst()) return c.getLong(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return -1;
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
