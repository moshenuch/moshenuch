package com.moshenuch.c2reborn;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.Telephony;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class C2PhoneView extends View {
    private static final float DW = 360f;
    private static final float DH = 780f;
    private static final float SX = 73f;
    private static final float SY = 96f;
    private static final float SW = 214f;
    private static final float SH = 286f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Typeface fontRegular;
    private final android.graphics.Typeface fontBold;
    private final Bitmap wallpaper;
    private final Bitmap[] menuIcons = new Bitmap[9];
    private final HashMap<String, Bitmap> listIcons = new HashMap<>();
    private final SharedPreferences prefs;
    private final AndroidBackend backend;
    private final NativeGalleryStore galleryStore;
    private final NativeVoiceRecorder voiceRecorder;
    private final NativeMusicPlayer musicEngine;
    private Bitmap galleryPreviewBitmap;
    private String galleryPreviewTitle = "";
    private String galleryPreviewUri = "";
    private int galleryPreviewIndex = -1;
    private boolean gallerySortVideos = false;
    private final Deque<PageState> history = new ArrayDeque<>();

    private float fitScale = 1f, offX = 0f, offY = 0f;
    private String page = "home";
    private int sel = 0;
    private String notice = "";
    private long noticeUntil = 0L;

    private String profile = "General";
    private String theme = "Dark";
    private boolean bluetooth = false;
    private String alarmTime = "07:00";
    private boolean alarmOn = false;
    private boolean alarmRepeat = false;
    // Reborn HTML 6.342 call-filter controls are demo only: no interception of real Android calls.
    private boolean blacklistEnabled = false;
    private boolean whitelistEnabled = false;
    private final ArrayList<String> blacklistNumbers = new ArrayList<>();
    private final ArrayList<String> whitelistNumbers = new ArrayList<>();

    private final ArrayList<Contact> contacts = new ArrayList<>();
    // Functional Nokia multi-tap search, with an optional numeric mode.
    private String contactSearchQuery = "";
    private boolean contactSearchNumeric = false;
    private int contactSearchLastKey = -1, contactSearchTapCycle = 0;
    private long contactSearchLastAt = 0L;
    // Android contact IDs map to all their numbers; one phonebook row per ID.
    private final HashMap<Long, ArrayList<String>> contactNumbersById = new HashMap<>();
    private final HashMap<Long, ArrayList<Long>> contactNumberRawIds = new HashMap<>();
    private final HashMap<Long, ArrayList<Long>> contactNumberDataIds = new HashMap<>();
    private long editTargetRawId = -1L, editTargetDataId = -1L;
    private String editTargetNumber = "";
    private final NativeContactGroups contactGroups;
    private String activeContactGroupId = "";
    private String pendingGroupMemberNumber = "";
    private String groupConfirmId = "";
    private String groupConfirmNumber = "";
    private final ArrayList<AndroidBackend.DeviceSms> deviceSms = new ArrayList<>();
    private final ArrayList<AndroidBackend.DeviceSms> smsInbox = new ArrayList<>();
    // Fictional emulator data, strictly confined to debuggable builds.
    private boolean demoMode = false;
    private final ArrayList<NativeSmsDetails.Entry> smsExtractedDetails = new ArrayList<>();
    private final ArrayList<SmsThread> smsThreads = new ArrayList<>();
    private final ArrayList<AndroidBackend.DeviceCall> deviceCalls = new ArrayList<>();
    private final HashSet<Long> localReadSmsIds = new HashSet<>();
    // Persistent local overrides; the Android SMS provider is not changed.
    private final HashSet<Long> localUnreadSmsIds = new HashSet<>();
    private final HashSet<Long> hiddenSmsIds = new HashSet<>();
    private final HashSet<Long> markedThreadIds = new HashSet<>();
    private final HashSet<Long> markedSmsIds = new HashSet<>();
    private final HashSet<Integer> markedContactIndices = new HashSet<>();
    // Nokia speed dials: app-local slot 2..9 to phone-number mapping only.
    private final HashMap<Integer,String> speedDialNumbers = new HashMap<>();
    private int speedDialSlot = 2;
    private String speedDialAssignment = "";
    private boolean speedDiallingEnabled = false;
    private static final long SPEED_DIAL_HOLD_MS = 700L;
    private static final float KEYPAD_DRAG_TOLERANCE = 12f;
    private int touchPointerId = -1;
    private float touchStartX, touchStartY;
    private String touchPressedKey;
    private boolean touchLongConsumed = false;
    private Runnable pendingSpeedDialHold;
    // Call records are hidden within this app; the Android call log stays untouched.
    private final HashSet<String> hiddenCallKeys = new HashSet<>();
    private final HashSet<String> markedCallKeys = new HashSet<>();
    private long activeThreadId = -1L;
    private long activeMessageId = -1L;
    private float conversationScrollY = 0f;
    private boolean deviceContactsActive = false;
    private boolean deviceSmsActive = false;
    private boolean deviceCallsActive = false;
    private final ArrayList<String> notes = new ArrayList<>();
    private final ArrayList<String> todos = new ArrayList<>();
    private final ArrayList<String> calendar = new ArrayList<>();
    private final ArrayList<DraftMessage> drafts = new ArrayList<>();
    // Copied SMS templates are app-local text, never cloned provider SMS.
    private final ArrayList<String> savedSmsTemplates = new ArrayList<>();
    private int editingDraftIndex = -1;

    private String dial = "";
    private String activeCall = "";
    private long callStarted = 0L;
    private boolean loudspeaker = false;

    private String inputMode = "T9";
    private String recipient = "";
    private String messageBody = "";
    private int composeFocus = 0;
    private int lastTapKey = -1;
    private int tapIndex = 0;
    private long lastTapAt = 0L;
    private String t9Digits = "";
    private int t9Start = 0;
    private int t9Choice = 0;

    private String editBuffer = "";
    private String editKind = "";
    private int editIndex = -1;
    private int editField = 0;

    private String calc = "0";
    private Double calcStored = null;
    private char calcOp = 0;

    private boolean stopwatchRunning = false;
    private long stopwatchStarted = 0L;
    private long stopwatchAccum = 0L;
    private final ArrayList<String> stopwatchSplits = new ArrayList<>();

    private String countdownDigits = "";
    private long countdownEnd = 0L;

    private String loanPrincipal = "10000";
    private String loanRate = "5.0";
    private String loanMonths = "36";
    private int loanField = 0;

    // Audio timing and state are read from the actual native MediaPlayer.
    // Former synthetic music-track and timer state has been removed.

    private String browserUrl = "http://www.nokia.com/";
    private String detailTitle = "";
    private String detailText = "";
    private int inboxIndex = 0;
    private int contactIndex = 0;

    private boolean optionsOpen = false;
    private int optionSel = 0;
    private String optionSubmenuTitle = "";
    private boolean confirmOpen = false;
    private String confirmText = "";
    private String confirmAction = "";
    private boolean locked = false;
    private int unlockStep = 0;
    private String[] optionItems = new String[0];

    private final String[] mainMenu = {
            "Contacts", "Organiser", "Media",
            "Gallery", "Messaging", "Apps.",
            "Log", "Settings", "STORE"
    };
    private final int[] mainMenuIconIndex = {1,6,5,4,0,7,2,3,8};
    // The index in menuOrder is the visual selection; each value is a stable
    // destination. Reordering changes layout, never what an icon opens.
    private final ArrayList<Integer> menuOrder = NativeMenuLayout.defaultOrder();
    private String menuView = NativeMenuLayout.GRID_WITH_LABELS;
    private boolean menuOrganising = false;
    private int menuMovingFrom = -1;
    // Organisable, persistent and independently visible Go to shortcuts.
    private final NativeGoToShortcuts goToShortcuts = new NativeGoToShortcuts();
    // Assignable Nokia home-screen selection/navigation keys. Stored only in
    // this APK and validated so at least one key always opens the main Menu.
    private final NativeHomeShortcuts homeShortcuts = new NativeHomeShortcuts();
    private int shortcutAssignmentSlot = NativeHomeShortcuts.LSK;

    private final String[] t9Words = {
            "a","am","an","and","are","as","at","be","call","can","come","day","do","for",
            "good","hello","home","how","i","in","is","it","later","love","me","meeting",
            "message","my","no","nokia","now","of","ok","okay","on","one","phone","please",
            "see","send","test","thanks","the","this","time","to","today","tomorrow","we",
            "will","with","world","yes","you","your"
    };

    public C2PhoneView(Context context) {
        super(context);
        fontRegular = NokiaFontFaces.get(context, false);
        fontBold = NokiaFontFaces.get(context, true);
        p.setTypeface(fontRegular);
        // Decode the *exact* original HTML Nokia PNG from raw assets. This
        // avoids drawable-density transformations and makes failures visible.
        Bitmap loadedWallpaper = null;
        try (java.io.InputStream wallpaperFile =
                     getContext().getAssets().open("nokia_wallpaper.png")) {
            loadedWallpaper = BitmapFactory.decodeStream(wallpaperFile);
        } catch (Exception wallpaperError) {
            android.util.Log.e("C2Wallpaper", "Unable to load original HTML wallpaper", wallpaperError);
        }
        if (loadedWallpaper == null) {
            android.util.Log.e("C2Wallpaper", "Asset PNG decode returned null; attempting drawable fallback");
            loadedWallpaper = BitmapFactory.decodeResource(getResources(), R.drawable.screen_wallpaper);
        }
        wallpaper = loadedWallpaper;
        android.util.Log.i("C2Wallpaper", wallpaper == null ? "WALLPAPER MISSING"
                : "Decoded Nokia HTML wallpaper " + wallpaper.getWidth() + "x" + wallpaper.getHeight());
        for (int i = 0; i < menuIcons.length; i++) menuIcons[i] = RebornAssets.main(i);
        String[] ids = {
                "0595","0540","0513","0628","0715","0703","0558","0599",
                "0593","0922","0917","0562","0570","1066","0415","0724",
                "0557","0661","0528","0569","0580",
                "0567","0722","0596","0619","0625","0624","0909"
        };
        for (String id : ids) listIcons.put(id, RebornAssets.list(id));
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        setFocusableInTouchMode(true);
        backend = new AndroidBackend(context);
        prefs = context.getSharedPreferences("c2_state", Context.MODE_PRIVATE);
        contactGroups = new NativeContactGroups(prefs);
        galleryStore = new NativeGalleryStore(context, prefs);
        voiceRecorder = new NativeVoiceRecorder(context);
        musicEngine = new NativeMusicPlayer(context, prefs);
        musicEngine.setOnChanged(this::postInvalidate);
        loadState();
        refreshDeviceData();
    }

    public void onMicrophonePermissionResult(boolean granted) {
        showNotice(granted ? "Microphone ready. Select Record" : "Microphone permission denied");
        invalidate();
    }

    public void onAppPaused() {
        cancelKeypadTouch();
        if (voiceRecorder.isRecording()) finishVoiceRecording();
        voiceRecorder.stopPlayback();
        musicEngine.pause();
        invalidate();
    }

    public void onAppDestroyed() {
        cancelKeypadTouch();
        voiceRecorder.release();
        musicEngine.release();
    }

    private void beginVoiceRecording() {
        if (voiceRecorder.isRecording()) return;
        if (!(getContext() instanceof MainActivity)) {
            showNotice("Microphone unavailable");
            return;
        }
        if (!((MainActivity) getContext()).ensureRecorderPermission()) {
            showNotice("Allow microphone, then select Record");
            return;
        }
        if (voiceRecorder.start()) showNotice("Recording started");
        else showNotice("Unable to start recording");
        invalidate();
    }

    private void finishVoiceRecording() {
        if (!voiceRecorder.isRecording()) return;
        java.io.File saved = voiceRecorder.stop();
        showNotice(saved == null ? "Recording too short or failed" : "Recording saved");
        invalidate();
    }

    private void playSelectedVoiceNote() {
        if (voiceRecorder.at(sel) == null) {
            showNotice("No recording selected");
            return;
        }
        if (voiceRecorder.togglePlayback(sel, this::postInvalidate)) {
            showNotice(voiceRecorder.isPlaying() ? "Playing" : "Playback stopped");
        } else showNotice("Cannot play recording");
        invalidate();
    }

    private void showVoiceRecordingMemory() {
        java.util.ArrayList<java.io.File> files = voiceRecorder.entries();
        long size = 0;
        for (java.io.File f : files) size += f.length();
        detailTitle = "Recorder memory";
        detailText = "Saved recordings: " + files.size()
                + "\nUsed space: " + size + " bytes"
                + "\nLocation: App private storage"
                + "\nDelete cannot be undone";
        go("itemDetail");
    }

    private void voiceNoteDetails() {
        java.io.File file = voiceRecorder.at(sel);
        if (file == null) {
            showNotice("No recording selected");
            return;
        }
        detailTitle = "Recording details";
        detailText = "Name: " + file.getName()
                + "\nSize: " + file.length() + " bytes"
                + "\nRecorded: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                    .format(new Date(file.lastModified()))
                + "\nStored privately in this app";
        go("itemDetail");
    }

    public void onPermissionsChanged() {
        refreshDeviceData();
        invalidate();
    }

    /**
     * Called after Android's SMS provider changes while this Activity is
     * visible. The Activity debounces provider notifications and unregisters
     * its observer when backgrounded. The unread envelopes, conversation red
     * dots and Nokia status envelope all derive from refreshed SMS data.
     */
    public void onSmsProviderChanged() {
        refreshSmsFromPhone();
        invalidate();
    }

    /** Demo-data controls are absent from non-debuggable release APKs. */
    private boolean isDemoToolsAvailable() {
        return (getContext().getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void clearFixtureReadAndHiddenOverrides() {
        boolean dirtyRead = false, dirtyHidden = false;
        for (Long id : new ArrayList<>(localReadSmsIds)) {
            if (NativeAuditFixtures.isFixtureMessageId(id)) {
                localReadSmsIds.remove(id);
                dirtyRead = true;
            }
        }
        for (Long id : new ArrayList<>(localUnreadSmsIds)) {
            if (NativeAuditFixtures.isFixtureMessageId(id)) {
                localUnreadSmsIds.remove(id);
                dirtyRead = true;
            }
        }
        for (Long id : new ArrayList<>(hiddenSmsIds)) {
            if (NativeAuditFixtures.isFixtureMessageId(id)) {
                hiddenSmsIds.remove(id);
                dirtyHidden = true;
            }
        }
        if (dirtyRead) persistSmsReadOverrides();
        if (dirtyHidden) saveLongSet("sms_hidden", hiddenSmsIds);
        markedSmsIds.clear();
        markedThreadIds.clear();
        smsExtractedDetails.clear();
        activeMessageId = -1L;
        activeThreadId = -1L;
    }

    /**
     * Separate, reversible, opt-in set of debug fixtures. No provider insert,
     * update, delete, SMS send or call is ever part of this transition.
     */
    private void changeAuditDemoMode(boolean enable) {
        if (!isDemoToolsAvailable()) {
            showNotice("Debug tools unavailable");
            return;
        }
        clearFixtureReadAndHiddenOverrides();
        demoMode = enable;
        prefs.edit().putBoolean("audit_demo_mode_v1", enable).apply();
        refreshContactsFromPhone();
        if (!enable && !backend.has(Manifest.permission.READ_CONTACTS)) {
            // Restore any preexisting app-local Contacts that demo mode hid.
            contacts.clear();
            try {
                JSONArray stored = new JSONArray(prefs.getString("contacts", "[]"));
                for (int i = 0; i < stored.length(); i++) {
                    JSONObject item = stored.optJSONObject(i);
                    if (item != null)
                        contacts.add(new Contact(item.optString("name"),
                                item.optString("number")));
                }
            } catch (Exception ignored) { }
        }
        refreshSmsFromPhone();
        history.clear();
        page = enable ? "conversations" : "settings";
        sel = enable ? 0 : 15;
        optionsOpen = false;
        markedContactIndices.clear();
        contactIndex = 0;
        showNotice(enable ? "DEMO loaded: 12 contacts, 12 SMS" : "Demo data cleared");
        invalidate();
    }

    private void showAuditDemoDetails() {
        detailTitle = "Debug demo data";
        detailText = "12 fictional contacts (alphabetical)"
                + "\n2 contacts with multiple numbers"
                + "\n12 SMS across 6 conversations"
                + "\n3 unread incoming SMS and sent/read items"
                + "\nSender with no saved contact"
                + "\nPhone, email and web link examples"
                + "\nOnly in this app (no provider writes)"
                + "\nReal calls, SMS sending and Contacts edits blocked"
                + "\nSelect Seed again to reset read/hidden state";
        go("itemDetail");
    }

    private void refreshDeviceData() {
        refreshContactsFromPhone();
        refreshSmsFromPhone();
        refreshCallsFromPhone();
    }

    private void refreshContactsFromPhone() {
        if (demoMode) {
            // Isolated data: do not read or mix with actual provider contacts.
            deviceContactsActive = true;
            contacts.clear();
            contactNumbersById.clear();
            markedContactIndices.clear();
            for (NativeAuditFixtures.ContactRow sample : NativeAuditFixtures.contacts()) {
                contacts.add(new Contact(sample.name, sample.numbers[0],
                        sample.id, sample.id, sample.id, true));
                contactNumbersById.put(sample.id,
                        new ArrayList<>(java.util.Arrays.asList(sample.numbers)));
            }
            contactIndex = Math.max(0, Math.min(contactIndex, contacts.size() - 1));
            return;
        }
        deviceContactsActive = backend.has(Manifest.permission.READ_CONTACTS);
        contactNumbersById.clear();
        contactNumberRawIds.clear();
        contactNumberDataIds.clear();
        if (!deviceContactsActive) {
            // Clear formerly visible provider contacts if permission was revoked.
            for (int i = contacts.size() - 1; i >= 0; i--)
                if (contacts.get(i).device) contacts.remove(i);
            markedContactIndices.clear();
            contactIndex = Math.min(contactIndex, Math.max(0, contacts.size() - 1));
            return;
        }
        ArrayList<AndroidBackend.DeviceContact> real = backend.loadContacts();
        contacts.clear();
        markedContactIndices.clear();
        for (AndroidBackend.DeviceContact c : real) {
            contacts.add(new Contact(c.name, c.number, c.contactId, c.rawContactId, c.phoneDataId, true));
            contactNumbersById.put(c.contactId, new ArrayList<>(c.numbers));
            contactNumberRawIds.put(c.contactId, new ArrayList<>(c.numberRawIds));
            contactNumberDataIds.put(c.contactId, new ArrayList<>(c.numberDataIds));
        }
        contactIndex = Math.min(contactIndex, Math.max(0, contacts.size() - 1));
    }

    private void refreshSmsFromPhone() {
        // Provider-independent SMS screen testing, with no READ_SMS permission.
        deviceSmsActive = demoMode || backend.has(Manifest.permission.READ_SMS);
        if (!deviceSmsActive) {
            // Remove cached phone SMS immediately after permission revocation.
            // App-owned drafts and copied templates remain untouched.
            deviceSms.clear();
            smsInbox.clear();
            smsThreads.clear();
            smsExtractedDetails.clear();
            if ("smsUseDetails".equals(page)) {
                history.clear();
                page = "messaging";
                sel = 0;
            }
            markedThreadIds.clear();
            markedSmsIds.clear();
            activeMessageId = -1L;
            activeThreadId = -1L;
            return;
        }
        deviceSms.clear();
        if (demoMode) {
            for (NativeAuditFixtures.MessageRow row :
                    NativeAuditFixtures.messages(System.currentTimeMillis())) {
                if (!hiddenSmsIds.contains(row.id))
                    deviceSms.add(new AndroidBackend.DeviceSms(row.id, row.threadId,
                            row.address, row.body, row.date, row.type, row.read));
            }
        } else {
            for (AndroidBackend.DeviceSms m : backend.loadSms(800)) {
                if (!hiddenSmsIds.contains(m.id)) deviceSms.add(m);
            }
        }
        smsInbox.clear();
        smsThreads.clear();
        for (AndroidBackend.DeviceSms m : deviceSms) {
            if (m.type == Telephony.Sms.MESSAGE_TYPE_INBOX) smsInbox.add(m);
            boolean seen = false;
            for (SmsThread t : smsThreads) {
                if (t.threadId == m.threadId) {
                    seen = true;
                    break;
                }
            }
            if (!seen) smsThreads.add(new SmsThread(m.threadId, m.address, m.body, m.date));
        }
        if ("conversations".equals(page) && sel >= smsThreads.size()) sel = Math.max(0, smsThreads.size() - 1);
        if ("inbox".equals(page) && sel >= smsInbox.size()) sel = Math.max(0, smsInbox.size() - 1);
        if ("conversation".equals(page) && sel >= conversationMessages().size())
            sel = Math.max(0, conversationMessages().size() - 1);
    }

    private void refreshCallsFromPhone() {
        deviceCallsActive = backend.has(Manifest.permission.READ_CALL_LOG);
        if (!deviceCallsActive) return;
        deviceCalls.clear();
        for (AndroidBackend.DeviceCall item : backend.loadCalls(300)) {
            if (!hiddenCallKeys.contains(callRecordKey(item))) deviceCalls.add(item);
        }
    }

    private void loadState() {
        demoMode = isDemoToolsAvailable() && prefs.getBoolean("audit_demo_mode_v1", false);
        menuView = NativeMenuLayout.validatedMode(
                prefs.getString("native_menu_view_v1", NativeMenuLayout.GRID_WITH_LABELS));
        menuOrder.clear();
        ArrayList<Integer> storedMenu = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("native_menu_order_v1", "[]"));
            for (int i = 0; i < array.length(); i++) storedMenu.add(array.getInt(i));
        } catch (Exception ignored) {
            storedMenu.clear();
        }
        menuOrder.addAll(NativeMenuLayout.validatedOrder(storedMenu));
        boolean goToRestored = goToShortcuts.restoreSerialized(
                prefs.getString("goto_order_v1", "[]"),
                prefs.getString("goto_visible_v1", "[]"));
        auditGoTo("load restored=" + goToRestored);
        boolean homeKeyRestored = homeShortcuts.restore(
                prefs.getString("home_shortcuts_v1", homeShortcuts.serialize()));
        auditHomeShortcuts("load restored=" + homeKeyRestored);
        profile = prefs.getString("profile", "General");
        theme = prefs.getString("theme", "Dark");
        if ("Nokia".equals(theme)) theme = "Dark";
        bluetooth = prefs.getBoolean("bluetooth", false);
        alarmTime = prefs.getString("alarmTime", "07:00");
        alarmOn = prefs.getBoolean("alarmOn", false);
        alarmRepeat = prefs.getBoolean("alarmRepeat", false);
        speedDiallingEnabled = prefs.getBoolean("speed_dialling_enabled_v1", false);
        blacklistEnabled = prefs.getBoolean("rebornBlacklistOn", false);
        whitelistEnabled = prefs.getBoolean("rebornWhitelistOn", false);
        loadStringList("rebornBlacklist", blacklistNumbers);
        loadStringList("rebornWhitelist", whitelistNumbers);
        try {
            JSONObject speed = new JSONObject(prefs.getString("speed_dials_v1", "{}"));
            for (int slot = 2; slot <= 9; slot++) {
                String number = speed.optString(String.valueOf(slot), "");
                if (!number.trim().isEmpty()) speedDialNumbers.put(slot, number);
            }
        } catch (Exception ignored) { }

        loadStringList("notes", notes);
        loadStringList("todos", todos);
        loadStringList("calendar", calendar);
        loadStringList("sms_templates_v1", savedSmsTemplates);
        loadDrafts();
        loadLongSet("sms_local_read", localReadSmsIds);
        loadLongSet("sms_local_unread", localUnreadSmsIds);
        loadLongSet("sms_hidden", hiddenSmsIds);
        hiddenCallKeys.addAll(prefs.getStringSet("call_hidden_v1",
                java.util.Collections.<String>emptySet()));

        try {
            JSONArray a = new JSONArray(prefs.getString("contacts", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                contacts.add(new Contact(o.optString("name"), o.optString("number")));
            }
        } catch (Exception ignored) {}

        if (contacts.isEmpty()) {
            contacts.add(new Contact("Alex Morgan", "+44 7700 900123"));
            contacts.add(new Contact("Demo contact", "+44 7700 900456"));
            contacts.add(new Contact("Nokia service", "12345"));
            saveContacts();
        }
        if (notes.isEmpty()) {
            notes.add("Welcome to GPT c2 from reborn.");
            saveStringList("notes", notes);
        }
    }

    private void loadStringList(String key, ArrayList<String> out) {
        try {
            JSONArray a = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        } catch (Exception ignored) {}
    }

    private void loadDrafts() {
        String stored = prefs.getString("drafts_v2", null);
        try {
            if (stored != null) {
                JSONArray entries = new JSONArray(stored);
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject obj = entries.optJSONObject(i);
                    if (obj == null) continue;
                    drafts.add(new DraftMessage(obj.optString("recipient", ""),
                            obj.optString("body", ""), obj.optString("inputMode", "T9"),
                            obj.optLong("createdAt", 0L)));
                }
                return;
            }
            // Upgrade legacy body-only drafts once without discarding saved text.
            JSONArray legacy = new JSONArray(prefs.getString("drafts", "[]"));
            for (int i = 0; i < legacy.length(); i++) {
                String body = legacy.optString(i, "");
                if (!body.isEmpty())
                    drafts.add(new DraftMessage("", body, "T9", System.currentTimeMillis()));
            }
            saveDrafts();
        } catch (Exception ignored) {
            // Retain the original stored values; do not overwrite on parse error.
        }
    }

    private void saveDrafts() {
        JSONArray array = new JSONArray();
        try {
            for (DraftMessage draft : drafts) {
                JSONObject obj = new JSONObject();
                obj.put("recipient", draft.recipient);
                obj.put("body", draft.body);
                obj.put("inputMode", draft.inputMode);
                obj.put("createdAt", draft.createdAt);
                array.put(obj);
            }
            prefs.edit().putString("drafts_v2", array.toString()).apply();
        } catch (Exception ignored) {
            showNotice("Unable to save draft");
        }
    }

    private void saveCurrentDraft() {
        if (recipient.trim().isEmpty() && messageBody.trim().isEmpty()) return;
        DraftMessage newDraft = new DraftMessage(recipient, messageBody, inputMode,
                System.currentTimeMillis());
        if (editingDraftIndex >= 0 && editingDraftIndex < drafts.size())
            drafts.set(editingDraftIndex, newDraft);
        else {
            drafts.add(0, newDraft);
            editingDraftIndex = 0;
        }
        saveDrafts();
    }

    private void saveStringList(String key, List<String> values) {
        JSONArray a = new JSONArray();
        for (String s : values) a.put(s);
        prefs.edit().putString(key, a.toString()).apply();
    }

    private void loadLongSet(String key, Set<Long> out) {
        try {
            Set<String> stored = prefs.getStringSet(key, null);
            if (stored == null) return;
            for (String v : stored) out.add(Long.parseLong(v));
        } catch (Exception ignored) {}
    }

    private void saveLongSet(String key, Set<Long> values) {
        HashSet<String> out = new HashSet<>();
        for (Long v : values) out.add(String.valueOf(v));
        prefs.edit().putStringSet(key, out).apply();
    }

    private void saveContacts() {
        if (demoMode || deviceContactsActive) return;
        JSONArray a = new JSONArray();
        try {
            for (Contact c : contacts) {
                JSONObject o = new JSONObject();
                o.put("name", c.name);
                o.put("number", c.number);
                a.put(o);
            }
        } catch (Exception ignored) {}
        prefs.edit().putString("contacts", a.toString()).apply();
    }

    private void saveSpeedDialSettings() {
        JSONObject values = new JSONObject();
        try {
            for (int slot = 2; slot <= 9; slot++) {
                String number = speedDialNumbers.get(slot);
                if (number != null && !number.trim().isEmpty())
                    values.put(String.valueOf(slot), number);
            }
        } catch (Exception ignored) { }
        prefs.edit().putString("speed_dials_v1", values.toString()).apply();
    }

    private String[] speedDialSlots() {
        String[] labels = new String[8];
        for (int i = 0; i < labels.length; i++) {
            int slot = i + 2;
            String number = speedDialNumbers.get(slot);
            labels[i] = slot + ": " + (number == null ? "(Empty)" : contactNameFor(number));
        }
        return labels;
    }

    private void showSpeedDialDetails(int slot) {
        String number = speedDialNumbers.get(slot);
        if (number == null || number.isEmpty()) {
            showNotice("No number assigned to " + slot);
            return;
        }
        detailTitle = "Speed dial " + slot;
        detailText = "Contact: " + contactNameFor(number)
                + "\nNumber: " + number
                + "\nSaved locally in the APK, not Android speed dials";
        go("itemDetail");
    }

    private void assignSpeedDial(int slot, String number) {
        if (slot < 2 || slot > 9 || number == null || number.trim().isEmpty()) {
            showNotice("No phone number to assign");
            return;
        }
        speedDialNumbers.put(slot, number);
        saveSpeedDialSettings();
        showNotice("Speed dial " + slot + " assigned in app");
    }

    private void copyNumberToClipboard(String number) {
        if (number == null || number.trim().isEmpty()) {
            showNotice("No phone number");
            return;
        }
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                showNotice("Clipboard unavailable");
                return;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", number));
            showNotice("Number copied");
        } catch (RuntimeException ignored) {
            showNotice("Clipboard unavailable");
        }
    }

    private void saveFilterSettings() {
        prefs.edit().putBoolean("rebornBlacklistOn", blacklistEnabled)
                .putBoolean("rebornWhitelistOn", whitelistEnabled).apply();
        saveStringList("rebornBlacklist", blacklistNumbers);
        saveStringList("rebornWhitelist", whitelistNumbers);
    }

    private String[] displayFilterPool(ArrayList<String> pool) {
        String[] names = new String[pool.size()];
        for (int i = 0; i < pool.size(); i++) names[i] = contactNameFor(pool.get(i));
        return names;
    }

    private void addNumberToFilter(boolean black, String number) {
        if (number == null) return;
        String key = number.replaceAll("[^0-9+]", "").replaceAll("(?<=.)\\+", "");
        if (key.isEmpty()) { showNotice("No phone number"); return; }
        ArrayList<String> pool = black ? blacklistNumbers : whitelistNumbers;
        if (pool.contains(key)) { showNotice("Already on list"); return; }
        pool.add(key);
        saveFilterSettings();
        showNotice(black ? "Added to blacklist" : "Added to whitelist");
    }

    /**
     * Log only fixed numeric shortcut IDs (never contact names, phone numbers,
     * messages, or other private data). Disable in non-debuggable builds.
     * On emulator: adb logcat -s C2GoToAudit:I '*:S'
     */
    private void auditGoTo(String event) {
        if (!isDemoToolsAvailable()) return;
        android.util.Log.i("C2GoToAudit",
                event + " order=" + goToShortcuts.serializedOrder()
                        + " shown=" + goToShortcuts.serializedEnabled());
    }

    private void auditHomeShortcuts(String event) {
        if (isDemoToolsAvailable())
            android.util.Log.i("C2HomeKeys", event + " keys=" + homeShortcuts.serialize());
    }

    private void saveHomeShortcuts() {
        prefs.edit().putString("home_shortcuts_v1", homeShortcuts.serialize()).apply();
        auditHomeShortcuts("save");
    }

    private void saveGoToShortcuts() {
        prefs.edit()
                .putString("goto_order_v1", goToShortcuts.serializedOrder())
                .putString("goto_visible_v1", goToShortcuts.serializedEnabled())
                .apply();
        auditGoTo("save");
    }

    private void saveMenuPreferences() {
        JSONArray order = new JSONArray();
        for (Integer item : menuOrder) order.put(item);
        prefs.edit().putString("native_menu_view_v1", menuView)
                .putString("native_menu_order_v1", order.toString()).apply();
    }

    private void saveSettings() {
        prefs.edit()
                .putString("profile", profile)
                .putString("theme", theme)
                .putBoolean("bluetooth", bluetooth)
                .putString("alarmTime", alarmTime)
                .putBoolean("alarmOn", alarmOn)
                .putBoolean("alarmRepeat", alarmRepeat)
                .apply();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        fitScale = Math.min(getWidth() / DW, getHeight() / DH);
        offX = (getWidth() - DW * fitScale) / 2f;
        offY = (getHeight() - DH * fitScale) / 2f;

        c.save();
        c.translate(offX, offY);
        c.scale(fitScale, fitScale);
        drawPhone(c);
        c.restore();

        postInvalidateDelayed(500);
    }

    private void drawPhone(Canvas c) {
        p.setShader(new LinearGradient(0, 0, 0, DH,
                Color.rgb(26,30,30), Color.rgb(3,5,5), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(45, 8, 315, 772), 42, 42, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.4f);
        p.setColor(Color.rgb(185,190,190));
        c.drawRoundRect(new RectF(45, 8, 315, 772), 42, 42, p);
        p.setStrokeWidth(1f);
        p.setColor(Color.rgb(80,84,84));
        c.drawRoundRect(new RectF(49, 12, 311, 768), 38, 38, p);
        p.setStyle(Paint.Style.FILL);

        p.setColor(Color.rgb(190,195,195));
        c.drawRoundRect(new RectF(41, 95, 45, 700), 2, 2, p);
        c.drawRoundRect(new RectF(315, 95, 319, 700), 2, 2, p);

        p.setColor(Color.rgb(80,82,84));
        c.drawRoundRect(new RectF(155, 27, 205, 30), 2, 2, p);

        p.setColor(Color.rgb(220,220,220));
        p.setTypeface(fontBold);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(18);
        c.drawText("NOKIA", 180, 61, p);
        p.setTypeface(fontRegular);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(10);
        c.drawText("C2", 78, 59, p);

        p.setColor(Color.rgb(3,4,4));
        c.drawRoundRect(new RectF(67, 86, 293, 397), 5, 5, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        p.setColor(Color.rgb(82,88,88));
        c.drawRoundRect(new RectF(69, 88, 291, 395), 4, 4, p);
        p.setStyle(Paint.Style.FILL);

        c.save();
        c.clipRect(SX, SY, SX + SW, SY + SH);
        c.translate(SX, SY);
        float scale = SW / 240f;
        c.scale(scale, scale);
        drawDisplay(c);
        c.restore();

        drawControls(c);
    }

    private void drawDisplay(Canvas c) {
        p.setColor(themeBackground());
        c.drawRect(0, 0, 240, 320, p);

        if ("home".equals(page)) drawHome(c);
        else if ("menu".equals(page)) drawMenu(c);
        else if ("compose".equals(page)) drawCompose(c);
        else if ("dial".equals(page)) drawDial(c);
        else if ("call".equals(page)) drawCall(c);
        else if ("cameraViewfinder".equals(page) || "videoViewfinder".equals(page)) drawNokiaCameraViewfinder(c);
        else if ("cameraResult".equals(page)) drawCameraResult(c);
        else if ("calculator".equals(page)) drawCalculator(c);
        else if ("loan".equals(page)) drawLoan(c);
        else if ("stopwatch".equals(page)) drawStopwatch(c);
        else if ("countdown".equals(page)) drawCountdown(c);
        else if ("music".equals(page)) drawMusic(c);
        else if ("browser".equals(page)) drawBrowser(c);
        else if ("messageRead".equals(page)) drawMessageRead(c);
        else if ("conversation".equals(page)) drawConversation(c);
        else if ("contactDetail".equals(page)) drawContactDetail(c);
        else if ("contactSearch".equals(page)) drawContactSearch(c);
        else if ("edit".equals(page)) drawEditor(c);
        else if ("alarmEdit".equals(page)) drawAlarmEdit(c);
        else if ("itemDetail".equals(page)) drawItemDetail(c);
        else if ("galleryPreview".equals(page)) drawGalleryPreview(c);
        else if ("voiceRecorder".equals(page)) drawNativeVoiceRecorder(c);
        else drawListPage(c);

        drawSoftBar(c);

        if (optionsOpen) drawOptions(c);
        if (confirmOpen) drawConfirm(c);
        if (noticeUntil > SystemClock.uptimeMillis() && !notice.isEmpty()) drawNotice(c);
    }

    private int themeBackground() {
        if ("Silver".equals(theme)) return Color.rgb(38, 38, 42);
        if ("Blue".equals(theme)) return Color.rgb(8, 18, 34);
        return Color.BLACK;
    }

    private int themeText() {
        return Color.WHITE;
    }

    private int accent() {
        if ("Silver".equals(theme)) return Color.rgb(76, 87, 93);
        if ("Dark".equals(theme)) return Color.rgb(65, 123, 148);
        if ("Blue".equals(theme)) return Color.rgb(45, 102, 153);
        return Color.rgb(40, 118, 145);
    }

    private void drawStatus(Canvas c, String title) {
        p.setColor(Color.BLACK);
        c.drawRect(0, 0, 240, 41, p);
        // Firmware-derived status glyphs; the screenshot's order is signal, battery,
        // unread envelope. All three sit together at the left of the status clock.
        Bitmap statusSignal = RebornIconAtlas.get("STATUS_SIGNAL");
        if (statusSignal != null) c.drawBitmap(statusSignal, null, new RectF(8, 4, 28, 19), p);
        Bitmap statusBattery = RebornIconAtlas.get("STATUS_BATTERY");
        if (statusBattery != null) c.drawBitmap(statusBattery, null, new RectF(35, 5, 55, 19), p);
        if (hasUnreadSms()) {
            Bitmap statusMail = RebornIconAtlas.get("STATUS_MSG");
            if (statusMail != null) c.drawBitmap(statusMail, null, new RectF(59, 4, 78, 19), p);
            else drawUnreadEnvelope(c, 61, 5, Color.WHITE);
        }
        if (demoMode && isDemoToolsAvailable()) {
            p.setColor(Color.rgb(255, 209, 70));
            p.setTypeface(fontBold);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(11);
            if (isDemoToolsAvailable()) c.drawText("DEMO", 150, 17, p);
        }
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTypeface(fontRegular);
        p.setTextSize(14);
        c.drawText(new SimpleDateFormat("HH:mm", Locale.UK).format(new Date()), 232, 17, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(16);
        c.drawText(title, 6, 37, p);
    }

    private void drawHome(Canvas c) {
        // Use the installed wallpaper on the whole LCD; the Home chrome is
        // drawn separately instead of covering the image with a dark vignette.
        if (wallpaper != null) {
            p.setColor(Color.WHITE);
            p.setAlpha(255);
            c.drawBitmap(wallpaper, null, new RectF(0, 0, 240, 320), p);
        } else {
            p.setColor(Color.rgb(143, 148, 151));
            c.drawRect(0, 0, 240, 286, p);
        }
        p.setTypeface(fontRegular);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(13);
        c.drawText("Demo network", 8, 23, p);
        p.setTextSize(12);
        c.drawText(new SimpleDateFormat("EEE dd-MM-yyyy", Locale.UK).format(new Date()), 8, 61, p);
        c.drawText(profile, 8, 267, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(15);
        c.drawText(new SimpleDateFormat("HH:mm", Locale.UK).format(new Date()), 232, 22, p);
        if (hasUnreadSms()) {
            Bitmap statusMail = RebornIconAtlas.get("STATUS_MSG");
            if (statusMail != null) c.drawBitmap(statusMail, null,
                    new RectF(208, 29, 229, 45), p);
        }
        if (alarmOn) {
            p.setTextSize(11);
            c.drawText("Alarm " + alarmTime, 230, 266, p);
        }
        // Demo data is isolated by the data backend; its warning belongs in
        // debug diagnostics, not as an overlay on the Nokia Home display.
    }

    private void drawMenu(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        int chosen = NativeMenuLayout.destination(menuOrder, sel);
        drawStatus(c, NativeMenuLayout.GRID.equals(menuView) && chosen >= 0
                ? mainMenu[chosen] : "Menu");
        if (NativeMenuLayout.LIST.equals(menuView)) drawMenuList(c);
        else if (NativeMenuLayout.TAB.equals(menuView)) drawMenuTabs(c);
        else drawMenuGrid(c, NativeMenuLayout.GRID_WITH_LABELS.equals(menuView));
        if (menuOrganising) {
            p.setColor(Color.argb(220, 0, 0, 0));
            c.drawRect(0, 269, 240, 291, p);
            p.setColor(Color.WHITE);
            p.setTextSize(11);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(menuMovingFrom < 0 ? "Choose item to move"
                    : "Choose its destination", 120, 284, p);
        }
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(fontRegular);
    }

    private void drawMenuGrid(Canvas c, boolean showLabels) {
        for (int pos = 0; pos < menuOrder.size(); pos++) {
            int destination = menuOrder.get(pos);
            int col = pos % 3, row = pos / 3;
            float x = 4 + col * 78;
            float y = 43 + row * 78;
            RectF r = new RectF(x, y, x + 75, y + 72);
            if (pos == sel) {
                p.setShader(new LinearGradient(0, y, 0, y + 72,
                        Color.rgb(92, 92, 96), Color.rgb(18, 18, 20),
                        Shader.TileMode.CLAMP));
                c.drawRoundRect(r, 5, 5, p);
                p.setShader(null);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1);
                p.setColor(Color.LTGRAY);
                c.drawRoundRect(r, 5, 5, p);
                p.setStyle(Paint.Style.FILL);
            }
            if (menuOrganising && pos == menuMovingFrom) drawMenuMoveOutline(c, r);
            Bitmap icon = menuIcons[mainMenuIconIndex[destination]];
            float iconTop = showLabels ? y + 6 : y + 13;
            if (icon != null) c.drawBitmap(icon, null,
                    new RectF(x + 20, iconTop, x + 55, iconTop + 35), p);
            if (showLabels) {
                p.setColor(Color.WHITE);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(11);
                p.setTypeface(fontRegular);
                c.drawText(mainMenu[destination], x + 37.5f, y + 60, p);
            }
        }
    }

    private void drawMenuList(Canvas c) {
        int visible = 5;
        int first = Math.max(0, Math.min(sel - 2, menuOrder.size() - visible));
        for (int row = 0; row < visible; row++) {
            int pos = first + row, dest = menuOrder.get(pos);
            float y = 43 + row * 47;
            RectF rect = new RectF(4, y, 236, y + 44);
            if (pos == sel) {
                p.setColor(Color.WHITE);
                c.drawRoundRect(rect, 3, 3, p);
            }
            if (menuOrganising && pos == menuMovingFrom) drawMenuMoveOutline(c, rect);
            Bitmap icon = menuIcons[mainMenuIconIndex[dest]];
            if (icon != null) c.drawBitmap(icon, null,
                    new RectF(12, y + 5, 45, y + 38), p);
            p.setColor(pos == sel ? Color.BLACK : Color.WHITE);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(17);
            p.setTypeface(pos == sel ? fontBold : fontRegular);
            c.drawText(mainMenu[dest], 55, y + 29, p);
        }
    }

    private void drawMenuTabs(Canvas c) {
        // Nokia-style tab navigation: the focused icon and title occupy the
        // main panel while the neighbouring tabs remain navigable above it.
        int selectedDest = menuOrder.get(Math.max(0, Math.min(sel, menuOrder.size() - 1)));
        for (int offset = -2; offset <= 2; offset++) {
            int pos = (sel + offset + menuOrder.size()) % menuOrder.size();
            int dest = menuOrder.get(pos);
            float x = 5 + (offset + 2) * 47;
            RectF tab = new RectF(x, 45, x + 43, 90);
            if (offset == 0) {
                p.setColor(Color.WHITE);
                c.drawRoundRect(tab, 4, 4, p);
            }
            if (menuOrganising && pos == menuMovingFrom) drawMenuMoveOutline(c, tab);
            Bitmap icon = menuIcons[mainMenuIconIndex[dest]];
            if (icon != null) c.drawBitmap(icon, null,
                    new RectF(x + 5, 50, x + 38, 83), p);
        }
        p.setColor(Color.argb(155, 6, 18, 34));
        c.drawRoundRect(new RectF(19, 106, 221, 261), 6, 6, p);
        Bitmap focused = menuIcons[mainMenuIconIndex[selectedDest]];
        if (focused != null) c.drawBitmap(focused, null, new RectF(86, 129, 154, 197), p);
        p.setColor(Color.WHITE);
        p.setTypeface(fontBold);
        p.setTextSize(18);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(mainMenu[selectedDest], 120, 234, p);
        p.setTypeface(fontRegular);
        p.setTextSize(11);
        c.drawText((sel + 1) + "/" + menuOrder.size(), 120, 254, p);
    }

    private void drawMenuMoveOutline(Canvas c, RectF rect) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(Color.rgb(255, 207, 54));
        c.drawRoundRect(rect, 4, 4, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawListPage(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        String[] titleItems = itemsFor(page);
        String title = "conversations".equals(page) && titleItems.length > 0
                ? "Conversations " + (sel + 1) + "/" + titleItems.length
                : "conversation".equals(page) ? "Conversation" : titleFor(page);
        drawStatus(c, title);
        String[] items = itemsFor(page);

        if (items.length == 0) {
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(14);
            c.drawText("(empty)", 120, 140, p);
            return;
        }

        int visible = ("messaging".equals(page) || "settings".equals(page) || "gallery".equals(page)) ? 4 : 5;
        int start = Math.max(0, Math.min(sel - (visible - 1), Math.max(0, items.length - visible)));
        float rowH = (286f - 42f) / visible;
        boolean showIcons = !"conversations".equals(page) && !"contactsNames".equals(page)
                && (page.equals("messaging") || page.equals("settings") || page.equals("contacts")
                || page.equals("gallery") || page.equals("media") || page.equals("organiser")
                || page.equals("log") || page.equals("applications") || page.equals("inbox")
                || page.equals("sent"));
        for (int row = 0; row < visible && start + row < items.length; row++) {
            int idx = start + row;
            float y = 42 + row * rowH;
            boolean selected = idx == sel;
            if (selected) {
                p.setColor(Color.WHITE);
                c.drawRect(3, y, 233, y + rowH - 2, p);
            }
            int textColor = selected ? Color.BLACK : Color.WHITE;
            float textX = showIcons ? 49 : 10;
            boolean callList = "calllog".equals(page) || "missed".equals(page) ||
                    "received".equals(page) || "dialled".equals(page);
            if (callList) {
                AndroidBackend.DeviceCall record = callRecordForPage(page, idx);
                if (record != null && markedCallKeys.contains(callRecordKey(record))) {
                    p.setColor(selected ? Color.BLACK : Color.WHITE);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.6f);
                    c.drawRect(209, y + 11, 225, y + 27, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setTextSize(16);
                    p.setTypeface(fontBold);
                    p.setTextAlign(Paint.Align.LEFT);
                    c.drawText("✓", 211, y + 25, p);
                    p.setTypeface(fontRegular);
                }
            }
            if ("conversations".equals(page)) textX = NativeConversationRowLayout.LABEL_X;
            if ("conversation".equals(page)) textX = 36;
            if ("gallerySort".equals(page)) {
                drawFirmwareChoice(c, idx == galleryStore.sortMode(gallerySortVideos)
                        ? "RADIO_ON" : "RADIO_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if ("speedDialMode".equals(page)) {
                drawFirmwareChoice(c, idx == (speedDiallingEnabled ? 1 : 0)
                        ? "RADIO_ON" : "RADIO_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if ("menuViewPicker".equals(page)) {
                drawFirmwareChoice(c, idx == NativeMenuLayout.modeIndex(menuView)
                        ? "RADIO_ON" : "RADIO_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if ("shortcutAssign".equals(page)) {
                drawFirmwareChoice(c, idx == homeShortcuts.assigned(shortcutAssignmentSlot)
                        ? "RADIO_ON" : "RADIO_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if ("gotoOrganise".equals(page)) {
                int shortcutId = goToShortcuts.visualId(idx);
                drawFirmwareChoice(c, goToShortcuts.isEnabled(shortcutId)
                        ? "CHECK_ON" : "CHECK_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if ("musicEqualiser".equals(page)) {
                drawFirmwareChoice(c, idx == musicEngine.selectedPresetRow()
                        ? "RADIO_ON" : "RADIO_OFF", 11, y + 9, 24);
                textX = 46;
            }
            if (("blackmode".equals(page) || "whitemode".equals(page)) && idx < 2) {
                boolean enabled = "blackmode".equals(page) ? blacklistEnabled : whitelistEnabled;
                drawFirmwareChoice(c, idx == (enabled ? 1 : 0) ? "RADIO_ON" : "RADIO_OFF",
                        11, y + 9, 24);
                textX = 46;
            }
            Bitmap icon = showIcons ? iconForRow(page, idx) : null;
            if (icon != null) c.drawBitmap(icon, null, new RectF(10, y + 8, 40, y + 38), p);
            else if ("media".equals(page) && idx == 4) drawVoiceRecorderIcon(c, 10, y + 8, 30, selected);
            else if ("media".equals(page) && idx == 5) drawEqualiserIcon(c, 10, y + 8, 30, selected);

            if ("contactsNames".equals(page) && !markedContactIndices.isEmpty()) {
                drawFirmwareChoice(c, markedContactIndices.contains(idx) ? "CHECK_ON" : "CHECK_OFF",
                        205, y + 8, 22);
            }
            if ("conversations".equals(page) && idx < smsThreads.size()) {
                SmsThread t = smsThreads.get(idx);
                Bitmap threadIcon = RebornIconAtlas.get(threadUnread(t.threadId) ? "THREAD_UNREAD" :
                        (isThreadLatestSent(t.threadId) ? "THREAD_SENT" : "THREAD_READ"));
                if (threadIcon != null) c.drawBitmap(threadIcon, null,
                        new RectF(NativeConversationRowLayout.ICON_LEFT, y + 11,
                                NativeConversationRowLayout.ICON_RIGHT, y + 37), p);
                if (!markedThreadIds.isEmpty()) {
                    drawFirmwareChoice(c, markedThreadIds.contains(t.threadId) ? "CHECK_ON" : "CHECK_OFF",
                            NativeConversationRowLayout.MARK_CHECKBOX_LEFT, y + 12, 18);
                }
                if (threadUnread(t.threadId)) {
                    p.setColor(Color.rgb(220, 40, 45));
                    c.drawCircle(NativeConversationRowLayout.UNREAD_DOT_CENTRE_X,
                            y + 18, NativeConversationRowLayout.UNREAD_DOT_RADIUS, p);
                }
            } else if ("conversation".equals(page)) {
                AndroidBackend.DeviceSms m = conversationMessageAt(idx);
                if (m != null) {
                    String iconState = m.type != Telephony.Sms.MESSAGE_TYPE_INBOX ? "SMS_SENT" :
                            (isSmsUnread(m) ? "SMS_UNREAD" : "SMS_READ");
                    Bitmap msgBitmap = RebornIconAtlas.get(iconState);
                    if (msgBitmap != null) c.drawBitmap(msgBitmap, null, new RectF(8, y + 10, 31, y + 33), p);
                    if (!markedSmsIds.isEmpty()) {
                        drawFirmwareChoice(c, markedSmsIds.contains(m.id) ? "CHECK_ON" : "CHECK_OFF",
                                207, y + 11, 18);
                    }
                }
            }
            p.setColor(textColor);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(15);
            p.setTypeface(selected ? fontBold : fontRegular);
            String rowText = items[idx];
            if ("conversations".equals(page)) {
                // Preserve the first character of names and phone numbers:
                // the envelope ends at x=29 and text now starts at x=37.
                // End-ellipsize long labels before their right-side unread
                // dot or the active Mark / Mark all checkbox.
                rowText = android.text.TextUtils.ellipsize(rowText,
                        new android.text.TextPaint(p),
                        NativeConversationRowLayout.labelMaxWidth(!markedThreadIds.isEmpty()),
                        android.text.TextUtils.TruncateAt.END).toString();
            } else if ("smsUseDetails".equals(page)) {
                rowText = android.text.TextUtils.ellipsize(rowText,
                        new android.text.TextPaint(p), 218f,
                        android.text.TextUtils.TruncateAt.END).toString();
            }
            c.drawText(rowText, textX, y + 24, p);
            p.setTypeface(fontRegular);
            String sub = "contactsNames".equals(page) ? "" : subtitleFor(page, idx);
            if (!sub.isEmpty()) {
                p.setColor(selected ? Color.DKGRAY : Color.LTGRAY);
                p.setTextSize(11);
                if ("conversations".equals(page))
                    sub = android.text.TextUtils.ellipsize(sub,
                            new android.text.TextPaint(p),
                            NativeConversationRowLayout.labelMaxWidth(!markedThreadIds.isEmpty()),
                            android.text.TextUtils.TruncateAt.END).toString();
                c.drawText(sub, textX, y + 40, p);
            }
        }

        if (items.length > visible) {
            p.setColor(Color.WHITE);
            c.drawRect(235, 47, 237, 270, p);
            float h = Math.max(18, 223f * visible / items.length);
            float y = 47 + (223f - h) * sel / Math.max(1, items.length - 1);
            p.setColor(Color.GRAY);
            c.drawRect(235, y, 237, y + h, p);
        }
    }


    /**
     * Native conversation renderer following the HTML v6.342 .thread/.bubble styling.
     * Selection alone never clears unread. Only opening a message changes local read state.
     */
    private void drawConversation(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        ArrayList<AndroidBackend.DeviceSms> messages = conversationMessages();
        String contact = messages.isEmpty() ? "Conversation" : contactNameFor(messages.get(0).address);
        p.setTypeface(fontBold);
        p.setTextSize(15);
        if (contact == null || contact.isEmpty()) contact = "Conversation";
        while (contact.length() > 1 && p.measureText(contact) > 182)
            contact = contact.substring(0, contact.length() - 1);
        drawStatus(c, contact);
        if (messages.isEmpty()) {
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(13);
            c.drawText("(empty)", 120, 145, p);
            return;
        }
        sel = Math.max(0, Math.min(sel, messages.size() - 1));
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(15);
        p.setTypeface(fontBold);
        c.drawText((sel + 1) + "/" + messages.size(), 234, 37, p);
        p.setTypeface(fontRegular);

        final int count = messages.size();
        final float[] tops = new float[count];
        final float[] heights = new float[count];
        final ArrayList<ArrayList<String>> previews = new ArrayList<>(count);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(14f);
        for (int i = 0; i < count; i++) {
            tops[i] = i == 0 ? 46 : tops[i - 1] + heights[i - 1] + 5;
            ArrayList<String> lines = smsBubbleLines(messages.get(i).body, 3);
            previews.add(lines);
            heights[i] = 24 + 15.8f * Math.max(1, lines.size());
        }
        float totalHeight = tops[count - 1] + heights[count - 1] - 46;
        float maxScroll = Math.max(0, totalHeight - 233);
        float selectedTop = tops[sel] - conversationScrollY;
        float selectedBottom = selectedTop + heights[sel];
        if (selectedBottom > 281) conversationScrollY += selectedBottom - 281;
        if (selectedTop < 46) conversationScrollY -= 46 - selectedTop;
        conversationScrollY = Math.max(0, Math.min(maxScroll, conversationScrollY));

        c.save();
        c.clipRect(1, 43, 239, 285);
        for (int i = 0; i < count; i++) {
            AndroidBackend.DeviceSms m = messages.get(i);
            float y = tops[i] - conversationScrollY;
            float h = heights[i];
            if (y > 287 || y + h < 42) continue;
            boolean chosen = i == sel;
            boolean sent = m.type != Telephony.Sms.MESSAGE_TYPE_INBOX;
            float x = sent ? 11 : 5;
            float right = sent ? 235 : 230;
            int bg = chosen ? Color.rgb(244, 244, 244) : Color.rgb(29, 42, 71);
            p.setColor(bg);
            c.drawRoundRect(new RectF(x, y, right, y + h), 3, 3, p);
            if (chosen) {
                p.setColor(Color.rgb(154, 163, 181));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.7f);
                c.drawRoundRect(new RectF(x, y, right, y + h), 3, 3, p);
                p.setStyle(Paint.Style.FILL);
            }

            Path tail = new Path();
            if (sent) {
                tail.moveTo(right, y + h - 4);
                tail.lineTo(right, y + h + 6);
                tail.lineTo(right - 9, y + h - 4);
            } else {
                tail.moveTo(x, y + h - 4);
                tail.lineTo(x, y + h + 6);
                tail.lineTo(x + 9, y + h - 4);
            }
            tail.close();
            p.setColor(bg);
            c.drawPath(tail, p);

            String kind = sent ? "SMS_SENT" : (isSmsUnread(m) ? "SMS_UNREAD" : "SMS_READ");
            Bitmap icon = RebornIconAtlas.get(kind);
            if (icon != null) c.drawBitmap(icon, null, new RectF(x + 4, y + 3, x + 22, y + 21), p);
            String time = smsDisplayTime(m.date);
            p.setColor(chosen ? Color.rgb(85, 85, 85) : Color.rgb(143, 163, 200));
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(10);
            p.setTypeface(fontRegular);
            c.drawText(time, right - 6, y + 13, p);

            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(14);
            p.setColor(chosen ? Color.rgb(20, 20, 20) : Color.rgb(223, 230, 245));
            ArrayList<String> lines = previews.get(i);
            for (int j = 0; j < lines.size(); j++)
                c.drawText(lines.get(j), x + 27, y + 34 + j * 15.8f, p);
            if (!markedSmsIds.isEmpty()) {
                drawFirmwareChoice(c, markedSmsIds.contains(m.id) ? "CHECK_ON" : "CHECK_OFF",
                        right - 23, y + h - 20, 18);
            }
        }
        c.restore();
        p.setTextAlign(Paint.Align.LEFT);
    }

    private String smsDisplayTime(long millis) {
        long stamp = millis > 0 ? millis : System.currentTimeMillis();
        SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd", Locale.UK);
        String fmt = day.format(new Date(stamp)).equals(day.format(new Date())) ? "HH:mm" : "dd-MM-yyyy";
        return new SimpleDateFormat(fmt, Locale.UK).format(new Date(stamp));
    }

    /**
     * Limit body text to the HTML's three-line message preview, with an ellipsis if truncated.
     * Use Android Paint measurements rather than fixed character counts.
     */
    private ArrayList<String> smsBubbleLines(String text, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(14f);
        p.setTypeface(fontRegular);
        String[] paragraphs = (text == null ? "" : text.replace("\r", "")).split("\n", -1);
        for (String para : paragraphs) {
            String line = "";
            for (String word : para.trim().split("\\s+")) {
                if (word.isEmpty()) continue;
                String trial = line.isEmpty() ? word : line + " " + word;
                if (p.measureText(trial) <= 190) { line = trial; continue; }
                if (!line.isEmpty()) { lines.add(line); line = ""; }
                while (word.length() > 1 && p.measureText(word) > 190) {
                    int split = 1;
                    while (split < word.length() && p.measureText(word.substring(0, split + 1)) <= 190) split++;
                    lines.add(word.substring(0, split));
                    word = word.substring(split);
                }
                line = word;
                if (lines.size() > limit) break;
            }
            if (!line.isEmpty() || para.isEmpty()) lines.add(line);
            if (lines.size() > limit) break;
        }
        if (lines.size() > limit) {
            while (lines.size() > limit) lines.remove(lines.size() - 1);
            String last = lines.get(limit - 1);
            while (last.length() > 0 && p.measureText(last + "…") > 190)
                last = last.substring(0, last.length() - 1);
            lines.set(limit - 1, last + "…");
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private boolean smsPreviewTruncated(AndroidBackend.DeviceSms m) {
        return m != null && smsBubbleLines(m.body, 4).size() > 3;
    }

    /** HTML uses Reply for short received bubbles, Open for sent/truncated bubbles. */
    private boolean smsConversationCenterOpens(AndroidBackend.DeviceSms m) {
        return m == null || m.type != Telephony.Sms.MESSAGE_TYPE_INBOX || smsPreviewTruncated(m);
    }


    /** Native list-item icons: exact mapping from the HTML v6.342 rows() screen map. */
    private Bitmap iconForRow(String pg, int idx) {
        String[] ids;
        switch (pg) {
            case "messaging":
                ids = new String[]{"0570","9001","0724","9002","0603","0602","0723","0415","0582","9003","0584","0598","0572","0604"};
                break;
            case "organiser":
                ids = new String[]{"0557","0715","0661","0703","0528","0558","0569","0580","DICT"};
                break;
            case "media":
                ids = new String[]{"GT23","GT54","GT03","0596","GT57","GTeq"};
                break;
            case "settings":
                ids = new String[]{"0595","0608","0513","0628","0574","0571","0692","0915","0568","0560","Iphone","Iacc","Icfg","Isec","Irst"};
                break;
            case "contacts":
                ids = new String[]{"0593","0922","0626","0599","0562","0605","0585","0572","9007","9006"};
                break;
            case "gallery":
                ids = new String[]{"MEMCARD","0619","0625","0620","GALth","GALgr","GALto","GALre","GALrc"};
                break;
            case "log":
                ids = new String[]{"0719","0565","0566","0563","0603","0572","0564","0921","0579","0580","0601","0627","0916","0565"};
                break;
            case "applications":
                ids = new String[]{"0617","0616","0534","0639"};
                break;
            case "inbox":
                return RebornIconAtlas.get(idx >= 0 && idx < smsInbox.size() && isSmsUnread(smsInbox.get(idx)) ? "SMS_UNREAD" : "SMS_READ");
            case "sent":
                return RebornIconAtlas.get("SMS_SENT");
            default:
                return null;
        }
        if (idx < 0 || idx >= ids.length) return null;
        Bitmap exact = RebornIconAtlas.get(ids[idx]);
        return exact != null ? exact : listIcons.get(ids[idx]);
    }

    /** Uses the newest actual SMS record, never guessing direction from an address. */
    private boolean isThreadLatestSent(long threadId) {
        AndroidBackend.DeviceSms latest = null;
        for (AndroidBackend.DeviceSms msg : deviceSms) {
            if (msg.threadId != threadId) continue;
            if (latest == null || msg.date > latest.date) latest = msg;
        }
        return latest != null && latest.type != Telephony.Sms.MESSAGE_TYPE_INBOX;
    }

    private void drawFirmwareChoice(Canvas c, String key, float x, float y, float size) {
        Bitmap icon = RebornSelectionIcons.get(getContext(), key);
        if (icon != null) c.drawBitmap(icon, null, new RectF(x, y, x + size, y + size), p);
    }

    private void drawVoiceRecorderIcon(Canvas c, float x, float y, float size, boolean selected) {
        int fg = selected ? Color.BLACK : Color.WHITE;
        p.setColor(Color.rgb(70, 110, 190));
        c.drawRoundRect(new RectF(x + 8, y + 2, x + 22, y + 19), 7, 7, p);
        p.setColor(fg);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        c.drawArc(new RectF(x + 5, y + 8, x + 25, y + 26), 0, 180, false, p);
        c.drawLine(x + 15, y + 19, x + 15, y + 27, p);
        c.drawLine(x + 10, y + 27, x + 20, y + 27, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawEqualiserIcon(Canvas c, float x, float y, float size, boolean selected) {
        int fg = selected ? Color.BLACK : Color.WHITE;
        float[] heights = {10, 20, 14, 24};
        for (int i = 0; i < 4; i++) {
            float bx = x + 3 + i * 7;
            p.setColor(i % 2 == 0 ? Color.rgb(60, 150, 230) : Color.rgb(90, 200, 110));
            c.drawRoundRect(new RectF(bx, y + 27 - heights[i], bx + 5, y + 27), 1, 1, p);
        }
        p.setColor(fg);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        c.drawRect(x + 1, y + 1, x + 30, y + 29, p);
        p.setStyle(Paint.Style.FILL);
    }

    private String subtitleFor(String pg, int idx) {
        if ("profiles".equals(pg) && idx < itemsFor(pg).length && itemsFor(pg)[idx].equals(profile)) return "Active";
        if ("themes".equals(pg) && idx < itemsFor(pg).length && itemsFor(pg)[idx].equals(theme)) return "Selected";
        if ("bluetooth".equals(pg) && idx == 0) return bluetooth ? "On" : "Off";
        if ("alarm".equals(pg)) {
            if (idx == 0) return alarmOn ? "On" : "Off";
            if (idx == 1) return alarmTime;
            if (idx == 2) return alarmRepeat ? "On" : "Off";
        }
        if ("notes".equals(pg) && idx < notes.size()) return "";
        if ("contactsNames".equals(pg) && idx < contacts.size()) return contacts.get(idx).number;
        if ("conversation".equals(pg)) {
            AndroidBackend.DeviceSms m = conversationMessageAt(idx);
            if (m != null) {
                String dir = m.type == Telephony.Sms.MESSAGE_TYPE_SENT ? "Sent " : "Received ";
                return dir + new SimpleDateFormat("HH:mm", Locale.UK).format(new Date(m.date));
            }
        }
        if ("conversations".equals(pg) && deviceSmsActive && idx < smsThreads.size()) {
            SmsThread t = smsThreads.get(idx);
            String text = t.body == null ? "" : t.body.replace("\n", " ");
            if (text.length() > 25) text = text.substring(0, 25) + "…";
            return text;
        }
        if ("messaging".equals(pg)) {
            if (idx == 1) return deviceSmsActive ? smsThreads.size() + " conversations" : "SMS permission required";
            if (idx == 2) return drafts.size() + " messages";
        }
        if ("settings".equals(pg) && idx == 1) return "Dark.nth";
        if ("shortcuts".equals(pg)) {
            if (idx == 0) return homeShortcuts.label(NativeHomeShortcuts.LSK);
            if (idx == 1) return homeShortcuts.label(NativeHomeShortcuts.RSK);
            if (idx == 2) return "Assign Up / Down / Left / Right";
            if (idx == 3) return homeShortcuts.label(NativeHomeShortcuts.CENTRE);
        }
        if ("shortcutNavigation".equals(pg) && idx >= 0 && idx < 4)
            return homeShortcuts.label(NativeHomeShortcuts.navigationSlot(idx));
        if ("drafts".equals(pg) && idx < drafts.size()) {
            String target = drafts.get(idx).recipient;
            return target.isEmpty() ? "Draft" : "To: " + target;
        }
        return "";
    }

    private void drawCompose(Canvas c) {
        drawStatus(c, "Create message");
        p.setColor("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE);
        c.drawRect(5, 24, 235, 65, p);
        if (composeFocus == 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(accent());
            c.drawRect(5, 24, 235, 65, p);
            p.setStyle(Paint.Style.FILL);
        }
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(9);
        c.drawText("To:", 10, 36, p);
        p.setTextSize(13);
        c.drawText(recipient.isEmpty() ? "Enter number" : recipient, 10, 56, p);

        p.setColor("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE);
        c.drawRect(5, 70, 235, 274, p);
        if (composeFocus == 1) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(accent());
            c.drawRect(5, 70, 235, 274, p);
            p.setStyle(Paint.Style.FILL);
        }
        p.setColor(themeText());
        p.setTextSize(13);
        drawWrappedText(c, messageBody.isEmpty() ? "Write message" : messageBody, 10, 91, 215, 17, 10);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(9);
        int smsLength = messageBody.length();
        int smsSegments = Math.max(1, (smsLength <= 160 ? 1 : (smsLength + 152) / 153));
        int smsRemaining = 160 - smsLength; // Reference displays negative overflow, e.g. -6/2
        c.drawText(smsRemaining + "/" + smsSegments, 229, 288, p);
    }

    private void drawDial(Canvas c) {
        drawStatus(c, "Dial");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(28);
        c.drawText(dial.isEmpty() ? " " : dial, 120, 120, p);
        p.setTextSize(11);
        c.drawText("Enter number", 120, 154, p);
    }

    private void drawCall(Canvas c) {
        drawStatus(c, "Call");
        int state = C2InCallService.state();
        String status = state == android.telecom.Call.STATE_RINGING ? "Incoming call" :
                state == android.telecom.Call.STATE_DIALING ? "Calling." :
                state == android.telecom.Call.STATE_CONNECTING ? "Connecting." :
                state == android.telecom.Call.STATE_ACTIVE ? "In call" :
                state == android.telecom.Call.STATE_HOLDING ? "On hold" : "Calling.";
        p.setColor(Color.rgb(22, 66, 122));
        c.drawRoundRect(new RectF(7, 46, 233, 242), 8, 8, p);
        p.setColor(Color.rgb(125, 168, 212));
        c.drawRoundRect(new RectF(16, 54, 224, 87), 5, 5, p);
        p.setColor(Color.WHITE);
        p.setTypeface(fontBold);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(16);
        c.drawText(status, 120, 76, p);
        String displayName = contactNameFor(activeCall);
        // Telecom can report country-code-normalised numbers. Compare against
        // every provider contact number, including secondary numbers, before
        // falling back to the call-log's cached name.
        if (displayName.equals(activeCall) && activeCall != null && !activeCall.isEmpty()) {
            for (Contact contact : contacts) {
                boolean match = android.telephony.PhoneNumberUtils.compare(
                        contact.number, activeCall);
                ArrayList<String> aliases = contactNumbersById.get(contact.contactId);
                if (!match && aliases != null) {
                    for (String alias : aliases) {
                        if (android.telephony.PhoneNumberUtils.compare(alias, activeCall)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match && contact.name != null && !contact.name.isEmpty()) {
                    displayName = contact.name;
                    break;
                }
            }
        }
        // Cached call-log names can resolve contacts whose provider number
        // differs from Telecom's dial string (country code, separators, etc).
        if (displayName.equals(activeCall)) {
            for (AndroidBackend.DeviceCall record : deviceCalls) {
                if (!record.name.isEmpty() &&
                        android.telephony.PhoneNumberUtils.compare(record.number, activeCall)) {
                    displayName = record.name;
                    break;
                }
            }
        }
        p.setTextSize(19);
        if (displayName.length() > 21) displayName = displayName.substring(0, 20) + "…";
        c.drawText(displayName, 120, 116, p);
        p.setTypeface(fontRegular);
        p.setTextSize(13);
        if (!displayName.equals(activeCall))
            c.drawText(activeCall, 120, 139, p);
        long connectedAt = C2InCallService.connectTime();
        long secs = connectedAt > 0 && state == android.telecom.Call.STATE_ACTIVE
                ? Math.max(0, (System.currentTimeMillis() - connectedAt) / 1000L) : 0L;
        c.drawText(connectedAt > 0 ? formatSeconds(secs) : "00:00", 120, 166, p);
        p.setColor(Color.rgb(10, 35, 65));
        c.drawRoundRect(new RectF(25, 184, 215, 195), 4, 4, p);
        p.setColor(Color.rgb(135, 193, 246));
        c.drawRoundRect(new RectF(27, 186, 30 + (int)(Math.min(60,secs) * 3), 193), 3, 3, p);
        p.setColor(Color.WHITE);
        p.setTextSize(12);
        c.drawText(C2InCallService.isRinging() ? "Call to answer" :
                loudspeaker ? "Loudspeaker on" : "Handset", 120, 228, p);
    }

    private void drawMessageRead(Canvas c) {
        AndroidBackend.DeviceSms m = smsById(activeMessageId);
        String name = m == null ? "Message unavailable" : contactNameFor(m.address);
        drawStatus(c, name);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(12);
        boolean outgoing = m != null && m.type != Telephony.Sms.MESSAGE_TYPE_INBOX;
        Bitmap icon = m == null ? null : RebornIconAtlas.get(outgoing ? "SMS_SENT" :
                (isSmsUnread(m) ? "SMS_UNREAD" : "SMS_READ"));
        if (icon != null) c.drawBitmap(icon, null, new RectF(8, 43, 31, 66), p);
        c.drawText(m == null ? "Unavailable" : outgoing ? "Sent record" :
                (isSmsUnread(m) ? "Unread" : "Received"), icon == null ? 8 : 36, 57, p);
        if (m != null) {
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(10);
            c.drawText(new SimpleDateFormat("dd/MM HH:mm", Locale.UK).format(new Date(m.date)),
                    231, 75, p);
        }
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(14);
        drawWrappedText(c, m == null ? "Message no longer available." : m.body, 8, 94, 220, 18, 10);
    }

    private void drawContactSearch(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        drawStatus(c, "Search names");
        p.setColor(Color.rgb(19, 24, 36));
        c.drawRect(5, 37, 235, 85, p);
        p.setColor(Color.WHITE);
        p.setTypeface(fontRegular);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(11);
        c.drawText(contactSearchNumeric ? "Number search (123)" : "Name search (abc)", 11, 52, p);
        p.setTextSize(18);
        String queryLabel = contactSearchQuery.isEmpty() ? "Search..." : contactSearchQuery;
        c.drawText(android.text.TextUtils.ellipsize(queryLabel,
                new android.text.TextPaint(p), 211f,
                android.text.TextUtils.TruncateAt.START).toString(), 11, 76, p);
        ArrayList<Integer> matches = contactSearchMatches();
        if (matches.isEmpty()) {
            p.setTextSize(13);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(contacts.isEmpty() ? "No contacts" : "No matches", 120, 162, p);
        } else {
            int visible = 5;
            int first = Math.max(0, Math.min(sel - 2, Math.max(0, matches.size() - visible)));
            for (int index = first; index < Math.min(matches.size(), first + visible); index++) {
                Contact found = contacts.get(matches.get(index));
                int row = index - first;
                float top = 87 + row * 38;
                boolean highlighted = index == sel;
                if (highlighted) {
                    p.setColor(Color.WHITE);
                    c.drawRect(5, top, 234, top + 36, p);
                }
                p.setColor(highlighted ? Color.BLACK : Color.WHITE);
                p.setTextAlign(Paint.Align.LEFT);
                p.setTypeface(highlighted ? fontBold : fontRegular);
                p.setTextSize(14);
                c.drawText(android.text.TextUtils.ellipsize(found.name,
                        new android.text.TextPaint(p), 214f,
                        android.text.TextUtils.TruncateAt.END).toString(), 11, top + 16, p);
                p.setTypeface(fontRegular);
                p.setTextSize(11);
                p.setColor(highlighted ? Color.DKGRAY : Color.LTGRAY);
                c.drawText(android.text.TextUtils.ellipsize(found.number,
                        new android.text.TextPaint(p), 213f,
                        android.text.TextUtils.TruncateAt.END).toString(), 11, top + 31, p);
            }
            p.setTextAlign(Paint.Align.RIGHT);
            p.setColor(Color.WHITE);
            p.setTextSize(10);
            c.drawText((Math.min(sel, matches.size() - 1) + 1)
                    + "/" + matches.size(), 231, 282, p);
        }
        p.setColor(Color.WHITE);
        p.setTypeface(fontRegular);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawContactDetail(Canvas c) {
        drawStatus(c, "Contact details");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        if (contacts.isEmpty()) {
            p.setTextSize(13);
            c.drawText("Contact unavailable", 12, 95, p);
            return;
        }

        Contact ct = contacts.get(Math.max(0, Math.min(contactIndex, contacts.size() - 1)));
        ArrayList<String> numbers = currentContactNumbers();
        // Source-exact HTML v6.343 CONTbust portrait: 42x42 CSS pixels,
        // right-aligned beside the contact name, not a fabricated dot glyph.
        Bitmap portrait = NokiaContactSprites.bust();
        if (portrait != null) {
            p.setFilterBitmap(true);
            c.drawBitmap(portrait, null, new RectF(187, 43, 229, 85), p);
        }
        p.setColor(themeText());
        p.setTypeface(fontBold);
        p.setTextSize(18);
        String shownName = android.text.TextUtils.ellipsize(ct.name,
                new android.text.TextPaint(p), 172f,
                android.text.TextUtils.TruncateAt.END).toString();
        c.drawText(shownName, 9, 77, p);
        p.setTypeface(fontRegular);

        if (numbers.isEmpty()) {
            p.setTextSize(13);
            c.drawText("No phone numbers", 12, 124, p);
            return;
        }
        sel = Math.max(0, Math.min(sel, numbers.size() - 1));
        int selected = sel;
        // Keep v1.27's individual phone-number selection while reproducing
        // the original Nokia handset icon and highlight on each number row.
        int first = Math.min(Math.max(0, selected - 2), Math.max(0, numbers.size() - 4));
        Bitmap handset = NokiaContactSprites.phone();
        c.save();
        c.clipRect(1, 95, 239, 276);
        for (int i = first; i < Math.min(first + 4, numbers.size()); i++) {
            int row = i - first;
            float top = 104 + row * 42;
            boolean active = i == selected;
            if (active) {
                p.setColor(Color.rgb(228, 231, 232));
                c.drawRoundRect(new RectF(6, top, 234, top + 40), 2, 2, p);
            }
            if (handset != null) {
                p.setFilterBitmap(true);
                c.drawBitmap(handset, null, new RectF(11, top + 8, 33, top + 30), p);
            }
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(15);
            p.setTypeface(active ? fontBold : fontRegular);
            p.setColor(active ? Color.rgb(17, 17, 17) : themeText());
            String shortValue = android.text.TextUtils.ellipsize(
                    numbers.get(i), new android.text.TextPaint(p), 193f,
                    android.text.TextUtils.TruncateAt.END).toString();
            c.drawText(shortValue, 38, top + 26, p);
        }
        c.restore();
        p.setTypeface(fontRegular);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(10);
        c.drawText("Number " + (selected + 1) + " of " + numbers.size()
                + "  •  Up/Down", 120, 283, p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawEditor(Canvas c) {
        String ttl = "Edit";
        if ("note".equals(editKind)) ttl = editIndex >= 0 ? "Edit note" : "New note";
        if ("todo".equals(editKind)) ttl = "To-do note";
        if ("calendar".equals(editKind)) ttl = "Calendar note";
        if ("contact".equals(editKind)) ttl = editIndex >= 0 ? "Edit contact" : "New contact";
        if ("groupNew".equals(editKind)) ttl = "New group";
        if ("groupRename".equals(editKind)) ttl = "Rename group";
        drawStatus(c, ttl);

        if ("contact".equals(editKind)) {
            String[] parts = editBuffer.split("\\n", -1);
            String name = parts.length > 0 ? parts[0] : "";
            String num = parts.length > 1 ? parts[1] : "";
            drawEditorField(c, "Name", name, 48, editField == 0);
            drawEditorField(c, editIndex >= 0 ? "Selected number" : "Number",
                    num, 112, editField == 1);
        } else {
            p.setColor("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE);
            c.drawRect(6, 27, 234, 274, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(accent());
            c.drawRect(6, 27, 234, 274, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(themeText());
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(13);
            drawWrappedText(c, editBuffer.isEmpty() ? "Enter text" : editBuffer, 11, 49, 215, 18, 11);
        }
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(9);
        c.drawText(inputMode, 229, 289, p);
    }

    private void drawEditorField(Canvas c, String label, String value, float y, boolean focused) {
        p.setColor("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE);
        c.drawRect(7, y, 233, y + 52, p);
        if (focused) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(accent());
            c.drawRect(7, y, 233, y + 52, p);
            p.setStyle(Paint.Style.FILL);
        }
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(9);
        c.drawText(label, 12, y + 14, p);
        p.setTextSize(14);
        c.drawText(value.isEmpty() ? " " : value, 12, y + 37, p);
    }

    private void drawAlarmEdit(Canvas c) {
        drawStatus(c, "Set alarm");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(30);
        String d = editBuffer;
        while (d.length() < 4) d += "_";
        String shown = d.substring(0,2) + ":" + d.substring(2,4);
        c.drawText(shown, 120, 120, p);
        p.setTextSize(11);
        c.drawText("Enter time (24 hour)", 120, 153, p);
    }

    private void drawCalculator(Canvas c) {
        drawStatus(c, "Calculator");
        // Preserve the complete Nokia title: its baseline is y=37 and
        // the earlier display rectangle began at y=30, painting over it.
        p.setColor("Dark".equals(theme) ? Color.rgb(25,31,33) : Color.WHITE);
        c.drawRect(8, NativeOverlayLayout.CALC_PANEL_TOP,
                232, NativeOverlayLayout.CALC_PANEL_BOTTOM, p);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(28);
        c.drawText(calc, 223, NativeOverlayLayout.CALC_VALUE_BASELINE, p);

        String[] ops = {"▲ ×","◀ −","OK =","▶ +","▼ ÷","* .   # ±"};
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(13);
        for (int i = 0; i < ops.length; i++)
            c.drawText(ops[i], 120, NativeOverlayLayout.CALC_FIRST_OPERATION_BASELINE
                    + i * NativeOverlayLayout.CALC_OPERATION_LINE_SPACING, p);
    }

    private void drawLoan(Canvas c) {
        drawStatus(c, "Loan calculator");
        String[] labels = {"Loan amount", "Interest %", "Months"};
        String[] values = {loanPrincipal, loanRate, loanMonths};
        for (int i = 0; i < 3; i++) {
            // Like Calculator, the first panel must not cover the title.
            float y = NativeOverlayLayout.LOAN_FIRST_FIELD_TOP + i * 62;
            p.setColor(i == loanField ? accent() : ("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE));
            c.drawRoundRect(new RectF(8, y, 232, y + 48), 4, 4, p);
            p.setColor(i == loanField ? Color.WHITE : themeText());
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(9);
            c.drawText(labels[i], 15, y + 14, p);
            p.setTextSize(18);
            c.drawText(values[i], 15, y + 38, p);
        }
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(11);
        c.drawText("OK = calculate monthly payment", 120, 244, p);
        c.drawText("Use ▲/▼ to change field", 120, 263, p);
    }

    private void drawStopwatch(Canvas c) {
        drawStatus(c, "Stopwatch");
        long ms = stopwatchAccum + (stopwatchRunning ? SystemClock.elapsedRealtime() - stopwatchStarted : 0);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(27);
        c.drawText(formatMillis(ms), 120, 80, p);
        p.setTextSize(11);
        c.drawText(stopwatchRunning ? "Running" : "Stopped", 120, 106, p);
        p.setTextAlign(Paint.Align.LEFT);
        int start = Math.max(0, stopwatchSplits.size() - 5);
        for (int i = start; i < stopwatchSplits.size(); i++) {
            c.drawText((i + 1) + "   " + stopwatchSplits.get(i), 24, 140 + (i - start) * 25, p);
        }
    }

    private void drawCountdown(Canvas c) {
        drawStatus(c, "Countdown timer");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        if (countdownEnd > SystemClock.elapsedRealtime()) {
            long remain = (countdownEnd - SystemClock.elapsedRealtime() + 999) / 1000;
            p.setTextSize(28);
            c.drawText(formatSeconds(remain), 120, 95, p);
            p.setTextSize(11);
            c.drawText("Counting down", 120, 123, p);
        } else {
            if (countdownEnd != 0) {
                countdownEnd = 0;
                showNotice("Time is up");
            }
            p.setTextSize(28);
            c.drawText(countdownDigits.isEmpty() ? "00:00" : countdownDigits + " min", 120, 95, p);
            p.setTextSize(11);
            c.drawText("Enter minutes, then press OK", 120, 123, p);
        }
    }

    private void drawMusic(Canvas c) {
        drawStatus(c, "Media player");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(42);
        c.drawText("♪", 120, 90, p);
        p.setTypeface(fontBold);
        p.setTextSize(15);
        String name = musicEngine.title();
        if (name.length() > 25) name = name.substring(0, 24) + "…";
        c.drawText(name, 120, 128, p);
        p.setTypeface(fontRegular);
        p.setTextSize(11);
        c.drawText(musicEngine.index() < 0 ? musicEngine.tracks.size() + " available"
                : (musicEngine.index() + 1) + "/" + musicEngine.tracks.size(), 120, 148, p);
        int elapsed = musicEngine.positionMs() / 1000;
        int duration = musicEngine.durationMs() / 1000;
        p.setTextSize(13);
        c.drawText(formatSeconds(elapsed) + " / " + formatSeconds(duration), 120, 184, p);
        p.setColor(Color.rgb(185,190,190));
        c.drawRect(30, 200, 210, 205, p);
        p.setColor(accent());
        c.drawRect(30, 200, 30 + (duration <= 0 ? 0 : 180 * Math.min(1f,
                (float) elapsed / duration)), 205, p);
        p.setColor(themeText());
        p.setTextSize(12);
        c.drawText("◀    " + (musicEngine.isPlaying() ? "Pause" : "Play") + "    ▶", 120, 240, p);
        p.setTextSize(10);
        c.drawText("Shuffle " + (musicEngine.shuffleEnabled() ? "On" : "Off")
                + "   Repeat " + musicEngine.repeatLabel(), 120, 261, p);
        if (musicEngine.tracks.isEmpty()) {
            p.setTextSize(10);
            c.drawText("Options > Add audio files", 120, 275, p);
        }
        if (musicEngine.isPlaying()) postInvalidateDelayed(500);
    }

    private void drawBrowser(Canvas c) {
        drawStatus(c, "Web");
        p.setColor("Dark".equals(theme) ? Color.rgb(49,55,58) : Color.WHITE);
        c.drawRect(4, 25, 236, 52, p);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(10);
        c.drawText(browserUrl, 8, 43, p);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(33);
        c.drawText("◎", 120, 123, p);
        p.setTextSize(14);
        c.drawText("Nokia Web", 120, 151, p);
        p.setTextSize(11);
        drawWrappedText(c, "Offline native browser simulation. No WebView is used.", 35, 182, 170, 16, 4);
    }

    private void drawNativeVoiceRecorder(Canvas c) {
        drawStatus(c, "Voice recorder");
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(17);
        c.drawText(voiceRecorder.isRecording() ? "Recording" : "Ready to record", 120, 84, p);
        p.setColor(voiceRecorder.isRecording() ? Color.rgb(225, 55, 55) : Color.GRAY);
        c.drawCircle(120, 144, voiceRecorder.isRecording() ? 19 : 13, p);
        p.setColor(Color.WHITE);
        p.setTextSize(14);
        long seconds = voiceRecorder.elapsedSeconds();
        String clock = String.format(Locale.UK, "%02d:%02d", seconds / 60, seconds % 60);
        c.drawText(clock, 120, 190, p);
        p.setTextSize(11);
        c.drawText("Recordings: " + voiceRecorder.entries().size(), 120, 228, p);
        if (voiceRecorder.isRecording()) {
            if (voiceRecorder.elapsedSeconds() >= 300) {
                finishVoiceRecording();
                showNotice("Five-minute recording limit reached");
            } else postInvalidateDelayed(500);
        }
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGalleryPreview(Canvas c) {
        drawStatus(c, galleryPreviewTitle.isEmpty() ? "Images" : galleryPreviewTitle);
        p.setColor(Color.BLACK);
        c.drawRect(4, 32, 236, 280, p);
        if (galleryPreviewBitmap != null && !galleryPreviewBitmap.isRecycled()) {
            float iw = galleryPreviewBitmap.getWidth();
            float ih = galleryPreviewBitmap.getHeight();
            float scale = Math.min(220f / Math.max(1f, iw), 224f / Math.max(1f, ih));
            float width = iw * scale, height = ih * scale;
            float left = 120f - width / 2f, top = 156f - height / 2f;
            p.setColor(Color.WHITE);
            c.drawBitmap(galleryPreviewBitmap, null,
                    new RectF(left, top, left + width, top + height), p);
        } else {
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(12);
            c.drawText("Image unavailable", 120, 146, p);
            c.drawText("Select another image", 120, 165, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawItemDetail(Canvas c) {
        drawStatus(c, detailTitle.isEmpty() ? "Details" : detailTitle);
        // Reserve the complete 41px Nokia status/title header and a visual
        // divider; the old first baseline (y=48) placed 13px letters over
        // the title baseline (y=37) on Settings > Phone and other details.
        p.setColor(Color.rgb(70, 82, 95));
        c.drawRect(9, NativeOverlayLayout.DETAIL_DIVIDER_Y,
                231, NativeOverlayLayout.DETAIL_DIVIDER_Y + 1, p);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(fontRegular);
        p.setTextSize(NativeOverlayLayout.DETAIL_TEXT_SIZE);
        drawWrappedText(c, detailText,
                NativeOverlayLayout.DETAIL_TEXT_X,
                NativeOverlayLayout.DETAIL_TEXT_BASELINE,
                NativeOverlayLayout.DETAIL_TEXT_WIDTH,
                NativeOverlayLayout.DETAIL_LINE_HEIGHT,
                NativeOverlayLayout.DETAIL_MAX_LINES);
    }

    private void drawSoftBar(Canvas c) {
        boolean homeBar = "home".equals(page);
        p.setColor(homeBar ? Color.rgb(216, 220, 222) : Color.BLACK);
        c.drawRect(0, 286, 240, 320, p);
        if (homeBar) {
            p.setColor(Color.rgb(132, 137, 140));
            c.drawRect(0, 286, 240, 287, p);
        }
        String[] soft = softLabels();
        p.setColor(homeBar ? Color.rgb(28, 34, 36) : Color.WHITE);
        p.setTextSize(14);
        p.setTypeface(fontBold);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(soft[0], 6, 307, p);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(soft[1], 120, 307, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(soft[2], 234, 307, p);
        p.setTypeface(fontRegular);
    }

    private String[] softLabels() {
        if (confirmOpen) return new String[]{"Yes", "", "No"};
        if (optionsOpen) return new String[]{"", "Select", "Back"};
        if (locked) return new String[]{"Unlock", "", ""};
        if ("home".equals(page)) return new String[]{
                homeShortcuts.label(NativeHomeShortcuts.LSK),
                homeShortcuts.label(NativeHomeShortcuts.CENTRE),
                homeShortcuts.label(NativeHomeShortcuts.RSK)};
        if ("menu".equals(page)) {
            if (menuOrganising)
                return new String[]{menuMovingFrom < 0 ? "Pick" : "Move",
                        menuMovingFrom < 0 ? "Pick" : "Place",
                        menuMovingFrom < 0 ? "Done" : "Cancel"};
            return new String[]{"Options", "Select", "Exit"};
        }
        if ("cameraViewfinder".equals(page)) return new String[]{"Options", "Capture", "Back"};
        if ("videoViewfinder".equals(page)) {
            if (videoSaving()) return new String[]{"", "Saving...", ""};
            return new String[]{videoRecording() ? "" : "Options",
                    videoRecording() ? "Stop" : "Record",
                    videoRecording() ? "Stop" : "Back"};
        }
        if ("cameraResult".equals(page)) return new String[]{"Options", "Save", "Back"};
        if ("menuViewPicker".equals(page)) return new String[]{"", "Select", "Back"};
        if ("shortcutAssign".equals(page)) return new String[]{"", "Assign", "Back"};
        if ("gotoOrganise".equals(page)) return new String[]{"Options", "Toggle", "Done"};
        if ("voiceRecorder".equals(page))
            return new String[]{"Options", voiceRecorder.isRecording() ? "Stop" : "Record", "Back"};
        if ("voiceRecordings".equals(page))
            return new String[]{"Options", voiceRecorder.isPlaying() ? "Stop" : "Play", "Back"};
        if ("contactsNames".equals(page) ||
                "calllog".equals(page) || "missed".equals(page) ||
                "received".equals(page) || "dialled".equals(page))
            return new String[]{"Options", "Details", "Back"};
        if ("conversations".equals(page)) return new String[]{"Options", "Open", "Back"};
        if ("conversation".equals(page)) return new String[]{"Options", smsConversationCenterOpens(conversationMessageAt(sel)) ? "Open" : "Reply", "Back"};
        if ("compose".equals(page)) return new String[]{"Options", "Send", "Back"};
        if ("dial".equals(page)) return new String[]{"Options", "Call", dial.isEmpty() ? "Back" : "Clear"};
        if ("call".equals(page)) return new String[]{
                "Options", C2InCallService.isRinging() ? "Answer" : "End",
                loudspeaker ? "Handset" : "Loudsp."};
        if ("calculator".equals(page)) return new String[]{"Options", "=", "Back"};
        if ("loan".equals(page)) return new String[]{"Calculate", "", "Back"};
        if ("stopwatch".equals(page)) return new String[]{"Split", stopwatchRunning ? "Stop" : "Start", "Back"};
        if ("countdown".equals(page)) return new String[]{"Options", "Start", "Back"};
        if ("music".equals(page)) return new String[]{"Options", musicEngine.isPlaying() ? "Pause" : "Play", "Back"};
        if ("musicLibrary".equals(page)) return new String[]{"Options", "Play", "Back"};
        if ("musicEqualiser".equals(page)) return new String[]{"Options", "Select", "Back"};
        if ("messageRead".equals(page)) {
            AndroidBackend.DeviceSms selected = smsById(activeMessageId);
            return new String[]{"Options", selected != null &&
                    selected.type != Telephony.Sms.MESSAGE_TYPE_INBOX ? "Forward" : "Reply", "Back"};
        }
        if ("contactDetail".equals(page)) return new String[]{"Options",
                currentContactNumbers().isEmpty() ? "" : "Call", "Back"};
        if ("contactSearch".equals(page)) return new String[]{"Options",
                contactSearchMatches().isEmpty() ? "" : "Open",
                contactSearchQuery.isEmpty() ? "Back" : "Clear"};
        if ("edit".equals(page) || "alarmEdit".equals(page)) return new String[]{"Save", "", editBuffer.isEmpty() ? "Back" : "Clear"};
        if ("browser".equals(page)) return new String[]{"Options", "Go", "Back"};
        if ("itemDetail".equals(page)) return new String[]{"Options", "", "Back"};
        if ("galleryPreview".equals(page)) return new String[]{"Options", "", "Back"};
        if ("galleryImages".equals(page) || "galleryVideos".equals(page))
            return new String[]{"Options", "Open", "Back"};
        return new String[]{"Options", "Select", "Back"};
    }

    private void drawOptions(Canvas c) {
        p.setColor(Color.rgb(17,25,35));
        c.drawRect(0, 0, 240, 286, p);

        p.setColor(Color.BLACK);
        c.drawRect(0, 0, 240, 28, p);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(fontRegular);
        p.setTextSize(18);
        c.drawText(optionSubmenuTitle.isEmpty() ? "Options" : optionSubmenuTitle, 6, 21, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(13);
        // Nokia reference popup header contains only "Options".
        // Item position/count is not displayed in this overlay.

        int visible = 6;
        int start = Math.max(0, Math.min(optionSel - (visible - 1), Math.max(0, optionItems.length - visible)));
        for (int row = 0; row < visible && start + row < optionItems.length; row++) {
            int idx = start + row;
            float y = 29 + row * 40;
            if (idx == optionSel) {
                p.setColor(Color.WHITE);
                c.drawRect(0, y, 240, y + 38, p);
            }
            p.setColor(idx == optionSel ? Color.BLACK : Color.WHITE);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(17);
            c.drawText(optionItems[idx], 8, y + 25, p);
        }
    }

    private void drawConfirm(Canvas c) {
        // Nokia-style Canvas overlay: no Android AlertDialog or stock
        // "Confirm" heading. The normal C2 softkey bar supplies Yes/No.
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(7, 15, 25));
        c.drawRect(11, 93, 229, 208, p);
        p.setColor(Color.rgb(197, 211, 225));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        c.drawRect(11.5f, 93.5f, 228.5f, 207.5f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(38, 59, 81));
        c.drawRect(13, 96, 227, 119, p);
        p.setColor(Color.WHITE);
        p.setTypeface(fontBold);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(14);
        c.drawText("Question", 21, 113, p);
        p.setTypeface(fontRegular);
        p.setTextSize(15);
        p.setColor(Color.WHITE);
        drawWrappedText(c, confirmText, 22, 145, 194, 18, 3);
    }

    private void drawNotice(Canvas c) {
        // A notice is an opaque foreground box, not see-through list text.
        // drawWrappedText uses x as its LEFT edge: Paint.Align.CENTER with
        // x=48 caused "Assigned: Messaging" to lose its initial letters.
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(20, 25, 27));
        c.drawRoundRect(new RectF(
                NativeOverlayLayout.NOTICE_LEFT, NativeOverlayLayout.NOTICE_TOP,
                NativeOverlayLayout.NOTICE_RIGHT, NativeOverlayLayout.NOTICE_BOTTOM),
                8, 8, p);
        p.setColor(Color.WHITE);
        p.setTypeface(fontRegular);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(NativeOverlayLayout.NOTICE_TEXT_SIZE);
        c.save();
        c.clipRect(NativeOverlayLayout.NOTICE_TEXT_X,
                NativeOverlayLayout.NOTICE_TOP + 4,
                NativeOverlayLayout.NOTICE_TEXT_X + NativeOverlayLayout.NOTICE_TEXT_WIDTH,
                NativeOverlayLayout.NOTICE_BOTTOM - 4);
        // Measure actual glyph widths before drawing either line. Long
        // rejection text wraps fully; exceptional overlong words and
        // messages exceeding two lines end-ellipsize instead of clipping.
        java.util.ArrayList<String> lines = NativeNoticeText.wrap(notice,
                NativeOverlayLayout.NOTICE_TEXT_WIDTH,
                NativeOverlayLayout.NOTICE_MAX_LINES, p::measureText);
        for (int i = 0; i < lines.size(); i++) {
            c.drawText(lines.get(i), NativeOverlayLayout.NOTICE_TEXT_X,
                    NativeOverlayLayout.NOTICE_TEXT_BASELINE +
                            i * NativeOverlayLayout.NOTICE_LINE_HEIGHT, p);
        }
        c.restore();
    }

    private void drawControls(Canvas c) {
        p.setColor(Color.rgb(235,235,235));
        c.drawRoundRect(new RectF(75, 414, 108, 417), 2, 2, p);
        c.drawRoundRect(new RectF(252, 414, 285, 417), 2, 2, p);

        p.setShader(new LinearGradient(0, 432, 0, 515,
                Color.rgb(220,220,220), Color.rgb(95,97,99), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(137, 432, 223, 515), 15, 15, p);
        p.setShader(null);
        p.setColor(Color.rgb(12,14,14));
        c.drawRoundRect(new RectF(146, 441, 214, 506), 11, 11, p);
        p.setColor(Color.rgb(230,230,230));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(8);
        c.drawText("▲", 180, 451, p);
        c.drawText("▼", 180, 499, p);
        c.drawText("◀", 155, 476, p);
        c.drawText("▶", 205, 476, p);
        p.setTextSize(10);
        p.setTypeface(fontBold);
        c.drawText("OK", 180, 477, p);
        p.setTypeface(fontRegular);

        p.setShader(new LinearGradient(0, 453, 0, 491,
                Color.rgb(35,38,38), Color.rgb(5,7,7), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(60, 453, 132, 491), 9, 9, p);
        c.drawRoundRect(new RectF(228, 453, 300, 491), 9, 9, p);
        p.setShader(null);
        p.setTextSize(20);
        p.setColor(Color.rgb(45,190,95));
        c.drawText("☎", 96, 479, p);
        p.setColor(Color.rgb(225,65,75));
        c.drawText("☎", 264, 479, p);

        String[][] labels = {
                {"1","●●"},{"2","abc"},{"3","def"},
                {"4","ghi"},{"5","jkl"},{"6","mno"},
                {"7","pqrs"},{"8","tuv"},{"9","wxyz"},
                {"*","+"},{"0","↻"},{"#","⌂ *"}
        };
        float[] xs = {48, 138, 228};
        float[] ys = {535, 588, 641, 694};
        for (int r = 0; r < 4; r++) {
            for (int col = 0; col < 3; col++) {
                int i = r * 3 + col;
                RectF kr = new RectF(xs[col], ys[r], xs[col] + 84, ys[r] + 45);
                p.setShader(new LinearGradient(0, kr.top, 0, kr.bottom,
                        Color.rgb(31,35,35), Color.rgb(4,6,6), Shader.TileMode.CLAMP));
                c.drawRoundRect(kr, 4, 4, p);
                p.setShader(null);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1);
                p.setColor(Color.rgb(70,76,76));
                c.drawRoundRect(kr, 4, 4, p);
                p.setStyle(Paint.Style.FILL);

                p.setColor(Color.rgb(47,184,233));
                p.setTypeface(fontBold);
                p.setTextSize(16);
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText(labels[i][0], kr.centerX(), kr.top + 26, p);
                p.setTypeface(fontRegular);
                p.setTextSize(8);
                if (i != 0 && i != 9 && i != 10 && i != 11)
                    c.drawText(labels[i][1], kr.centerX() + 20, kr.top + 26, p);
                else
                    c.drawText(labels[i][1], kr.centerX() + 14, kr.top + 37, p);
            }
        }
    }

    private void drawSoftKey(Canvas c, RectF r, boolean left) {
        p.setColor(Color.rgb(42,43,46));
        c.drawRoundRect(r, 11,11,p);
        p.setColor(Color.rgb(210,210,213));
        p.setTextSize(8);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(left ? "—" : "—", r.centerX(), r.centerY() + 3, p);
    }

    private void drawWrappedText(Canvas c, String text, float x, float y, float maxWidth, float lineHeight, int maxLines) {
        if (text == null) text = "";
        String[] paras = text.split("\\n", -1);
        float cy = y;
        int lines = 0;
        for (String para : paras) {
            String[] words = para.split(" ");
            String line = "";
            for (String word : words) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (p.measureText(test) > maxWidth && !line.isEmpty()) {
                    c.drawText(line, x, cy, p);
                    cy += lineHeight;
                    lines++;
                    if (lines >= maxLines) return;
                    line = word;
                } else line = test;
            }
            c.drawText(line, x, cy, p);
            cy += lineHeight;
            lines++;
            if (lines >= maxLines) return;
        }
    }

    private String titleFor(String pg) {
        switch (pg) {
            case "messaging": return "Messaging";
            case "inbox": return "Inbox";
            case "conversations": return "Conversations";
            case "conversation": return "Conversation";
            case "sent": return "Sent items";
            case "drafts": return "Drafts";
            case "outbox": return "Outbox";
            case "savedmessages": return "Saved items";
            case "smsUseDetails": return "Use detail";
            case "reports": return "Delivery reports";
            case "email": return "E-mail";
            case "ims": return "IMs";
            case "voicemessages": return "Voice messages";
            case "infomessages": return "Info messages";
            case "servicecommands": return "Serv. commands";
            case "deletemessages": return "Delete messages";
            case "messagesettings": return "Message settings";
            case "recipientPicker": return "Select contact";
            case "symbolPicker": return "Symbols";
            case "emojiPicker": return "Emoticons";
            case "menuViewPicker": return "Main menu view";
            case "contacts": return "Contacts";
            case "contactsNames": return "Names";
            case "contactSearch": return "Search names";
            case "contactGroups": return "Groups";
            case "groupMembers": {
                NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
                return group == null ? "Group" : group.name;
            }
            case "groupContactPicker": return "Add group member";
            case "groupContactAssign": return "Choose group";
            case "speedDials": return "Speed dials";
            case "speedDialPicker": return "Choose contact";
            case "speedDialAssign": return "Select speed dial";
            case "log": return "Log";
            case "settings": return "Settings";
            case "demoTools": return "Demo data (debug)";
            case "callsettings": return "Call settings";
            case "speedDialMode": return "Speed dialling";
            case "callfilter": return "Call filtering";
            case "blackmode": return "Reject blacklist";
            case "whitemode": return "Whitelist mode";
            case "blackpool": return "Blacklist numbers";
            case "whitepool": return "Whitelist numbers";
            case "aboutNative": return "About";
            case "profiles": return "Profiles";
            case "themes": return "Themes";
            case "connectivity": return "Connectivity";
            case "bluetooth": return "Bluetooth";
            case "gallery": return "Gallery";
            case "musicLibrary": return "Music library";
            case "musicEqualiser": return "Equaliser";
            case "voiceRecorder": return "Voice recorder";
            case "voiceRecordings": return "Recordings";
            case "galleryImages": return "Images";
            case "galleryVideos": return "Video clips";
            case "galleryPreview": return "Image";
            case "gallerySort": return "Sort files";
            case "media": return "Media";
            case "organiser": return "Organiser";
            case "maps": return "Maps";
            case "scientific": return "Scientific";
            case "alarm": return "Alarm clock";
            case "calendar": return "Calendar";
            case "todo": return "To-do list";
            case "notes": return "Notes";
            case "applications": return "Applications";
            case "games": return "Games";
            case "collection": return "Collection";
            case "calllog": return "All calls";
            case "missed": return "Missed calls";
            case "received": return "Received calls";
            case "dialled": return "Dialled numbers";
            case "shortcuts": return "My shortcuts";
            case "shortcutNavigation": return "Navigation key";
            case "shortcutAssign": return "Assign shortcut";
            case "goto": return "Go to";
            case "gotoOrganise": return "Organise shortcuts";
            case "web": return "STORE";
            default: return "GPT c2 from reborn";
        }
    }

    private String installedApkVersion() {
        try {
            return getContext().getPackageManager()
                    .getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String[] itemsFor(String pg) {
        switch (pg) {
            case "messaging":
                return new String[]{"Create message","Conversations","Drafts","Outbox","Sent items","Saved items","Delivery reports","E-mail","IMs","Voice messages","Info messages","Serv. commands","Delete messages","Message settings"};
            case "inbox": {
                if (deviceSmsActive) {
                    String[] a = new String[smsInbox.size()];
                    for (int i = 0; i < a.length; i++) a[i] = contactNameFor(smsInbox.get(i).address);
                    return a;
                }
                return new String[]{"Allow SMS permission"};
            }
            case "conversations": {
                if (deviceSmsActive) {
                    String[] a = new String[smsThreads.size()];
                    for (int i = 0; i < a.length; i++) a[i] = contactNameFor(smsThreads.get(i).address);
                    return a;
                }
                return new String[]{"Allow SMS permission"};
            }
            case "conversation": {
                ArrayList<AndroidBackend.DeviceSms> msgs = conversationMessages();
                String[] a = new String[msgs.size()];
                for (int i = 0; i < msgs.size(); i++) {
                    String body = msgs.get(i).body == null ? "" : msgs.get(i).body.replace("\n", " ");
                    a[i] = body.length() > 28 ? body.substring(0, 28) + "…" : body;
                }
                return a;
            }
            case "sent":
                return sentSmsItems();
            case "drafts": {
                if (drafts.isEmpty()) return new String[]{"(empty)"};
                String[] labels = new String[drafts.size()];
                for (int i = 0; i < labels.length; i++) {
                    String body = drafts.get(i).body.replace("\n", " ").trim();
                    if (body.isEmpty()) body = "(recipient only)";
                    labels[i] = body.length() > 28 ? body.substring(0, 28) + "…" : body;
                }
                return labels;
            }
            case "outbox":
                return new String[]{"(empty)"};
            case "smsUseDetails": {
                String[] labels = new String[smsExtractedDetails.size()];
                for (int i = 0; i < labels.length; i++)
                    labels[i] = smsExtractedDetails.get(i).label();
                return labels;
            }
            case "savedmessages": {
                String[] labels = new String[savedSmsTemplates.size()];
                for (int i = 0; i < labels.length; i++) {
                    String raw = savedSmsTemplates.get(i).replace('\n', ' ').trim();
                    labels[i] = raw.length() > 34 ? raw.substring(0, 34) + "…" : raw;
                }
                return labels;
            }
            case "reports":
                return new String[]{"No verified delivery receipts"};
            case "email":
                return new String[]{"Mailboxes","Create e-mail","E-mail settings"};
            case "ims":
                return new String[]{"Sign in","Saved conversations","IM settings"};
            case "voicemessages":
                return new String[]{"Listen to voice msgs.","Voice mailbox no."};
            case "infomessages":
                return new String[]{"Info service","Topics","Language"};
            case "servicecommands":
                return new String[]{"Service command editor"};
            case "deletemessages":
                return new String[]{"All messages","By folder","Inbox","Sent items","Drafts"};
            case "messagesettings":
                return new String[]{"General settings","Text messages","Multimedia messages","E-mail messages","Service messages"};
            case "recipientPicker": {
                String[] a = new String[contacts.size() + contactGroups.groups.size()];
                for (int i = 0; i < contacts.size(); i++) a[i] = contacts.get(i).name;
                for (int i = 0; i < contactGroups.groups.size(); i++)
                    a[contacts.size() + i] = contactGroups.groups.get(i).name;
                return a;
            }
            case "symbolPicker":
                return new String[]{".",",","?","!","@","#","%","&","(",")","-","+","/",";",":","'"};
            case "emojiPicker":
                return new String[]{"☺","☹","♥","★","♪","✓","☀","☕"};
            case "menuViewPicker":
                return NativeMenuLayout.MODES;
            case "contacts":
                return new String[]{"Names","Add new","Synchronise all","Settings","Groups","Speed dials","Service numbers","Del. all contacts","Move contacts","Copy contacts"};
            case "contactsNames": {
                String[] a = new String[contacts.size()];
                for (int i = 0; i < contacts.size(); i++) a[i] = contacts.get(i).name;
                return a;
            }
            case "contactGroups": {
                if (contactGroups.groups.isEmpty())
                    return new String[]{"(Create a group)"};
                String[] labels = new String[contactGroups.groups.size()];
                for (int i = 0; i < labels.length; i++) {
                    NativeContactGroups.Group group = contactGroups.groups.get(i);
                    labels[i] = group.name + " (" + group.numbers.size() + ")";
                }
                return labels;
            }
            case "groupMembers": {
                NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
                if (group == null || group.numbers.isEmpty())
                    return new String[]{"(No members — Add member)"};
                String[] labels = new String[group.numbers.size()];
                for (int i = 0; i < labels.length; i++)
                    labels[i] = contactNameFor(group.numbers.get(i));
                return labels;
            }
            case "groupContactPicker": {
                if (contacts.isEmpty()) return new String[]{"(No contacts)"};
                String[] labels = new String[contacts.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = contacts.get(i).name;
                return labels;
            }
            case "groupContactAssign": {
                if (contactGroups.groups.isEmpty()) return new String[]{"(Create a group)"};
                String[] labels = new String[contactGroups.groups.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = contactGroups.groups.get(i).name;
                return labels;
            }
            case "speedDials":
            case "speedDialAssign":
                return speedDialSlots();
            case "speedDialPicker": {
                String[] a = new String[contacts.size()];
                for (int i = 0; i < contacts.size(); i++) a[i] = contacts.get(i).name;
                return a;
            }
            case "log":
                return new String[]{"All calls","Missed calls","Received calls","Dialled numbers","Msg. recipients","Clear log lists","Video & voice calls","Internet call durat.","Data counter","Pack. data timer","Message log","Positioning","Sync log","Simulate missed call"};
            case "calllog":
                return callItemsFor(0);
            case "missed":
                return callItemsFor(CallLog.Calls.MISSED_TYPE);
            case "received":
                return callItemsFor(CallLog.Calls.INCOMING_TYPE);
            case "dialled":
                return callItemsFor(CallLog.Calls.OUTGOING_TYPE);
            case "settings":
                if (isDemoToolsAvailable())
                    return new String[]{"Profiles","Themes","Tones","Lights","Display","Date and time","My shortcuts","Sync and backup","Connectivity","Call","Phone","Accessories","Configuration","Security","Rest. factory sett.","Demo data (debug)"};
                return new String[]{"Profiles","Themes","Tones","Lights","Display","Date and time","My shortcuts","Sync and backup","Connectivity","Call","Phone","Accessories","Configuration","Security","Rest. factory sett."};
            case "demoTools":
                return isDemoToolsAvailable() ? new String[]{
                    "Seed / reset sample data",
                    "Clear demo data",
                    "What is included"} : new String[0];
            case "aboutNative":
                return new String[]{"APK version: " + installedApkVersion(),"HTML reference: v6.343","Partial native update","No WebView"};
            case "callsettings":
                return new String[]{"Call type setting","Call divert","Anykey answer","Automatic redial",
                "Video-voice redial","Voice clarity","Speed dialling: " + (speedDiallingEnabled ? "On" : "Off"),
                "Call waiting","Call duration display",
                "Summary after call","Send my caller ID","Send net caller ID","Slide call handling","Call filtering"};
            case "speedDialMode": return new String[]{"Off","On"};
            case "callfilter":
                return new String[]{"Reject unknown callers (pending)",
                  "Reject blacklist: " + (blacklistEnabled ? "On" : "Off"),
                  "Blacklist numbers (" + blacklistNumbers.size() + ")",
                  "Whitelist mode: " + (whitelistEnabled ? "On" : "Off"),
                  "Whitelist numbers (" + whitelistNumbers.size() + ")",
                  "Demo only: not real call blocking"};
            case "blackmode": return new String[]{"Off","On"};
            case "whitemode": return new String[]{"Off","On"};
            case "blackpool": return displayFilterPool(blacklistNumbers);
            case "whitepool": return displayFilterPool(whitelistNumbers);
            case "profiles":
                return new String[]{"General","Silent","Meeting","Outdoor","My style 1","My style 2","Flight"};
            case "themes":
                return new String[]{"Nokia","Blue","Silver","Dark"};
            case "connectivity":
                return new String[]{"Bluetooth","Packet data","USB data cable","Sync and backup"};
            case "bluetooth":
                return new String[]{"Bluetooth","Connect audio acc.","Paired devices","Active devices","My phone's visibility","My phone's name"};
            case "shortcuts":
                return new String[]{"Left selection key","Right selection key","Navigation key","Home screen key"};
            case "shortcutNavigation":
                return new String[]{"Navigation up","Navigation down","Navigation left","Navigation right"};
            case "shortcutAssign":
                return NativeHomeShortcuts.ACTION_NAMES;
            case "goto": {
                ArrayList<Integer> visible = goToShortcuts.enabledIds();
                String[] result = new String[visible.size()];
                for (int i = 0; i < visible.size(); i++)
                    result[i] = NativeGoToShortcuts.NAMES[visible.get(i)];
                return result;
            }
            case "gotoOrganise": {
                String[] result = new String[NativeGoToShortcuts.COUNT];
                for (int i = 0; i < result.length; i++)
                    result[i] = NativeGoToShortcuts.NAMES[goToShortcuts.visualId(i)];
                return result;
            }
            case "gallery":
                return new String[]{"Memory card","Images","Video clips","Music files","Themes","Graphics","Tones","Recordings","Received files"};
            case "voiceRecordings": {
                java.util.ArrayList<java.io.File> files = voiceRecorder.entries();
                if (files.isEmpty()) return new String[]{"(No recordings)"};
                String[] titles = new String[files.size()];
                for (int i = 0; i < titles.length; i++) titles[i] = NativeVoiceRecorder.title(files.get(i));
                return titles;
            }
            case "gallerySort": return new String[]{"Name A–Z","Name Z–A","Newest added"};
            case "galleryImages":
            case "galleryVideos": {
                java.util.List<NativeGalleryStore.Entry> media =
                        galleryStore.entries("galleryVideos".equals(pg));
                if (media.isEmpty()) return new String[]{"(No selected files) — Add from phone"};
                String[] names = new String[media.size()];
                for (int i = 0; i < names.length; i++) names[i] = media.get(i).title;
                return names;
            }
            case "media":
                return new String[]{"Camera","Video camera","Media player","Radio","Voice recorder","Equaliser"};
            case "musicLibrary": {
                if (musicEngine.tracks.isEmpty()) return new String[]{"(Add audio files)"};
                String[] names = new String[musicEngine.tracks.size()];
                for (int i = 0; i < names.length; i++) names[i] = musicEngine.tracks.get(i).name;
                return names;
            }
            case "musicEqualiser":
                return musicEngine.presets();
            case "organiser":
                return new String[]{"Alarm clock","Calendar","Maps","To-do list","Notes","Calculator","Countd. timer","Stopwatch","Dictionary"};
            case "maps":
                return new String[]{"Current position","Find address","Favourites","Route planner"};
            case "scientific":
                return new String[]{"sin","cos","tan","√","x²","log10","1/x","π"};
            case "alarm":
                return new String[]{"Alarm","Alarm time","Repeat","Alarm tone","Snooze time-out"};
            case "calendar":
                return calendar.isEmpty() ? new String[]{"Add note"} : calendar.toArray(new String[0]);
            case "todo":
                return todos.isEmpty() ? new String[]{"Add"} : todos.toArray(new String[0]);
            case "notes":
                return notes.isEmpty() ? new String[]{"Add note"} : notes.toArray(new String[0]);
            case "applications":
                return new String[]{"Games","Collection","Extras","Downloads"};
            case "games":
                return new String[]{"Block'd","Bounce Tales","Diamond Rush","Rally 3D"};
            case "collection":
                return new String[]{"Converter II","Size Converter","World Clock","Opera Mini"};
            case "web":
                return new String[]{"Home","Bookmarks","Go to address","Last web address","Web settings"};
            default:
                return new String[0];
        }
    }

    private void openSelected() {
        if ("menu".equals(page)) {
            if (menuOrganising) {
                moveMainMenuSelection();
                return;
            }
            String[] routes = {"contacts","organiser","media","gallery","messaging","applications","log","settings","web"};
            int destination = NativeMenuLayout.destination(menuOrder, sel);
            if (destination < 0 || destination >= routes.length) {
                showNotice("Menu item unavailable");
                return;
            }
            if (destination == 0) refreshContactsFromPhone();
            if (destination == 4) refreshSmsFromPhone();
            if (destination == 6) refreshCallsFromPhone();
            go(routes[destination]);
            return;
        }
        if ("menuViewPicker".equals(page)) {
            menuView = NativeMenuLayout.MODES[Math.max(0, Math.min(sel, NativeMenuLayout.MODES.length - 1))];
            saveMenuPreferences();
            back();
            showNotice(menuView);
            return;
        }

        if ("messaging".equals(page)) {
            if (sel == 0) {
                recipient = "";
                messageBody = "";
                composeFocus = 0;
                inputMode = "T9";
                go("compose");
            } else {
                String[] routes = {"","conversations","drafts","outbox","sent","savedmessages","reports","email","ims","voicemessages","infomessages","servicecommands","deletemessages","messagesettings"};
                go(routes[Math.max(1, Math.min(sel, routes.length - 1))]);
            }
            return;
        }

        if ("smsUseDetails".equals(page)) {
            showSelectedSmsDetail();
            return;
        }

        if ("inbox".equals(page)) {
            if (!deviceSmsActive) {
                showNotice("Allow SMS permission");
                return;
            }
            if (sel < 0 || sel >= smsInbox.size()) {
                showNotice("No received message selected");
                return;
            }
            inboxIndex = sel;
            activeMessageId = smsInbox.get(sel).id;
            markSmsReadLocally(activeMessageId);
            go("messageRead");
            return;
        }

        if ("conversations".equals(page)) {
            if (deviceSmsActive && sel < smsThreads.size()) {
                activeThreadId = smsThreads.get(sel).threadId;
                // Marks belong only to the conversation in which they were made.
                markedSmsIds.clear();
                // Entering a thread marks every received message read in the
                // app, not just a single message opened later.
                if (setThreadSmsReadStateLocally(activeThreadId, false) > 0)
                    persistSmsReadOverrides();
                go("conversation");
            } else showNotice("SMS permission required");
            return;
        }

        if ("conversation".equals(page)) {
            AndroidBackend.DeviceSms m = conversationMessageAt(sel);
            if (m != null) {
                if (!smsConversationCenterOpens(m)) {
                    recipient = m.address;
                    messageBody = "";
                    composeFocus = 1;
                    go("compose");
                } else {
                    activeMessageId = m.id;
                    markSmsReadLocally(m.id);
                    go("messageRead");
                }
            }
            return;
        }

        if ("recipientPicker".equals(page) && !contacts.isEmpty()) {
            recipient = contacts.get(sel).number;
            back();
            composeFocus = 1;
            showNotice("Recipient added");
            return;
        }

        if ("symbolPicker".equals(page)) {
            messageBody += itemsFor(page)[sel];
            back();
            composeFocus = 1;
            return;
        }

        if ("emojiPicker".equals(page)) {
            messageBody += itemsFor(page)[sel];
            back();
            composeFocus = 1;
            return;
        }

        if ("savedmessages".equals(page)) {
            useSelectedSmsTemplate();
            return;
        }
        if (Arrays.asList("outbox","reports","email","ims","voicemessages","infomessages","servicecommands","deletemessages","messagesettings").contains(page)) {
            detailTitle = itemsFor(page)[sel];
            if ("outbox".equals(page)) detailText = "Outbox records loaded: "
                    + countSmsType(Telephony.Sms.MESSAGE_TYPE_OUTBOX)
                    + "\nActual sending status not verified.";
            else if ("reports".equals(page)) detailText =
                    "Carrier delivery receipts are not tracked by this app.\n"
                    + "A Sent item does not prove delivery.\n"
                    + "No verified delivery reports available.";
            else if ("email".equals(page)) detailText = "E-mail account is not configured.";
            else if ("ims".equals(page)) detailText = "Instant messaging service is offline.";
            else if ("voicemessages".equals(page)) detailText = "Voice mailbox service.";
            else if ("infomessages".equals(page)) detailText = "Network information message settings.";
            else if ("servicecommands".equals(page)) detailText = "Enter a service command for the network.";
            else if ("deletemessages".equals(page)) detailText =
                    "Bulk system SMS deletion is not implemented.\nPhone SMS records are unchanged.";
            else if ("messagesettings".equals(page)) detailText = "Messaging configuration.";
            else detailText = "Saved message.";
            go("itemDetail");
            return;
        }

        if ("sent".equals(page)) {
            AndroidBackend.DeviceSms selectedSent = sentMessageAt(sel);
            if (selectedSent != null) {
                activeMessageId = selectedSent.id;
                go("messageRead");
            } else showNotice("No sent message selected");
            return;
        }

        if ("drafts".equals(page)) {
            if (!drafts.isEmpty() && sel >= 0 && sel < drafts.size()) {
                int selectedIndex = sel;
                DraftMessage draft = drafts.get(selectedIndex);
                messageBody = draft.body;
                recipient = draft.recipient;
                inputMode = draft.inputMode;
                composeFocus = recipient.isEmpty() ? 0 : 1;
                go("compose");
                editingDraftIndex = selectedIndex;
            }
            return;
        }

        if ("contacts".equals(page)) {
            if (sel == 0) {
                refreshContactsFromPhone();
                go("contactsNames");
            } else if (sel == 1) beginContactEdit(-1);
            else if (sel == 2) {
                refreshContactsFromPhone();
                showNotice(deviceContactsActive
                        ? "Android contacts refreshed. Cloud sync not performed."
                        : "Android Contacts permission unavailable; no cloud sync");
            } else if (sel == 5) {
                go("speedDials");
            } else if (sel == 4) {
                go("contactGroups");
            } else {
                detailTitle = itemsFor(page)[sel];
                if (sel == 6) detailText = "SIM service numbers are not available from this APK.";
                else if (sel == 7) detailText = "Delete all contacts is not implemented.\nNo contacts changed.";
                else detailText = "This contact option is not yet implemented.\nNo data changed.";
                go("itemDetail");
            }
            return;
        }

        if ("contactGroups".equals(page)) {
            NativeContactGroups.Group group = contactGroups.at(sel);
            if (group == null) beginGroupEdit(false);
            else {
                activeContactGroupId = group.id;
                go("groupMembers");
            }
            return;
        }
        if ("groupMembers".equals(page)) {
            NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
            if (group == null) {
                showNotice("Group unavailable");
            } else if (sel >= group.numbers.size()) {
                openGroupMemberPicker();
            } else {
                String number = group.numbers.get(sel);
                detailTitle = "Group member";
                detailText = "Name: " + contactNameFor(number) +
                        "\nNumber: " + number +
                        "\nMembership stored in this app only";
                go("itemDetail");
            }
            return;
        }
        if ("groupContactPicker".equals(page)) {
            if (sel >= 0 && sel < contacts.size()) {
                NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
                if (group == null) {
                    showNotice("Group unavailable");
                } else {
                    String number = contacts.get(sel).number;
                    if (contactGroups.addMember(group.id, number)) {
                        back();
                        if ("groupMembers".equals(page))
                            sel = group.numbers.size() - 1;
                        showNotice("Member added in app");
                    } else showNotice("Already assigned, invalid or group full");
                }
            } else showNotice("No contacts to add");
            return;
        }
        if ("groupContactAssign".equals(page)) {
            NativeContactGroups.Group group = contactGroups.at(sel);
            if (group == null) {
                showNotice("Create a group first");
            } else if (contactGroups.addMember(group.id, pendingGroupMemberNumber)) {
                back();
                showNotice("Added to " + group.name + " in app");
            } else showNotice("Already a member, invalid or group full");
            return;
        }

        if ("speedDials".equals(page)) {
            speedDialSlot = sel + 2;
            if (speedDialNumbers.containsKey(speedDialSlot))
                showSpeedDialDetails(speedDialSlot);
            else if (contacts.isEmpty()) showNotice("Add a contact first");
            else go("speedDialPicker");
            return;
        }
        if ("speedDialAssign".equals(page)) {
            int slot = sel + 2;
            assignSpeedDial(slot, speedDialAssignment);
            back();
            return;
        }
        if ("speedDialPicker".equals(page)) {
            if (sel >= 0 && sel < contacts.size()) {
                assignSpeedDial(speedDialSlot, contacts.get(sel).number);
                back();
                if ("speedDials".equals(page)) sel = speedDialSlot - 2;
            } else showNotice("Add a contact first");
            return;
        }

        if ("contactsNames".equals(page)) {
            if (sel >= contacts.size()) {
                NativeContactGroups.Group group = contactGroups.at(sel - contacts.size());
                if (group != null) {
                    activeContactGroupId = group.id;
                    go("groupMembers");
                } else showNotice("Group unavailable");
            } else if (sel >= 0 && sel < contacts.size()) {
                contactIndex = sel;
                go("contactDetail"); // Starts on the contact's primary number.
            } else showNotice("No contact selected");
            return;
        }
        if ("contactSearch".equals(page)) {
            openContactSearchResult();
            return;
        }

        if ("log".equals(page)) {
            String[] routes = {"calllog","missed","received","dialled"};
            if (sel >= 0 && sel < 4) go(routes[sel]);
            else if (sel == 10) showSmsMessageLog();
            else {
                detailTitle = itemsFor(page)[Math.max(0, Math.min(sel, itemsFor(page).length - 1))];
                detailText = "Not implemented in this native APK.";
                go("itemDetail");
            }
            return;
        }

        if (Arrays.asList("calllog","missed","received","dialled").contains(page)) {
            showCallDetails(page, sel);
            return;
        }

        if ("demoTools".equals(page)) {
            if (!isDemoToolsAvailable()) {
                showNotice("Debug tools unavailable");
                return;
            }
            if (sel == 0) changeAuditDemoMode(true);
            else if (sel == 1) requestConfirm("clearAuditDemo",
                    "Clear all fictional contacts and SMS and return to your phone?");
            else showAuditDemoDetails();
            return;
        }

        if ("settings".equals(page)) {
            if (sel == 15 && isDemoToolsAvailable()) go("demoTools");
            else if (sel == 0) go("profiles");
            else if (sel == 1) go("themes");
            else if (sel == 6) go("shortcuts");
            else if (sel == 7) {
                detailTitle = "Sync and backup";
                detailText = "Phone switch\nCreate backup\nRestore backup\nData transfer\nSynchronise all";
                go("itemDetail");
            }
            else if (sel == 8) go("connectivity");
            else if (sel == 9) go("callsettings");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = sel == 10
                        ? "Phone settings are not yet implemented in this native recreation.\n"
                            + "Your Android phone settings remain unchanged."
                        : "This setting is not yet implemented in this native recreation.\n"
                            + "No Android system settings were changed.";
                go("itemDetail");
            }
            return;
        }

        if ("callsettings".equals(page)) {
            if (sel == 13) go("callfilter");
            else if (sel == 6) {
                go("speedDialMode");
                sel = speedDiallingEnabled ? 1 : 0;
            } else showNotice("Not implemented in native APK");
            return;
        }
        if ("speedDialMode".equals(page)) {
            speedDiallingEnabled = sel == 1;
            prefs.edit().putBoolean("speed_dialling_enabled_v1", speedDiallingEnabled).apply();
            back();
            showNotice("Speed dialling " + (speedDiallingEnabled ? "On" : "Off"));
            return;
        }
        if ("callfilter".equals(page)) {
            if (sel == 1) { go("blackmode"); sel = blacklistEnabled ? 1 : 0; }
            else if (sel == 2) go("blackpool");
            else if (sel == 3) { go("whitemode"); sel = whitelistEnabled ? 1 : 0; }
            else if (sel == 4) go("whitepool");
            else if (sel == 0) showNotice("Unknown callers: definition pending");
            else showNotice("Real Android calls are not blocked");
            return;
        }
        if ("blackmode".equals(page) || "whitemode".equals(page)) {
            if ("blackmode".equals(page)) blacklistEnabled = sel == 1;
            else whitelistEnabled = sel == 1;
            saveFilterSettings();
            back();
            return;
        }
        if ("blackpool".equals(page) || "whitepool".equals(page)) {
            ArrayList<String> pool = "blackpool".equals(page) ? blacklistNumbers : whitelistNumbers;
            if (!pool.isEmpty()) requestConfirm("removeFromFilter", "Remove " + pool.get(Math.min(sel,pool.size()-1)) + "?");
            return;
        }
        if ("aboutNative".equals(page)) { showNotice("Native APK " + installedApkVersion()); return; }

        if ("profiles".equals(page)) {
            profile = itemsFor(page)[sel];
            saveSettings();
            showNotice(profile + " activated");
            invalidate();
            return;
        }

        if ("themes".equals(page)) {
            theme = itemsFor(page)[sel];
            saveSettings();
            showNotice(theme + " selected");
            invalidate();
            return;
        }

        if ("connectivity".equals(page)) {
            if (sel == 0) go("bluetooth");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = "Connectivity simulation.";
                go("itemDetail");
            }
            return;
        }

        if ("bluetooth".equals(page)) {
            if (sel == 0) {
                bluetooth = !bluetooth;
                saveSettings();
                showNotice("Bluetooth " + (bluetooth ? "on" : "off"));
            } else {
                detailTitle = itemsFor(page)[sel];
                detailText = bluetooth ? "No devices found." : "Switch Bluetooth on first.";
                go("itemDetail");
            }
            return;
        }

        if ("gallery".equals(page)) {
            if (sel == 1 || sel == 2) openGalleryFolder(sel == 2);
            else if (sel == 3) go("musicLibrary");
            else if (sel == 7) go("voiceRecordings");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = sel == 0
                        ? "Memory card status cannot be determined here.\nNo storage amounts invented."
                        : "Not yet implemented in this native Gallery.\nImages and Video clips can be selected from Android.";
                go("itemDetail");
            }
            return;
        }

        if ("voiceRecordings".equals(page)) {
            playSelectedVoiceNote();
            return;
        }

        if ("musicLibrary".equals(page)) {
            if (musicEngine.tracks.isEmpty()) pickAudioFiles();
            else {
                if (musicEngine.open(sel, true)) go("music");
                else showNotice(musicEngine.error());
            }
            return;
        }
        if ("musicEqualiser".equals(page)) {
            if (musicEngine.choosePreset(sel)) {
                showNotice("Equaliser setting applied");
                back();
            } else showNotice(musicEngine.error());
            return;
        }

        if ("gallerySort".equals(page)) {
            galleryStore.sort(gallerySortVideos, sel);
            back();
            showNotice("Sort order updated");
            return;
        }

        if ("galleryImages".equals(page) || "galleryVideos".equals(page)) {
            openSelectedGalleryEntry();
            return;
        }

        if ("media".equals(page)) {
            if (sel == 2) go("music");
            else if (sel == 5) openMusicEqualiser();
            else if (sel == 4) go("voiceRecorder");
            else if (sel == 0 || sel == 1) {
                startCameraCapture(sel == 1);
            } else if (sel == 3) {
                detailTitle = "Radio";
                detailText = "FM tuner access is unavailable on this Android device.\nOriginal C2-01 required a wired headset.";
                go("itemDetail");
            } else {
                detailTitle = itemsFor(page)[sel];
                detailText = "Not yet implemented in this native Media screen.";
                go("itemDetail");
            }
            return;
        }

        if ("organiser".equals(page)) {
            if (sel == 0) go("alarm");
            else if (sel == 1) go("calendar");
            else if (sel == 2) go("maps");
            else if (sel == 3) go("todo");
            else if (sel == 4) go("notes");
            else if (sel == 5) go("calculator");
            else if (sel == 6) go("countdown");
            else if (sel == 7) go("stopwatch");
            else if (sel == 8) {
                detailTitle = "Dictionary";
                detailText = "Dictionary is not yet implemented in this native port.";
                go("itemDetail");
            }
            return;
        }

        if ("maps".equals(page)) {
            detailTitle = itemsFor(page)[sel];
            detailText = "Maps simulation\nNo network connection required for this native recreation.";
            go("itemDetail");
            return;
        }

        if ("scientific".equals(page)) {
            applyScientific(sel);
            return;
        }

        if ("alarm".equals(page)) {
            if (sel == 0) {
                alarmOn = !alarmOn;
                saveSettings();
                showNotice("Alarm " + (alarmOn ? "on" : "off"));
            } else if (sel == 1) {
                editBuffer = alarmTime.replace(":", "");
                go("alarmEdit");
            } else if (sel == 2) {
                alarmRepeat = !alarmRepeat;
                saveSettings();
                showNotice("Repeat " + (alarmRepeat ? "on" : "off"));
            } else {
                detailTitle = itemsFor(page)[sel];
                detailText = sel == 3 ? "Nokia tune" : "5 minutes";
                go("itemDetail");
            }
            return;
        }

        if ("calendar".equals(page)) {
            if (calendar.isEmpty()) beginTextEdit("calendar", -1, "");
            else {
                detailTitle = "Calendar note";
                detailText = calendar.get(sel);
                go("itemDetail");
            }
            return;
        }

        if ("todo".equals(page)) {
            if (todos.isEmpty()) beginTextEdit("todo", -1, "");
            else beginTextEdit("todo", sel, todos.get(sel));
            return;
        }

        if ("notes".equals(page)) {
            if (notes.isEmpty()) beginTextEdit("note", -1, "");
            else beginTextEdit("note", sel, notes.get(sel));
            return;
        }

        if ("applications".equals(page)) {
            if (sel == 0) go("games");
            else if (sel == 1) go("collection");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = "Application catalogue.";
                go("itemDetail");
            }
            return;
        }

        if ("games".equals(page) || "collection".equals(page)) {
            detailTitle = itemsFor(page)[sel];
            detailText = "Version 1.0\nJava application from the C2-01 era.";
            go("itemDetail");
            return;
        }

        if ("web".equals(page)) {
            if (sel == 0) {
                browserUrl = "http://www.nokia.com/";
                go("browser");
            } else if (sel == 1) {
                browserUrl = "about:bookmarks";
                go("browser");
            } else if (sel == 2) {
                beginTextEdit("url", -1, "http://");
            } else if (sel == 3) go("browser");
            else {
                detailTitle = "Web settings";
                detailText = "Configuration, appearance, cookies and cache.";
                go("itemDetail");
            }
            return;
        }

        if ("shortcuts".equals(page)) {
            if (sel == 2) go("shortcutNavigation");
            else startHomeShortcutAssignment(sel == 0 ? NativeHomeShortcuts.LSK :
                    sel == 1 ? NativeHomeShortcuts.RSK : NativeHomeShortcuts.CENTRE);
            return;
        }
        if ("shortcutNavigation".equals(page)) {
            int slot = NativeHomeShortcuts.navigationSlot(sel);
            if (slot >= 0) startHomeShortcutAssignment(slot);
            else showNotice("Unknown navigation key");
            return;
        }
        if ("shortcutAssign".equals(page)) {
            if (!homeShortcuts.assign(shortcutAssignmentSlot, sel)) {
                showNotice("Keep Menu on at least one home key");
                return;
            }
            saveHomeShortcuts();
            String actionLabel = homeShortcuts.label(shortcutAssignmentSlot);
            back();
            showNotice("Assigned: " + actionLabel);
            return;
        }

        if ("goto".equals(page)) {
            // Selection is a VISIBLE position. Resolve to the stable shortcut
            // ID before performing an action, so reordered items cannot
            // accidentally launch the wrong feature.
            openGoToShortcut(goToShortcuts.visibleId(sel));
            return;
        }
        if ("gotoOrganise".equals(page)) {
            toggleGoToOrganiseSelection();
            return;
        }

        String[] items = itemsFor(page);
        if (items.length > 0) {
            detailTitle = items[sel];
            detailText = "Details";
            go("itemDetail");
        }
    }

    private void showOrganiserMemory(String title, List<String> values) {
        int characters = 0;
        for (String item : values) characters += item == null ? 0 : item.length();
        detailTitle = title + " memory";
        detailText = "Saved entries: " + values.size()
                + "\nText characters: " + characters
                + "\nStorage: App data\nDevice storage capacity unknown";
        go("itemDetail");
    }

    private void beginTextEdit(String kind, int index, String text) {
        editKind = kind;
        editIndex = index;
        editBuffer = text == null ? "" : text;
        editField = 0;
        inputMode = "Abc";
        go("edit");
    }

    private void beginGroupEdit(boolean rename) {
        NativeContactGroups.Group group =
                rename ? contactGroups.byId(activeContactGroupId) : null;
        if (rename && group == null) {
            showNotice("Group unavailable");
            return;
        }
        editKind = rename ? "groupRename" : "groupNew";
        editIndex = -1;
        editField = 0;
        editBuffer = rename ? group.name : "";
        inputMode = "abc";
        go("edit");
    }

    private void openGroupMemberPicker() {
        refreshContactsFromPhone();
        if (contacts.isEmpty()) {
            showNotice("No contacts to add");
            return;
        }
        go("groupContactPicker");
    }

    private String selectedGroupMemberNumber() {
        NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
        return group == null || sel < 0 || sel >= group.numbers.size()
                ? null : group.numbers.get(sel);
    }

    private void showGroupDetails() {
        NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
        if (group == null) {
            showNotice("Group unavailable");
            return;
        }
        detailTitle = "Group details";
        detailText = "Name: " + group.name
                + "\nMembers: " + group.numbers.size()
                + "\nStored in this APK only"
                + "\nAndroid Contacts groups unchanged";
        go("itemDetail");
    }

    private void beginContactEdit(int index) {
        editKind = "contact";
        editIndex = index;
        editTargetRawId = -1L;
        editTargetDataId = -1L;
        editTargetNumber = "";
        if (index >= 0 && index < contacts.size()) {
            Contact ct = contacts.get(index);
            String chosenNumber = "contactDetail".equals(page) ?
                    selectedContactNumber() : ct.number;
            if (chosenNumber == null) chosenNumber = ct.number;
            editTargetNumber = chosenNumber;
            ArrayList<String> nums = contactNumbersById.get(ct.contactId);
            ArrayList<Long> raws = contactNumberRawIds.get(ct.contactId);
            ArrayList<Long> ids = contactNumberDataIds.get(ct.contactId);
            if (nums != null && raws != null && ids != null) {
                for (int j = 0; j < nums.size() && j < raws.size() && j < ids.size(); j++) {
                    if (nums.get(j).equals(chosenNumber)) {
                        editTargetRawId = raws.get(j);
                        editTargetDataId = ids.get(j);
                        break;
                    }
                }
            }
            if (editTargetDataId <= 0 && chosenNumber.equals(ct.number)) {
                editTargetRawId = ct.rawContactId;
                editTargetDataId = ct.phoneDataId;
            }
            editBuffer = ct.name + "\n" + chosenNumber;
        } else editBuffer = "\n";
        editField = 0;
        inputMode = "Abc";
        go("edit");
    }

    private void saveEdit() {
        if (demoMode && "contact".equals(editKind)) {
            showNotice("DEMO: Contacts are read-only");
            return;
        }
        if ("alarmEdit".equals(page)) {
            if (editBuffer.length() == 4) {
                try {
                    int h = Integer.parseInt(editBuffer.substring(0,2));
                    int m = Integer.parseInt(editBuffer.substring(2,4));
                    if (h < 24 && m < 60) {
                        alarmTime = editBuffer.substring(0,2) + ":" + editBuffer.substring(2,4);
                        alarmOn = true;
                        saveSettings();
                        back();
                        showNotice("Alarm set");
                        return;
                    }
                } catch (Exception ignored) {}
            }
            showNotice("Invalid time");
            return;
        }

        String txt = editBuffer.trim();
        if ("contact".equals(editKind)) {
            String[] parts = editBuffer.split("\\n", -1);
            String name = parts.length > 0 ? parts[0].trim() : "";
            String num = parts.length > 1 ? parts[1].trim() : "";
            if (name.isEmpty() || num.isEmpty()) {
                showNotice("Enter name and number");
                return;
            }
            if (deviceContactsActive) {
                boolean ok;
                if (editIndex >= 0 && editIndex < contacts.size()) {
                    Contact old = contacts.get(editIndex);
                    if (editTargetRawId <= 0 || editTargetDataId <= 0) {
                        showNotice("Selected number cannot be edited");
                        return;
                    }
                    // Name-only edits should not rewrite an unchanged phone row.
                    // That extra provider operation could fail the entire batch.
                    ok = num.equals(editTargetNumber)
                            ? backend.updateContactName(editTargetRawId, name)
                            : backend.updateContact(old.contactId,
                                    editTargetRawId, editTargetDataId, name, num);
                } else {
                    ok = backend.addContact(name, num);
                }
                if (!ok) {
                    showNotice("Contact save failed");
                    return;
                }
                refreshContactsFromPhone();
            } else {
                if (editIndex >= 0 && editIndex < contacts.size()) contacts.set(editIndex, new Contact(name, num));
                else contacts.add(0, new Contact(name, num));
                saveContacts();
            }
            if (editIndex >= 0 && editIndex < contacts.size()) {
                backTo("contactDetail");
                selectContactNumber(num);
            } else backTo("contactsNames");
            showNotice("Contact saved");
            return;
        }

        if (txt.isEmpty()) {
            showNotice("Enter text");
            return;
        }

        if ("groupNew".equals(editKind)) {
            NativeContactGroups.Group group = contactGroups.create(txt);
            if (group == null) {
                showNotice("Name invalid/duplicate or group limit reached (30)");
                return;
            }
            activeContactGroupId = group.id;
            backTo("contactGroups");
            sel = Math.max(0, contactGroups.indexOf(group.id));
            showNotice("Group created in app");
            return;
        }
        if ("groupRename".equals(editKind)) {
            if (!contactGroups.rename(activeContactGroupId, txt)) {
                showNotice("Name invalid or already used");
                return;
            }
            backTo("groupMembers");
            showNotice("Group renamed in app");
            return;
        }

        if ("note".equals(editKind)) {
            if (editIndex >= 0 && editIndex < notes.size()) notes.set(editIndex, txt);
            else notes.add(0, txt);
            saveStringList("notes", notes);
            backTo("notes");
        } else if ("todo".equals(editKind)) {
            if (editIndex >= 0 && editIndex < todos.size()) todos.set(editIndex, txt);
            else todos.add(0, txt);
            saveStringList("todos", todos);
            backTo("todo");
        } else if ("calendar".equals(editKind)) {
            if (editIndex >= 0 && editIndex < calendar.size()) calendar.set(editIndex, txt);
            else calendar.add(0, txt);
            saveStringList("calendar", calendar);
            backTo("calendar");
        } else if ("url".equals(editKind)) {
            browserUrl = txt;
            backTo("web");
            go("browser");
        } else back();

        showNotice("Saved");
    }

    private void backTo(String target) {
        while (!history.isEmpty()) {
            PageState ps = history.pop();
            if (ps.page.equals(target)) {
                page = ps.page;
                sel = Math.min(ps.sel, Math.max(0, itemsFor(page).length - 1));
                optionsOpen = false;
                invalidate();
                return;
            }
        }
        page = target;
        sel = 0;
        if ("conversation".equals(target)) conversationScrollY = 0f;
        optionsOpen = false;
        invalidate();
    }

    public void onTelecomCallUpdated() {
        if (!C2InCallService.hasCall()) return;
        String number = C2InCallService.number();
        if (!number.isEmpty()) activeCall = number;
        // Telecom may supply a normalized number that differs from the dial
        // string; refresh provider-backed contacts before rendering caller ID.
        if (backend.has(Manifest.permission.READ_CONTACTS)) refreshContactsFromPhone();
        if (!"call".equals(page)) go("call");
        invalidate();
    }

    public void onTelecomCallEnded() {
        if (!"call".equals(page)) return;
        activeCall = "";
        callStarted = 0;
        loudspeaker = false;
        history.clear();
        page = "home";
        sel = 0;
        showNotice("Call ended");
        postDelayed(this::refreshCallsFromPhone, 700);
        invalidate();
    }

    public void onDialerRoleResult(boolean granted) {
        showNotice(granted ? "Phone app enabled - dial again" :
                "Set C2 as default Phone app to call");
    }

    public void onDialerRoleUnavailable() {
        showNotice("Default phone role unavailable");
    }

    private void startCall(String who) {
        if (demoMode) {
            showNotice("DEMO: Real calls are disabled");
            return;
        }
        if (who == null || who.trim().isEmpty()) {
            showNotice("Enter number");
            return;
        }
        MainActivity activity = getContext() instanceof MainActivity ?
                (MainActivity) getContext() : null;
        if (activity == null || !activity.hasDialerRole()) {
            showNotice("C2 must be default Phone app");
            if (activity != null) activity.requestDialerRole();
            return;
        }
        if (!backend.has(Manifest.permission.CALL_PHONE)) {
            showNotice("Allow Phone permission");
            return;
        }
        activeCall = who;
        callStarted = SystemClock.elapsedRealtime();
        loudspeaker = false;
        if (!backend.placeCall(who)) {
            activeCall = "";
            callStarted = 0;
            showNotice("Call could not be placed");
            return;
        }
        go("call");
    }

    private void toggleSpeaker() {
        if (!C2InCallService.hasCall()) {
            showNotice("Call not connected");
            return;
        }
        loudspeaker = !loudspeaker;
        C2InCallService.speaker(loudspeaker);
        showNotice(loudspeaker ? "Loudspeaker on" : "Loudspeaker off");
    }

    private void endCall() {
        if (!C2InCallService.hasCall() || !C2InCallService.hangup()) {
            showNotice("No active call to end");
            return;
        }
        // Wait for Telecom's onCallRemoved before exiting the C2 call page.
        showNotice("Ending call");
    }

    private void sendMessage() {
        if (demoMode) {
            showNotice("DEMO: Sending real SMS is disabled");
            return;
        }
        if (recipient.trim().isEmpty()) {
            composeFocus = 0;
            showNotice("Enter recipient");
            return;
        }
        if (messageBody.trim().isEmpty()) {
            composeFocus = 1;
            showNotice("Write message");
            return;
        }
        if (!backend.sendSms(recipient.trim(), messageBody)) {
            showNotice("SMS permission required");
            return;
        }
        if (editingDraftIndex >= 0 && editingDraftIndex < drafts.size()) {
            drafts.remove(editingDraftIndex);
            saveDrafts();
        }
        editingDraftIndex = -1;
        history.clear();
        page = "home";
        sel = 0;
        showNotice("SMS submitted; delivery unconfirmed");
        recipient = "";
        messageBody = "";
        postDelayed(this::refreshSmsFromPhone, 1200);
        invalidate();
    }

    private void saveDraftAndBack() {
        saveCurrentDraft();
        back();
    }

    private void go(String target) {
        if ("compose".equals(target)) editingDraftIndex = -1;
        if (("cameraViewfinder".equals(page) || "videoViewfinder".equals(page)) &&
                !"cameraViewfinder".equals(target) && !"videoViewfinder".equals(target) &&
                getContext() instanceof MainActivity)
            ((MainActivity) getContext()).showNativeCameraPreview(false);
        history.push(new PageState(page, sel));
        page = target;
        sel = 0;
        optionsOpen = false;
        resetTap();
        invalidate();
    }

    private void back() {
        if (("cameraViewfinder".equals(page) || "videoViewfinder".equals(page)) &&
                getContext() instanceof MainActivity)
            ((MainActivity) getContext()).showNativeCameraPreview(false);
        if ("music".equals(page)) musicEngine.pause();
        if (voiceRecorder.isRecording() && "voiceRecorder".equals(page))
            finishVoiceRecording();
        if ("voiceRecordings".equals(page)) voiceRecorder.stopPlayback();
        int previewTargetIndex = "galleryPreview".equals(page) ? galleryPreviewIndex : -1;
        if ("galleryPreview".equals(page)) clearGalleryPreview();
        resetTap();
        optionsOpen = false;
        if ("compose".equals(page) && (!messageBody.trim().isEmpty() || !recipient.trim().isEmpty())) {
            saveDraftAndBackPlain();
            return;
        }
        if (!history.isEmpty()) {
            PageState ps = history.pop();
            page = ps.page;
            sel = ps.sel;
        } else {
            page = "home";
            sel = 0;
        }
        if (previewTargetIndex >= 0 && "galleryImages".equals(page))
            sel = Math.min(previewTargetIndex, Math.max(0, galleryStore.images.size() - 1));
        if ("goto".equals(page))
            sel = Math.max(0, Math.min(sel, goToShortcuts.enabledCount() - 1));
        invalidate();
    }

    private void saveDraftAndBackPlain() {
        saveCurrentDraft();
        if (!history.isEmpty()) {
            PageState ps = history.pop();
            page = ps.page;
            sel = ps.sel;
        } else {
            page = "home";
            sel = 0;
        }
        invalidate();
    }

    public boolean handleAndroidBack() {
        if (videoSaving()) return true;
        if (videoRecording()) {
            ((MainActivity) getContext()).finishNativeVideoCapture();
            return true;
        }
        if ("menu".equals(page) && menuOrganising) {
            if (menuMovingFrom >= 0) menuMovingFrom = -1;
            else menuOrganising = false;
            invalidate();
            return true;
        }
        if (optionsOpen) {
            optionsOpen = false;
            optionSubmenuTitle = "";
            syncCameraPreviewOverlay();
            invalidate();
            return true;
        }
        if ("call".equals(page) && C2InCallService.hasCall()) {
            // Android Back must not leave a live call stranded behind the UI.
            showNotice("Press End to finish call");
            return true;
        }
        if (!"home".equals(page)) {
            back();
            return true;
        }
        return false;
    }

    private void press(String key) {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if ("videoViewfinder".equals(page) && videoSaving()) {
            showNotice("Saving video...");
            return;
        }

        if (confirmOpen) {
            if ("LSK".equals(key) || "OK".equals(key)) executeConfirmed();
            else if ("RSK".equals(key) || "END".equals(key)) {
                if ("restoreGoTo".equals(confirmAction))
                    auditGoTo("restore canceled NO; unchanged");
                confirmOpen = false;
                confirmAction = "";
                confirmText = "";
                invalidate();
            }
            return;
        }

        if (locked) {
            if (unlockStep == 0 && "LSK".equals(key)) {
                unlockStep = 1;
                showNotice("Now press *");
            } else if (unlockStep == 1 && "*".equals(key)) {
                locked = false;
                unlockStep = 0;
                showNotice("Keypad unlocked");
            } else {
                unlockStep = 0;
                showNotice("Keypad locked");
            }
            invalidate();
            return;
        }

        if (optionsOpen) {
            if ("UP".equals(key)) optionSel = (optionSel - 1 + optionItems.length) % optionItems.length;
            else if ("DOWN".equals(key)) optionSel = (optionSel + 1) % optionItems.length;
            else if ("OK".equals(key) || "LSK".equals(key)) chooseOption();
            else if ("RSK".equals(key) || "END".equals(key)) {
                optionsOpen = false;
                optionSubmenuTitle = "";
                syncCameraPreviewOverlay();
            }
            invalidate();
            return;
        }

        if ("END".equals(key)) {
            if (videoRecording()) {
                ((MainActivity) getContext()).finishNativeVideoCapture();
                return;
            }
            if ("call".equals(page)) endCall();
            else {
                musicEngine.pause();
                if (voiceRecorder.isRecording()) finishVoiceRecording();
                voiceRecorder.stopPlayback();
                if ("galleryPreview".equals(page)) clearGalleryPreview();
                if ("compose".equals(page)) saveCurrentDraft();
                history.clear();
                page = "home";
                sel = 0;
                menuOrganising = false;
                menuMovingFrom = -1;
                dial = "";
                invalidate();
            }
            return;
        }

        if ("CALL".equals(key)) {
            // This global Call-key handler runs before page-specific routing.
            // Answer ringing calls here; otherwise the call-screen handler
            // never receives the green hardware key.
            if ("call".equals(page) && C2InCallService.isRinging()) {
                if (!C2InCallService.answer()) showNotice("Unable to answer call");
            } else if ("dial".equals(page)) startCall(dial);
            else if ("contactDetail".equals(page)) callSelectedContactNumber();
            else if ("speedDials".equals(page)) {
                String number = speedDialNumbers.get(sel + 2);
                if (number != null) startCall(number);
                else showNotice("No speed dial assigned");
            } else if ("groupMembers".equals(page)) {
                String number = selectedGroupMemberNumber();
                if (number != null) startCall(number);
                else showNotice("Select a group member first");
            }
            else if ("conversation".equals(page)) {
                AndroidBackend.DeviceSms m = conversationMessageAt(sel);
                if (m != null) startCall(m.address);
            }
            else if ("home".equals(page)) go("dialled");
            else if ("contactSearch".equals(page)) {
                Contact found = selectedContactSearchResult();
                if (found != null) startCall(found.number);
                else showNotice("No matching contact");
            }
            else if (Arrays.asList("calllog","missed","received","dialled").contains(page)) {
                startCall(callNumberAt(page, sel));
            }
            return;
        }

        if ("home".equals(page)) {
            if ("OK".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.CENTRE));
            else if ("LSK".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.LSK));
            else if ("RSK".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.RSK));
            else if ("UP".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.UP));
            else if ("DOWN".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.DOWN));
            else if ("LEFT".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.LEFT));
            else if ("RIGHT".equals(key)) openAssignedHomeAction(
                    homeShortcuts.assigned(NativeHomeShortcuts.RIGHT));
            else if (isNumberKey(key) && !"#".equals(key)) {
                dial = "*".equals(key) ? "*" : key;
                go("dial");
            }
            return;
        }

        if ("menu".equals(page)) {
            if ("UP".equals(key) || "DOWN".equals(key) ||
                    "LEFT".equals(key) || "RIGHT".equals(key))
                sel = NativeMenuLayout.navigate(sel, key, menuView);
            else if ("OK".equals(key)) openSelected();
            else if ("RSK".equals(key)) {
                if (menuOrganising) {
                    if (menuMovingFrom >= 0) {
                        menuMovingFrom = -1;
                        showNotice("Move cancelled");
                    } else {
                        menuOrganising = false;
                        showNotice("Menu organised");
                    }
                } else {
                    history.clear();
                    page = "home";
                    sel = 0;
                }
            } else if ("LSK".equals(key)) {
                if (menuOrganising) moveMainMenuSelection();
                else openOptionsForPage();
            }
            invalidate();
            return;
        }

        if ("compose".equals(page)) {
            handleComposeKey(key);
            return;
        }

        if ("edit".equals(page)) {
            handleEditKey(key);
            return;
        }

        if ("alarmEdit".equals(page)) {
            if (isDigit(key) && editBuffer.length() < 4) editBuffer += key;
            else if ("LSK".equals(key)) saveEdit();
            else if ("RSK".equals(key)) {
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                else back();
            } else if ("OK".equals(key)) saveEdit();
            invalidate();
            return;
        }

        if ("dial".equals(page)) {
            if (isDigit(key) || "*".equals(key) || "#".equals(key)) {
                if (dial.length() < 24) dial += key;
            } else if ("OK".equals(key) || "CALL".equals(key)) startCall(dial);
            else if ("RSK".equals(key)) {
                if (!dial.isEmpty()) dial = dial.substring(0, dial.length() - 1);
                else back();
            } else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("loan".equals(page)) {
            handleLoanKey(key);
            return;
        }

        if ("call".equals(page)) {
            if ("CALL".equals(key) && C2InCallService.isRinging()) {
                if (!C2InCallService.answer()) showNotice("Unable to answer call");
            } else if ("OK".equals(key)) {
                if (C2InCallService.isRinging()) {
                    if (!C2InCallService.answer()) showNotice("Unable to answer call");
                } else endCall();
            } else if ("RSK".equals(key)) toggleSpeaker();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("cameraResult".equals(page)) {
            if ("LSK".equals(key)) {
                openOptionsForPage();
                return;
            }
            if ("OK".equals(key)) {
                showNotice(capturedPhotoSaveNotice());
                return;
            }
            if ("RSK".equals(key) || "END".equals(key)) {
                back();
                if (getContext() instanceof MainActivity)
                    post(() -> ((MainActivity) getContext()).showNativeCameraPreview(true));
            }
            return;
        }
        if ("cameraViewfinder".equals(page) || "videoViewfinder".equals(page)) {
            if ("OK".equals(key))
                launchHardwareCapture("videoViewfinder".equals(page));
            else if ("LSK".equals(key)) {
                if (videoRecording()) showNotice("Stop recording before opening Options");
                else openOptionsForPage();
            }
            else if ("RSK".equals(key)) {
                if (videoRecording()) ((MainActivity) getContext()).finishNativeVideoCapture();
                else back();
            }
            invalidate();
            return;
        }

        if ("calculator".equals(page)) {
            handleCalculatorKey(key);
            return;
        }

        if ("stopwatch".equals(page)) {
            if ("OK".equals(key)) toggleStopwatch();
            else if ("LSK".equals(key)) {
                long ms = stopwatchAccum + (stopwatchRunning ? SystemClock.elapsedRealtime() - stopwatchStarted : 0);
                stopwatchSplits.add(formatMillis(ms));
            } else if ("RSK".equals(key)) {
                if (!stopwatchRunning && stopwatchAccum > 0) {
                    stopwatchAccum = 0;
                    stopwatchSplits.clear();
                    showNotice("Reset");
                } else back();
            }
            invalidate();
            return;
        }

        if ("countdown".equals(page)) {
            if (isDigit(key) && countdownEnd == 0 && countdownDigits.length() < 3) countdownDigits += key;
            else if ("OK".equals(key)) {
                if (countdownEnd > 0) {
                    countdownEnd = 0;
                    showNotice("Timer stopped");
                } else {
                    int min = 0;
                    try { min = Integer.parseInt(countdownDigits); } catch (Exception ignored) {}
                    if (min > 0) {
                        countdownEnd = SystemClock.elapsedRealtime() + min * 60000L;
                        showNotice("Timer started");
                    } else showNotice("Enter minutes");
                }
            } else if ("RSK".equals(key)) {
                if (!countdownDigits.isEmpty() && countdownEnd == 0)
                    countdownDigits = countdownDigits.substring(0, countdownDigits.length() - 1);
                else back();
            } else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("music".equals(page)) {
            if ("LEFT".equals(key)) changeTrack(-1);
            else if ("RIGHT".equals(key)) changeTrack(1);
            else if ("OK".equals(key)) toggleMusic();
            else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("messageRead".equals(page)) {
            if ("OK".equals(key)) {
                AndroidBackend.DeviceSms current = smsById(activeMessageId);
                if (current == null) showNotice("Message unavailable");
                else if (current.type != Telephony.Sms.MESSAGE_TYPE_INBOX) forwardSmsCopy(current);
                else {
                    recipient = current.address;
                    messageBody = "";
                    composeFocus = 1;
                    go("compose");
                }
            } else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("contactSearch".equals(page)) {
            handleContactSearchKey(key);
            return;
        }
        if ("contactsNames".equals(page) && (isDigit(key) || "#".equals(key))) {
            beginContactSearch();
            handleContactSearchKey(key);
            return;
        }
        if ("contactDetail".equals(page)) {
            ArrayList<String> numbers = currentContactNumbers();
            if ("UP".equals(key) && !numbers.isEmpty())
                sel = (sel - 1 + numbers.size()) % numbers.size();
            else if ("DOWN".equals(key) && !numbers.isEmpty())
                sel = (sel + 1) % numbers.size();
            else if ("OK".equals(key)) callSelectedContactNumber();
            else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("voiceRecorder".equals(page)) {
            if ("OK".equals(key)) {
                if (voiceRecorder.isRecording()) finishVoiceRecording();
                else beginVoiceRecording();
            } else if ("LSK".equals(key)) openOptionsForPage();
            else if ("RSK".equals(key)) back();
            invalidate();
            return;
        }

        if ("browser".equals(page) || "itemDetail".equals(page) || "galleryPreview".equals(page)) {
            if ("galleryPreview".equals(page) &&
                    ("LEFT".equals(key) || "RIGHT".equals(key)))
                showAdjacentImage("LEFT".equals(key) ? -1 : 1);
            else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            else if ("OK".equals(key) && "browser".equals(page)) showNotice("Offline simulation");
            invalidate();
            return;
        }

        String[] items = itemsFor(page);
        if (items.length > 0) {
            if ("UP".equals(key)) sel = (sel - 1 + items.length) % items.length;
            else if ("DOWN".equals(key)) sel = (sel + 1) % items.length;
            else if ("OK".equals(key)) openSelected();
            else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            else if (isDigit(key)) {
                int n = Integer.parseInt(key);
                if (n >= 1 && n <= items.length) sel = n - 1;
            }
            invalidate();
            return;
        }

        // An empty call log still has a usable Options softkey. Previously
        // the generic list handler dropped LSK when itemsFor(page) was empty.
        if ("LSK".equals(key) &&
                ("log".equals(page) || "calllog".equals(page) ||
                 "missed".equals(page) || "received".equals(page) ||
                 "dialled".equals(page))) {
            if (items.length == 0) showNotice("No options");
            else openOptionsForPage();
        } else if ("RSK".equals(key)) back();
    }

    private void beginContactSearch() {
        contactSearchQuery = "";
        contactSearchNumeric = false;
        resetContactSearchTap();
        go("contactSearch");
    }

    private void resetContactSearchTap() {
        contactSearchLastKey = -1;
        contactSearchTapCycle = 0;
        contactSearchLastAt = 0L;
    }

    private ArrayList<Integer> contactSearchMatches() {
        ArrayList<Integer> matches = new ArrayList<>();
        String query = contactSearchQuery.toLowerCase(Locale.ROOT).trim();
        String digits = digitsOnly(contactSearchQuery);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            boolean found;
            if (contactSearchNumeric) {
                found = !digits.isEmpty() && digitsOnly(contact.number).contains(digits);
                if (!found) {
                    ArrayList<String> aliases = contactNumbersById.get(contact.contactId);
                    if (aliases != null)
                        for (String alternative : aliases)
                            if (!digits.isEmpty() && digitsOnly(alternative).contains(digits)) {
                                found = true;
                                break;
                            }
                }
            } else found = query.isEmpty() ||
                    contact.name.toLowerCase(Locale.ROOT).contains(query);
            if (found) matches.add(i);
        }
        return matches;
    }

    private Contact selectedContactSearchResult() {
        ArrayList<Integer> matches = contactSearchMatches();
        if (sel < 0 || sel >= matches.size()) return null;
        int index = matches.get(sel);
        return index < contacts.size() ? contacts.get(index) : null;
    }

    private void openContactSearchResult() {
        ArrayList<Integer> matches = contactSearchMatches();
        if (sel < 0 || sel >= matches.size()) {
            showNotice("No matching contact");
            return;
        }
        contactIndex = matches.get(sel);
        go("contactDetail");
    }

    private void handleContactSearchKey(String key) {
        if ("UP".equals(key) || "DOWN".equals(key)) {
            resetContactSearchTap();
            int count = contactSearchMatches().size();
            if (count > 0)
                sel = "UP".equals(key) ? (sel - 1 + count) % count : (sel + 1) % count;
        } else if ("OK".equals(key)) openContactSearchResult();
        else if ("LSK".equals(key)) {
            resetContactSearchTap();
            openOptionsForPage();
        } else if ("RSK".equals(key)) {
            if (contactSearchQuery.isEmpty()) back();
            else {
                contactSearchQuery = contactSearchQuery.substring(0, contactSearchQuery.length() - 1);
                sel = 0;
            }
            resetContactSearchTap();
        } else if ("#".equals(key)) {
            contactSearchNumeric = !contactSearchNumeric;
            contactSearchQuery = "";
            sel = 0;
            resetContactSearchTap();
        } else if ("*".equals(key)) {
            if (contactSearchNumeric && contactSearchQuery.length() < 24) {
                contactSearchQuery += "+";
                sel = 0;
            }
            resetContactSearchTap();
        } else if (isDigit(key)) {
            int digit = key.charAt(0) - '0';
            if (contactSearchNumeric) {
                if (contactSearchQuery.length() < 24) contactSearchQuery += key;
                resetContactSearchTap();
            } else {
                String[] maps = {" ", ".,?!1", "abc2", "def3", "ghi4",
                        "jkl5", "mno6", "pqrs7", "tuv8", "wxyz9"};
                String values = maps[digit];
                long now = SystemClock.uptimeMillis();
                if (digit != 0 && digit == contactSearchLastKey && now - contactSearchLastAt < 1050
                        && !contactSearchQuery.isEmpty()) {
                    contactSearchTapCycle = (contactSearchTapCycle + 1) % values.length();
                    contactSearchQuery = contactSearchQuery.substring(0, contactSearchQuery.length() - 1)
                            + values.charAt(contactSearchTapCycle);
                } else if (contactSearchQuery.length() < 24) {
                    contactSearchTapCycle = 0;
                    contactSearchQuery += values.charAt(0);
                }
                contactSearchLastKey = digit;
                contactSearchLastAt = now;
            }
            sel = 0;
        }
        invalidate();
    }

    private void handleComposeKey(String key) {
        if ("UP".equals(key) || "DOWN".equals(key)) {
            composeFocus = composeFocus == 0 ? 1 : 0;
            resetTap();
        } else if ("LEFT".equals(key) || "RIGHT".equals(key)) {
            // Cursor movement is intentionally simplified in this first native build.
        } else if ("OK".equals(key)) {
            if (composeFocus == 0) composeFocus = 1;
            else sendMessage();
        } else if ("LSK".equals(key)) openOptionsForPage();
        else if ("RSK".equals(key)) {
            resetTap();
            if (composeFocus == 0 && !recipient.isEmpty()) recipient = recipient.substring(0, recipient.length() - 1);
            else if (composeFocus == 1 && !messageBody.isEmpty()) messageBody = messageBody.substring(0, messageBody.length() - 1);
            else back();
        } else if ("#".equals(key) && composeFocus == 1) {
            commitT9();
            if ("T9".equals(inputMode)) inputMode = "abc";
            else if ("abc".equals(inputMode)) inputMode = "ABC";
            else if ("ABC".equals(inputMode)) inputMode = "123";
            else inputMode = "T9";
            resetTap();
        } else if ("*".equals(key) && composeFocus == 1) {
            commitT9();
            go("symbolPicker");
            resetTap();
        } else if (isDigit(key)) {
            if (composeFocus == 0) {
                recipient += key;
            } else {
                handleTextDigit(Integer.parseInt(key), true);
            }
        }
        invalidate();
    }

    private void handleEditKey(String key) {
        if ("contact".equals(editKind) && ("UP".equals(key) || "DOWN".equals(key))) {
            editField = editField == 0 ? 1 : 0;
            inputMode = editField == 1 ? "123" : "Abc";
            resetTap();
        } else if ("contact".equals(editKind) && "OK".equals(key)) {
            // The first OK commits the name's multi-tap character and advances
            // to Number. OK in Number saves; the left Save softkey saves directly.
            resetTap();
            if (editField == 0) {
                editField = 1;
                inputMode = "123";
            } else saveEdit();
        } else if ("OK".equals(key) || "LSK".equals(key)) {
            resetTap();
            saveEdit();
        } else if ("RSK".equals(key)) {
            resetTap();
            if ("contact".equals(editKind)) deleteContactEditorChar();
            else if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            else back();
        } else if ("#".equals(key)) {
            if ("Abc".equals(inputMode)) inputMode = "abc";
            else if ("abc".equals(inputMode)) inputMode = "ABC";
            else if ("ABC".equals(inputMode)) inputMode = "123";
            else inputMode = "Abc";
            resetTap();
        } else if ("*".equals(key)) {
            appendToEditor(" ");
            resetTap();
        } else if (isDigit(key)) {
            if ("url".equals(editKind)) {
                if ("1".equals(key)) appendToEditor(".");
                else handleEditorDigit(Integer.parseInt(key));
            } else if ("contact".equals(editKind) && editField == 1) {
                appendToContactField(key);
            } else handleEditorDigit(Integer.parseInt(key));
        }
        invalidate();
    }

    private void handleEditorDigit(int digit) {
        String before = currentEditorField();
        String after = multiTap(before, digit);
        setCurrentEditorField(after);
    }

    private String currentEditorField() {
        if (!"contact".equals(editKind)) return editBuffer;
        String[] parts = editBuffer.split("\\n", -1);
        if (editField == 0) return parts.length > 0 ? parts[0] : "";
        return parts.length > 1 ? parts[1] : "";
    }

    private void setCurrentEditorField(String value) {
        if (!"contact".equals(editKind)) {
            editBuffer = value;
            return;
        }
        String[] parts = editBuffer.split("\\n", -1);
        String a = parts.length > 0 ? parts[0] : "";
        String b = parts.length > 1 ? parts[1] : "";
        if (editField == 0) a = value; else b = value;
        editBuffer = a + "\n" + b;
    }

    private void appendToEditor(String s) {
        if ("contact".equals(editKind)) appendToContactField(s);
        else editBuffer += s;
    }

    private void appendToContactField(String s) {
        setCurrentEditorField(currentEditorField() + s);
    }

    private void deleteContactEditorChar() {
        String v = currentEditorField();
        if (!v.isEmpty()) setCurrentEditorField(v.substring(0, v.length() - 1));
        else back();
    }

    private void handleTextDigit(int digit, boolean allowT9) {
        if ("123".equals(inputMode)) {
            messageBody += digit;
            resetTap();
            return;
        }
        if ("T9".equals(inputMode) && allowT9 && digit >= 2 && digit <= 9) {
            handleT9(digit);
            return;
        }
        messageBody = multiTap(messageBody, digit);
    }

    private String multiTap(String target, int digit) {
        String[] maps = {" ",".,?!1","abc2","def3","ghi4","jkl5","mno6","pqrs7","tuv8","wxyz9"};
        String set = maps[Math.max(0, Math.min(9, digit))];
        if ("ABC".equals(inputMode) || ("Abc".equals(inputMode) &&
                ("contact".equals(editKind) && editField == 0) &&
                (target.isEmpty() || target.charAt(target.length() - 1) == ' ')))
            set = set.toUpperCase(Locale.UK);

        long now = SystemClock.uptimeMillis();
        if (digit == lastTapKey && now - lastTapAt < 1300 && !target.isEmpty()) {
            tapIndex = (tapIndex + 1) % set.length();
            target = target.substring(0, target.length() - 1) + set.charAt(tapIndex);
        } else {
            tapIndex = 0;
            target += set.charAt(0);
        }
        lastTapKey = digit;
        lastTapAt = now;
        return target;
    }

    private void handleT9(int digit) {
        if (t9Digits.isEmpty()) t9Start = messageBody.length();
        t9Digits += digit;
        List<String> matches = t9Matches(t9Digits);
        if (matches.isEmpty()) {
            commitT9();
            messageBody += digit;
            return;
        }
        t9Choice = 0;
        String word = matches.get(0);
        messageBody = messageBody.substring(0, t9Start) + word;
    }

    private void cycleT9() {
        if (t9Digits.isEmpty()) return;
        List<String> matches = t9Matches(t9Digits);
        if (matches.isEmpty()) return;
        t9Choice = (t9Choice + 1) % matches.size();
        messageBody = messageBody.substring(0, t9Start) + matches.get(t9Choice);
    }

    private List<String> t9Matches(String digits) {
        ArrayList<String> m = new ArrayList<>();
        for (String w : t9Words) if (wordDigits(w).startsWith(digits)) m.add(w);
        return m;
    }

    private String wordDigits(String word) {
        StringBuilder b = new StringBuilder();
        for (char ch : word.toLowerCase(Locale.UK).toCharArray()) {
            if ("abc".indexOf(ch) >= 0) b.append('2');
            else if ("def".indexOf(ch) >= 0) b.append('3');
            else if ("ghi".indexOf(ch) >= 0) b.append('4');
            else if ("jkl".indexOf(ch) >= 0) b.append('5');
            else if ("mno".indexOf(ch) >= 0) b.append('6');
            else if ("pqrs".indexOf(ch) >= 0) b.append('7');
            else if ("tuv".indexOf(ch) >= 0) b.append('8');
            else if ("wxyz".indexOf(ch) >= 0) b.append('9');
        }
        return b.toString();
    }

    private void commitT9() {
        t9Digits = "";
        t9Start = messageBody.length();
        t9Choice = 0;
    }

    private void resetTap() {
        lastTapKey = -1;
        tapIndex = 0;
        lastTapAt = 0;
        commitT9();
    }

    private void handleCalculatorKey(String key) {
        if (isDigit(key)) {
            if (calc.length() < 12) calc = "0".equals(calc) ? key : calc + key;
        } else if ("*".equals(key)) {
            if (!calc.contains(".")) calc += ".";
        } else if ("#".equals(key)) {
            if (calc.startsWith("-")) calc = calc.substring(1);
            else if (!"0".equals(calc)) calc = "-" + calc;
        } else if ("UP".equals(key)) setCalcOp('×');
        else if ("DOWN".equals(key)) setCalcOp('÷');
        else if ("LEFT".equals(key)) setCalcOp('-');
        else if ("RIGHT".equals(key)) setCalcOp('+');
        else if ("OK".equals(key)) calculate();
        else if ("RSK".equals(key)) {
            if (calc.length() > 1) calc = calc.substring(0, calc.length() - 1);
            else if (!"0".equals(calc)) calc = "0";
            else back();
        } else if ("LSK".equals(key)) openOptionsForPage();
        invalidate();
    }

    private void setCalcOp(char op) {
        try { calcStored = Double.parseDouble(calc); } catch (Exception e) { calcStored = 0d; }
        calcOp = op;
        calc = "0";
    }

    private void calculate() {
        if (calcStored == null || calcOp == 0) return;
        double b;
        try { b = Double.parseDouble(calc); } catch (Exception e) { b = 0; }
        double r;
        if (calcOp == '+') r = calcStored + b;
        else if (calcOp == '-') r = calcStored - b;
        else if (calcOp == '×') r = calcStored * b;
        else r = b == 0 ? Double.NaN : calcStored / b;
        if (Double.isNaN(r) || Double.isInfinite(r)) calc = "Error";
        else if (Math.abs(r - Math.rint(r)) < 0.0000001) calc = String.valueOf((long)Math.rint(r));
        else calc = String.valueOf(r);
        calcStored = null;
        calcOp = 0;
    }

    private void applyScientific(int which) {
        double x;
        try { x = Double.parseDouble(calc); } catch (Exception e) { x = 0; }
        double r = x;
        if (which == 0) r = Math.sin(Math.toRadians(x));
        else if (which == 1) r = Math.cos(Math.toRadians(x));
        else if (which == 2) r = Math.tan(Math.toRadians(x));
        else if (which == 3) r = x < 0 ? Double.NaN : Math.sqrt(x);
        else if (which == 4) r = x * x;
        else if (which == 5) r = x <= 0 ? Double.NaN : Math.log10(x);
        else if (which == 6) r = x == 0 ? Double.NaN : 1d / x;
        else if (which == 7) r = Math.PI;
        if (Double.isNaN(r) || Double.isInfinite(r)) calc = "Error";
        else if (Math.abs(r - Math.rint(r)) < 0.0000001) calc = String.valueOf((long)Math.rint(r));
        else calc = String.format(Locale.UK, "%.8g", r);
        showNotice(calc);
    }

    private void handleLoanKey(String key) {
        if ("UP".equals(key)) loanField = (loanField + 2) % 3;
        else if ("DOWN".equals(key)) loanField = (loanField + 1) % 3;
        else if (isDigit(key)) {
            String v = loanValue();
            if (v.length() < 10) setLoanValue(("0".equals(v) ? "" : v) + key);
        } else if ("*".equals(key) && loanField == 1 && !loanRate.contains(".")) {
            setLoanValue(loanRate + ".");
        } else if ("RSK".equals(key)) {
            String v = loanValue();
            if (!v.isEmpty()) setLoanValue(v.substring(0, v.length() - 1));
            else back();
        } else if ("OK".equals(key) || "LSK".equals(key)) {
            calculateLoan();
        }
        invalidate();
    }

    private String loanValue() {
        if (loanField == 0) return loanPrincipal;
        if (loanField == 1) return loanRate;
        return loanMonths;
    }

    private void setLoanValue(String v) {
        if (loanField == 0) loanPrincipal = v;
        else if (loanField == 1) loanRate = v;
        else loanMonths = v;
    }

    private void calculateLoan() {
        try {
            double principal = Double.parseDouble(loanPrincipal);
            double annual = Double.parseDouble(loanRate);
            int months = Integer.parseInt(loanMonths);
            if (principal <= 0 || months <= 0) throw new Exception();
            double monthlyRate = annual / 1200d;
            double payment = monthlyRate == 0 ? principal / months :
                    principal * monthlyRate / (1d - Math.pow(1d + monthlyRate, -months));
            showNotice("Monthly " + String.format(Locale.UK, "%.2f", payment));
        } catch (Exception e) {
            showNotice("Check loan values");
        }
    }

    private void toggleStopwatch() {
        if (stopwatchRunning) {
            stopwatchAccum += SystemClock.elapsedRealtime() - stopwatchStarted;
            stopwatchRunning = false;
        } else {
            stopwatchStarted = SystemClock.elapsedRealtime();
            stopwatchRunning = true;
        }
    }

    private void toggleMusic() {
        if (!musicEngine.toggle()) showNotice(musicEngine.error());
        invalidate();
    }

    private void changeTrack(int delta) {
        if (!musicEngine.skip(delta)) showNotice(musicEngine.error());
        invalidate();
    }

    private void pickAudioFiles() {
        if (getContext() instanceof MainActivity)
            ((MainActivity) getContext()).requestAudioFiles();
        else showNotice("Android audio file picker unavailable");
    }

    public void onAudioPickerUnavailable() {
        showNotice("No Android audio file picker available");
    }

    public void onAudioPicked(java.util.List<Uri> chosen) {
        int added = musicEngine.add(chosen);
        if (!"musicLibrary".equals(page)) go("musicLibrary");
        sel = Math.max(0, musicEngine.tracks.size() - added);
        showNotice(added > 0 ? added + " audio file(s) added" : "No new audio selected");
        invalidate();
    }

    private void openMusicEqualiser() {
        if (musicEngine.index() < 0) {
            showNotice("Open an audio file first");
        } else if (!musicEngine.hasEqualizer()) {
            showNotice("Equaliser unavailable on this device");
        } else {
            go("musicEqualiser");
            sel = musicEngine.selectedPresetRow();
        }
    }

    private void showGalleryDetails(boolean selectedOnly, boolean video) {
        if (selectedOnly) {
            NativeGalleryStore.Entry entry = "galleryPreview".equals(page)
                    ? findGalleryPreviewEntry() :
                    ("galleryImages".equals(page) || "galleryVideos".equals(page)
                            ? galleryStore.at(video, sel) : null);
            if (entry != null) {
                Uri uri = Uri.parse(entry.uri);
                String mime = null;
                Long size = null;
                try {
                    mime = getContext().getContentResolver().getType(uri);
                    try (android.database.Cursor cursor = getContext().getContentResolver()
                            .query(uri, new String[]{android.provider.OpenableColumns.SIZE},
                                    null, null, null)) {
                        if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0))
                            size = cursor.getLong(0);
                    }
                } catch (Exception ignored) { }
                detailTitle = "File details";
                detailText = "Name: " + entry.title +
                        "\nType: " + (mime == null ? "Unknown" : mime) +
                        "\nSize: " + (size == null ? "Unknown" : size + " bytes") +
                        "\nOriginal: Android document" +
                        "\nRemove from Gallery affects this app only";
                go("itemDetail");
                return;
            }
        }
        detailTitle = "Gallery memory";
        detailText = "User-selected images: " + galleryStore.images.size() +
                "\nUser-selected videos: " + galleryStore.videos.size() +
                "\nReal device storage: not queried" +
                "\nOriginal device media unchanged";
        go("itemDetail");
    }

    private NativeGalleryStore.Entry findGalleryPreviewEntry() {
        if (galleryPreviewIndex >= 0)
            return galleryStore.at(false, galleryPreviewIndex);
        for (NativeGalleryStore.Entry e : galleryStore.images)
            if (e.uri.equals(galleryPreviewUri)) return e;
        return null;
    }

    public boolean isInVideoViewfinder() {
        return "videoViewfinder".equals(page);
    }

    private boolean videoRecording() {
        return getContext() instanceof MainActivity &&
                ((MainActivity) getContext()).isNativeVideoRecording();
    }

    private boolean videoSaving() {
        return getContext() instanceof MainActivity &&
                ((MainActivity) getContext()).isNativeVideoFinalising();
    }

    public void onNativeVideoSaving() {
        showNotice("Saving video...");
        invalidate();
    }

    public void onNativeVideoRecordingChanged(boolean recording) {
        showNotice(recording ? "Recording video - press Stop to save" : "Recording stopped");
        invalidate();
    }

    /**
     * Stop's native encoder may remain blocked without producing a moov atom.
     * Leave the Video recorder rather than trapping every Nokia key on Saving.
     * Camera reuse is prevented in MainActivity until the worker returns.
     */
    public void onNativeVideoFinalisationTimedOut() {
        if ("videoViewfinder".equals(page)) {
            optionsOpen = false;
            go("media");
        }
        showNotice("Video encoder stalled - reopen C2 to retry");
        invalidate();
    }

    /** If an overdue stop eventually succeeds, index it without hijacking UI. */
    public void onNativeVideoSavedAfterTimeout(Uri uri) {
        int added = galleryStore.add(true,
                java.util.Collections.singletonList(uri));
        showNotice(added > 0 ? "Video saved to Gallery" :
                "Video saved, but Gallery index unchanged");
        invalidate();
    }

    public boolean isInCameraViewfinder() {
        return "cameraViewfinder".equals(page) || "videoViewfinder".equals(page);
    }

    private void startCameraCapture(boolean video) {
        // First enter the in-house Nokia UI rather than directly leaving C2.
        go(video ? "videoViewfinder" : "cameraViewfinder");
        if (getContext() instanceof MainActivity)
            post(() -> ((MainActivity) getContext()).showNativeCameraPreview(true));
    }

    private void launchHardwareCapture(boolean video) {
        if (!(getContext() instanceof MainActivity)) {
            showNotice("Android camera is unavailable");
            return;
        }
        MainActivity host = (MainActivity) getContext();
        if (!video) {
            host.captureNativePhoto();
        } else {
            host.toggleNativeVideoCapture();
        }
    }

    private void drawNokiaCameraViewfinder(Canvas c) {
        boolean video = "videoViewfinder".equals(page);
        drawStatus(c, video ? (videoSaving() ? "Saving video" :
                (videoRecording() ? "Video recording" : "Video recorder")) : "Camera");
        p.setColor(Color.rgb(19, 26, 30));
        c.drawRect(6, 45, 234, 253, p);
        p.setColor(Color.rgb(83, 105, 110));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        c.drawRect(8, 47, 232, 251, p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(fontBold);
        p.setColor(Color.WHITE);
        p.setTextSize(17);
        c.drawText(video ? "Video recorder" : "Camera", 120, 110, p);
        p.setTypeface(fontRegular);
        p.setTextSize(12);
        c.drawText("Live preview unavailable", 120, 141, p);
        c.drawText(video ? (videoSaving() ? "Saving video..." :
                (videoRecording() ? "Press Stop to save video" : "Press Record to start")) :
                "Check camera permission", 120, 161, p);
        p.setTextSize(13);
        c.drawText(video ? "Video mode" : "Photo mode", 120, 228, p);
    }

    public void onCameraCaptureFailed(String reason) {
        showNotice(reason);
        invalidate();
    }

    /** Called only when the Android camera saved a validated private file. */
    public void onCameraCaptureCompleted(boolean video, Uri uri) {
        int added = galleryStore.add(video, java.util.Collections.singletonList(uri));
        if (added == 0) {
            showNotice("Capture saved, but could not be added to Gallery");
            return;
        }
        if (!video) {
            showCapturedPhoto(uri);
            return;
        }
        go("galleryVideos");
        java.util.List<NativeGalleryStore.Entry> entries = galleryStore.entries(video);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).uri.equals(uri.toString())) {
                sel = i;
                break;
            }
        }
        showNotice(video ? ("media".equals(uri.getAuthority()) ?
                "Video saved to Movies" : "Video saved privately") : "Photo saved");
        invalidate();
    }

    private Uri capturedPhotoUri;
    private Bitmap capturedPhotoPreview;

    private String capturedPhotoSaveNotice() {
        if (capturedPhotoUri == null || !(getContext() instanceof MainActivity))
            return "No captured photo";
        if (!((MainActivity) getContext()).capturedPhotoExists(capturedPhotoUri))
            return "Photo missing from storage";
        // Capture already committed the file; Save verifies it did not disappear.
        return "media".equals(capturedPhotoUri.getAuthority())
                ? "Photo saved to Pictures" : "Photo saved privately";
    }
    private void showCapturedPhoto(Uri uri) {
        capturedPhotoUri = uri;
        if (capturedPhotoPreview != null) {
            capturedPhotoPreview.recycle();
            capturedPhotoPreview = null;
        }
        try (java.io.InputStream input = getContext().getContentResolver().openInputStream(uri)) {
            if (input != null) {
                android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                opts.inSampleSize = 4;
                capturedPhotoPreview = BitmapFactory.decodeStream(input, null, opts);
            }
            capturedPhotoPreview = orientCapturedPhotoPreview(uri, capturedPhotoPreview);
        } catch (Exception error) {
            android.util.Log.w("C2Camera", "Unable to render captured photo", error);
        }
        go("cameraResult");
        showNotice("Photo saved");
    }

    /** BitmapFactory does not apply EXIF orientation on Android.
     * Rotate/flip only the small sampled preview; leave the saved JPEG's
     * hardware orientation metadata intact for external Gallery apps.
     */
    private Bitmap orientCapturedPhotoPreview(Uri uri, Bitmap source) {
        if (source == null || android.os.Build.VERSION.SDK_INT < 24) return source;
        int orientation = android.media.ExifInterface.ORIENTATION_NORMAL;
        try (java.io.InputStream input =
                     getContext().getContentResolver().openInputStream(uri)) {
            if (input == null) return source;
            android.media.ExifInterface exif = new android.media.ExifInterface(input);
            orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ex) {
            android.util.Log.w("C2Camera", "Photo EXIF orientation unreadable", ex);
            return source;
        }
        android.graphics.Matrix transform = new android.graphics.Matrix();
        switch (orientation) {
            case android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                transform.setScale(-1f, 1f); break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                transform.setRotate(180f); break;
            case android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                transform.setScale(1f, -1f); break;
            case android.media.ExifInterface.ORIENTATION_TRANSPOSE:
                transform.setRotate(90f);
                transform.postScale(-1f, 1f); break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                transform.setRotate(90f); break;
            case android.media.ExifInterface.ORIENTATION_TRANSVERSE:
                transform.setRotate(270f);
                transform.postScale(-1f, 1f); break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                transform.setRotate(270f); break;
            default:
                return source;
        }
        try {
            Bitmap rotated = Bitmap.createBitmap(
                    source, 0, 0, source.getWidth(), source.getHeight(), transform, true);
            if (rotated != source) source.recycle();
            return rotated;
        } catch (RuntimeException ex) {
            android.util.Log.w("C2Camera", "Photo orientation transform failed", ex);
            return source;
        }
    }

    private void drawCameraResult(Canvas c) {
        drawStatus(c, "Photo");
        p.setColor(Color.rgb(22, 28, 31));
        c.drawRect(6, 42, 234, 270, p);
        if (capturedPhotoPreview != null) {
            p.setColor(Color.WHITE);
            c.drawBitmap(capturedPhotoPreview, null, new RectF(8, 44, 232, 268), p);
        } else {
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(13);
            c.drawText("Photo saved", 120, 158, p);
        }
    }

    private void openGalleryFolder(boolean video) {
        go(video ? "galleryVideos" : "galleryImages");
        if (galleryStore.entries(video).isEmpty())
            pickGalleryFiles(video);
    }

    private void pickGalleryFiles(boolean video) {
        if (getContext() instanceof MainActivity)
            ((MainActivity) getContext()).requestGalleryFiles(video);
        else showNotice("Android file picker not available");
    }

    /** Only called after explicit user selection in Android's document picker. */
    public void onGalleryPickerError() {
        showNotice("No Android file picker available");
        invalidate();
    }

    public void onGalleryPicked(boolean video, java.util.List<Uri> uris) {
        int added = galleryStore.add(video, uris);
        if ("galleryPreview".equals(page)) {
            clearGalleryPreview();
            back();
        }
        sel = 0;
        if (added > 0) showNotice(added + " file(s) added to Gallery");
        else showNotice("No new files selected");
        invalidate();
    }

    private void openSelectedGalleryEntry() {
        boolean video = "galleryVideos".equals(page);
        NativeGalleryStore.Entry item = galleryStore.at(video, sel);
        if (item == null) {
            pickGalleryFiles(video);
            return;
        }
        if (video) {
            if (!(getContext() instanceof MainActivity) ||
                    !((MainActivity) getContext()).viewGalleryVideo(item.uri))
                showNotice("No compatible video player");
            return;
        }
        int selected = sel;
        showGalleryPreviewAt(selected);
        go("galleryPreview");
    }

    private void showGalleryPreviewAt(int index) {
        NativeGalleryStore.Entry item = galleryStore.at(false, index);
        if (item == null) return;
        clearGalleryPreview();
        galleryPreviewIndex = index;
        galleryPreviewUri = item.uri;
        galleryPreviewTitle = item.title;
        galleryPreviewBitmap = galleryStore.decodePreview(item.uri, 240, 240);
        invalidate();
    }

    private void showAdjacentImage(int offset) {
        int n = galleryStore.images.size();
        if (n == 0) {
            showNotice("Gallery is empty");
            return;
        }
        int index = galleryPreviewIndex >= 0 ? galleryPreviewIndex : 0;
        showGalleryPreviewAt((index + offset + n) % n);
    }

    private void clearGalleryPreview() {
        if (galleryPreviewBitmap != null) {
            galleryPreviewBitmap.recycle();
            galleryPreviewBitmap = null;
        }
        galleryPreviewUri = "";
        galleryPreviewTitle = "";
        galleryPreviewIndex = -1;
    }

    private void moveMainMenuSelection() {
        if (!menuOrganising || !"menu".equals(page)) return;
        if (menuMovingFrom < 0) {
            menuMovingFrom = sel;
            showNotice("Choose destination");
        } else {
            int previous = menuMovingFrom;
            menuMovingFrom = -1;
            if (NativeMenuLayout.move(menuOrder, previous, sel)) {
                saveMenuPreferences();
                showNotice("Item moved");
            } else showNotice("Position unchanged");
        }
        invalidate();
    }

    private void startHomeShortcutAssignment(int slot) {
        if (slot < 0 || slot >= NativeHomeShortcuts.SLOT_COUNT) {
            showNotice("Home shortcut key unavailable");
            return;
        }
        shortcutAssignmentSlot = slot;
        go("shortcutAssign");
        sel = homeShortcuts.assigned(slot);
    }

    /**
     * Home-screen shortcut dispatch is deliberately app-local: Settings >
     * My shortcuts never changes Android global buttons or launcher actions.
     */
    private void openAssignedHomeAction(int action) {
        switch (action) {
            case NativeHomeShortcuts.MENU: go("menu"); break;
            case NativeHomeShortcuts.GO_TO: go("goto"); break;
            case NativeHomeShortcuts.NAMES:
                refreshContactsFromPhone();
                go("contactsNames");
                break;
            case NativeHomeShortcuts.MESSAGING: go("messaging"); break;
            case NativeHomeShortcuts.CONTACTS: go("contacts"); break;
            case NativeHomeShortcuts.GALLERY: go("gallery"); break;
            case NativeHomeShortcuts.CAMERA: startCameraCapture(false); break;
            case NativeHomeShortcuts.VIDEO_RECORDER: startCameraCapture(true); break;
            case NativeHomeShortcuts.ORGANISER: go("organiser"); break;
            case NativeHomeShortcuts.CALCULATOR: go("calculator"); break;
            case NativeHomeShortcuts.ALARM_CLOCK: go("alarm"); break;
            case NativeHomeShortcuts.SETTINGS: go("settings"); break;
            case NativeHomeShortcuts.PROFILES: go("profiles"); break;
            case NativeHomeShortcuts.NONE: break;
            default: showNotice("Unknown shortcut");
        }
        auditHomeShortcuts("action=" + action);
    }

    private void openGoToShortcut(int shortcutId) {
        // Dispatch through the EXACT same router exercised by CI tests.
        // This prevents a rearranged/hidden visual row from changing its
        // stable destination or converting Video into photo capture.
        auditGoTo("launch destination=" + shortcutId);
        boolean recognized = NativeGoToRouter.launch(shortcutId,
                new NativeGoToRouter.Handler() {
                    @Override public void lockKeypad() {
                        locked = true;
                        unlockStep = 0;
                        history.clear();
                        page = "home";
                        sel = 0;
                        showNotice("Keypad locked");
                    }
                    @Override public void openPage(String target) {
                        go(target);
                    }
                    @Override public void capture(boolean video) {
                        auditGoTo(video ? "capture VIDEO" : "capture PHOTO");
                        startCameraCapture(video);
                    }
                    @Override public void browser() {
                        browserUrl = "https://www.nokia.com/";
                        go("browser");
                    }
                });
        if (!recognized) showNotice("No shortcut selected");
    }

    private void toggleGoToOrganiseSelection() {
        int id = goToShortcuts.visualId(sel);
        boolean wasVisible = goToShortcuts.isEnabled(id);
        if (goToShortcuts.toggleAt(sel)) {
            auditGoTo("toggle item=" + id + " enabled=" + !wasVisible);
            saveGoToShortcuts();
            showNotice(wasVisible ? "Shortcut hidden" : "Shortcut shown");
        } else if (wasVisible && goToShortcuts.enabledCount() == 1) {
            showNotice("Keep at least one shortcut");
        } else showNotice("Shortcut unavailable");
    }

    private void moveGoToShortcut(int direction) {
        int from = sel;
        if (goToShortcuts.move(from, direction)) {
            sel = from + direction;
            auditGoTo("move from=" + from + " to=" + sel);
            saveGoToShortcuts();
            showNotice("Shortcut moved");
        } else showNotice("Already at end of list");
    }

    private void syncCameraPreviewOverlay() {
        if (isInCameraViewfinder() && getContext() instanceof MainActivity) {
            final boolean show = !optionsOpen && !confirmOpen;
            post(() -> {
                if (isInCameraViewfinder())
                    ((MainActivity) getContext()).showNativeCameraPreview(show);
            });
        }
    }

    private void openOptionsForPage() {
        optionSubmenuTitle = "";
        if ("shortcuts".equals(page) || "shortcutNavigation".equals(page))
            optionItems = new String[]{"Assign","Restore defaults","Help"};
        else if ("shortcutAssign".equals(page)) optionItems = new String[]{"Assign"};
        else if ("demoTools".equals(page)) optionItems = new String[]{"Select","What is included"};
        else if ("cameraViewfinder".equals(page))
            optionItems = new String[]{"Capture","Switch mode","Help"};
        else if ("videoViewfinder".equals(page))
            optionItems = new String[]{"Record","Switch to camera","Video clips","Help"};
        else if ("cameraResult".equals(page)) optionItems = new String[]{"Save","Delete","Send"};
        else if ("menuViewPicker".equals(page)) optionItems = new String[]{"Select"};
        else if ("menu".equals(page)) optionItems = new String[]{"Main menu view","Organise","Help"};
        else if ("messaging".equals(page)) {
            if (sel == 1) optionItems = new String[]{"New message","Inbox view","Message log","SIM messages","Memory status"};
            else if (sel == 2) optionItems = new String[]{"New message","Inbox view","Folder details","Message log","SIM messages","Memory status"};
            else if (sel >= 3 && sel <= 5) optionItems = new String[]{"New message","Folder details","Message log","Add mailbox","IM messages","Memory status"};
            else if (sel == 7) optionItems = new String[]{"New message","New e-mail","Add mailbox","Message log","IM messages","Memory status"};
            else optionItems = new String[]{"New message","Message log","Add mailbox","IM messages","Memory status"};
        }
        else if ("sent".equals(page))
            optionItems = new String[]{"Open","New message","Inbox view","Folder details","Message log","Memory status"};
        else if ("drafts".equals(page))
            optionItems = new String[]{"Open","New message","Delete draft","Delete all drafts","Inbox view","Folder details","Memory status"};
        else if ("outbox".equals(page))
            optionItems = new String[]{"New message","Inbox view","Folder details","Message log","SIM messages","Memory status"};
        else if ("savedmessages".equals(page))
            optionItems = new String[]{"Use template","Details","Delete template","Delete all templates","New message","Memory status"};
        else if ("smsUseDetails".equals(page)) {
            NativeSmsDetails.Entry entry = selectedSmsDetail();
            if (entry == null) optionItems = new String[]{"Back"};
            else if (NativeSmsDetails.Entry.PHONE.equals(entry.kind))
                optionItems = new String[]{"Details","Call","Send message","Save a contact","Copy"};
            else if (NativeSmsDetails.Entry.EMAIL.equals(entry.kind))
                optionItems = new String[]{"Details","Compose e-mail","Copy"};
            else optionItems = new String[]{"Details","Open link","Copy"};
        }
        else if ("inbox".equals(page)) {
            AndroidBackend.DeviceSms target = sel >= 0 && sel < smsInbox.size() ? smsInbox.get(sel) : null;
            optionItems = new String[]{"Reply","Reply as","Delete","Call","Use detail","Forward","Edit","Move","Copy as template","Message details","Conversation view","New message",
                    target != null && isSmsUnread(target) ? "Mark as read" : "Mark as unread"};
        }
        else if ("messageRead".equals(page)) {
            AndroidBackend.DeviceSms chosen = smsById(activeMessageId);
            optionItems = chosen != null && chosen.type != Telephony.Sms.MESSAGE_TYPE_INBOX
                    ? new String[]{"Forward","Delete","Call","Use detail","Message details","Conversation view","New message","Copy as template"}
                    : new String[]{"Reply","Reply as","Delete","Call","Use detail","Forward","Edit","Move","Copy as template","Message details","Conversation view","New message",
                             chosen != null && isSmsUnread(chosen) ? "Mark as read" : "Mark as unread"};
        }
        else if ("conversations".equals(page))
            // Both actions remain available when marked threads have mixed unread states.
            optionItems = new String[]{"Call","Conversation details","Delete conversation","Inbox view >","New message >","Mark >","Mark all","Unmark all",
                    "Mark as read","Mark as unread"};
        else if ("conversation".equals(page)) {
            AndroidBackend.DeviceSms target = conversationMessageAt(sel);
            ArrayList<Long> incomingTargets = receivedConversationTargets(target);
            String[] core = new String[]{"Reply","Forward","Message details","Delete","Call",
                    "Use detail","Move","Copy as template","Go to Drafts",
                    "Conversation details","Mark","Mark all","Unmark all"};
            if (incomingTargets.isEmpty()) optionItems = core;
            else {
                ArrayList<String> choices = new ArrayList<>(Arrays.asList(core));
                if (!markedSmsIds.isEmpty()) {
                    // Marked rows may mix read and unread; expose BOTH actions.
                    // This also works if the currently highlighted row is sent.
                    choices.add("Mark as read");
                    choices.add("Mark as unread");
                } else {
                    choices.add(isSmsUnread(target) ? "Mark as read" : "Mark as unread");
                }
                optionItems = choices.toArray(new String[0]);
            }
        }
        else if ("compose".equals(page))
            optionItems = new String[]{"Send","Preview","Insert","Add recipient >","Add subject","Clear field","Insert contact detail","Insert symbol","Editing options >","Writing language >","Prediction options >","Change to multim.","Save message >","Sending options >","Exit editor"};
        else if ("contacts".equals(page))
            optionItems = new String[]{"Open","Search","Add new","Memory status"};
        else if ("contactsNames".equals(page))
            optionItems = new String[]{"Search","Call >","Send message >","View conversations",
              "Add new >","Edit >","Delete contact","Add to group","Selecting options >",
              "Details","Add to contact","Edit contact","Copy contact","Move contact",
              "Merge contact","Add to blacklist","Add to whitelist"};
        else if ("contactSearch".equals(page))
            optionItems = new String[]{"Open","Call","Send message","Clear search","Back"};
        else if ("contactGroups".equals(page))
            optionItems = new String[]{"Open","Create group","Rename group","Delete group","Group details","Memory status"};
        else if ("groupMembers".equals(page))
            optionItems = new String[]{"Details","Call","Send message","Add member","Remove member",
                    "Rename group","Delete group","Group details"};
        else if ("groupContactPicker".equals(page) || "groupContactAssign".equals(page))
            optionItems = new String[]{"Select"};
        else if ("speedDials".equals(page))
            optionItems = new String[]{"Details","Call","Assign / Change","Remove","Memory status"};
        else if ("speedDialPicker".equals(page) || "speedDialAssign".equals(page))
            optionItems = new String[]{"Select"};
        else if ("contactDetail".equals(page))
            optionItems = new String[]{"Add detail >","Call","Edit","Delete","Send message >",
              "View conversations","Add image >","Use number","Set as default","Change type >",
              "Copy number","Send business card >","Add to group","Speed dial",
              "Add to blacklist","Add to whitelist"};
        else if ("log".equals(page))
            optionItems = new String[]{"View","Call","Clear lists","Restore hidden calls","Call timers"};
        else if ("missed".equals(page) || "received".equals(page) || "dialled".equals(page) || "calllog".equals(page))
            optionItems = new String[]{"Mark >","Mark all","Unmark all","Delete selected","Add to blacklist","Add to whitelist"};
        else if ("settings".equals(page))
            optionItems = isDemoToolsAvailable() ? new String[]{"Open","Demo data (debug)","Search","Help","About APK"} : new String[]{"Open","Search","Help","About APK"};
        else if ("blackpool".equals(page) || "whitepool".equals(page))
            optionItems = new String[]{"Remove"};
        else if ("callsettings".equals(page) || "callfilter".equals(page))
            optionItems = new String[]{"Open","Help"};
        else if ("speedDialMode".equals(page))
            optionItems = new String[]{"Select"};
        else if ("profiles".equals(page))
            optionItems = new String[]{"Activate","Personalise","Timed"};
        else if ("gallery".equals(page))
            optionItems = new String[]{"Open","View photos","View videos","Details","Memory status",
                    "Type of view","Sort","Add folder"};
        else if ("galleryImages".equals(page) || "galleryVideos".equals(page))
            optionItems = new String[]{"Open","Add from phone","Details","Sort",
                    "Remove from gallery","Clear folder","Memory status"};
        else if ("galleryPreview".equals(page))
            optionItems = new String[]{"Details","Remove from gallery","Add from phone"};
        else if ("gallerySort".equals(page))
            optionItems = new String[]{"Select"};
        else if ("media".equals(page))
            optionItems = new String[]{"Open","View photos","View videos","Voice recorder","Settings","Memory in use"};
        else if ("voiceRecorder".equals(page))
            optionItems = voiceRecorder.isRecording()
                    ? new String[]{"Stop and save","Recordings"}
                    : new String[]{"Record","Recordings","Memory status"};
        else if ("voiceRecordings".equals(page))
            optionItems = new String[]{"Play/Stop","Details","Delete","New recording","Memory status"};
        else if ("organiser".equals(page))
            optionItems = new String[]{"Open","Make a note","Week view","Go to date","Go to today","Settings","Memory status"};
        else if ("calendar".equals(page))
            optionItems = new String[]{"View","Make a note","Edit","Delete","Delete all notes","Week view","Go to date","Go to today","Memory status","Go to To-do list"};
        else if ("todo".equals(page))
            optionItems = new String[]{"Open","Add","Delete","Memory status","Go to calendar"};
        else if ("notes".equals(page))
            optionItems = new String[]{"Make a note","Delete","Edit","Use detail >","Send note","Delete all notes","Memory status"};
        else if ("applications".equals(page))
            optionItems = new String[]{"Open","Move","Move to folder","Organise","Add folder","Memory status"};
        else if ("web".equals(page))
            optionItems = new String[]{"Open","Home","Bookmarks","Go to address","Downloads","Settings"};
        else if ("goto".equals(page))
            optionItems = new String[]{"Select","Organise","Help"};
        else if ("gotoOrganise".equals(page))
            optionItems = new String[]{"Toggle","Move up","Move down","Restore defaults","Done"};
        else if ("dial".equals(page))
            optionItems = new String[]{"Call","Save","Send message","Add to contact"};
        else if ("call".equals(page))
            optionItems = new String[]{"Loudspeaker","Mute","Hold","Contacts","Main menu","End call"};
        else if ("music".equals(page))
            optionItems = new String[]{"Music library","Add audio files","Shuffle","Repeat","Equaliser","Track details"};
        else if ("musicLibrary".equals(page))
            optionItems = new String[]{"Play","Add audio files","Remove from library","Track details"};
        else if ("musicEqualiser".equals(page))
            optionItems = new String[]{"Select"};
        else if ("themes".equals(page))
            optionItems = new String[]{"Open","Theme downloads","Type of view","Sort","Search","Memory status"};
        else if ("connectivity".equals(page))
            optionItems = new String[]{"Open","Help"};
        else if ("bluetooth".equals(page))
            optionItems = new String[]{"Open","New search","Details","Delete pairing","Help"};
        else if ("calculator".equals(page))
            optionItems = new String[]{"Scientific calculator","Loan calculator","Instructions","Exit"};
        else if ("scientific".equals(page))
            optionItems = new String[]{"Standard calculator","Loan calculator","Instructions","Exit"};
        else if ("loan".equals(page))
            optionItems = new String[]{"Calculate","Standard calculator","Scientific calculator","Instructions","Exit","Editing options"};
        else if ("maps".equals(page))
            optionItems = new String[]{"Open map","Search","Favourites","Settings"};
        else if ("voicemessages".equals(page))
            optionItems = new String[]{"Call voice mailbox","Voice mailbox no.","Info"};
        else if ("ims".equals(page))
            optionItems = new String[]{"Sign in","Saved conversations","Settings"};
        else if ("browser".equals(page))
            optionItems = new String[]{"Open","Home","Bookmarks","Go to address","Last web addr.","Downloads","Settings"};
        else if ("stopwatch".equals(page))
            optionItems = new String[]{"Split timing","Lap timing"};
        else optionItems = new String[]{"Open","Details","Help"};

        // v6.343: add actual saved-contact lookup/new-contact actions on
        // number-bearing pages. Keep existing Save/Save to contacts entries.
        addContextNumberOptions();
        optionSel = 0;
        optionsOpen = true;
        syncCameraPreviewOverlay();
        invalidate();
    }

    /**
     * The HTML v6.343 number-options rule is contextual. Using the actual
     * selected row (rather than its contact display name) also prevents
     * replying to an unrelated number or saving the wrong contact.
     */
    private String selectedNumberForOptions() {
        AndroidBackend.DeviceSms sms;
        AndroidBackend.DeviceCall call;
        switch (page) {
            case "inbox":
                sms = sel >= 0 && sel < smsInbox.size() ? smsInbox.get(sel) : null;
                return sms == null ? null : sms.address;
            case "messageRead":
                sms = smsById(activeMessageId);
                return sms == null ? null : sms.address;
            case "conversation":
                sms = conversationMessageAt(sel);
                return sms == null ? null : sms.address;
            case "conversations":
                return sel >= 0 && sel < smsThreads.size() ? smsThreads.get(sel).address : null;
            case "sent":
                sms = sentMessageAt(sel);
                return sms == null ? null : sms.address;
            case "missed":
            case "received":
            case "dialled":
            case "calllog":
                call = callRecordForPage(page, sel);
                return call == null ? null : call.number;
            case "dial": return dial;
            case "call": return activeCall;
            case "groupMembers": return selectedGroupMemberNumber();
            case "speedDials": return speedDialNumbers.get(sel + 2);
            default: return null;
        }
    }

    /**
     * Match complete internationally formatted numbers, not merely their last
     * six or eight digits. This avoids displaying the wrong saved contact.
     * Android formatting supplies the local country prefix when appropriate.
     */
    private String contactMatchKey(String value) {
        if (value == null || digitsOnly(value).length() < 7) return "";
        String normalized = android.telephony.PhoneNumberUtils.formatNumberToE164(
                value, Locale.getDefault().getCountry());
        if (normalized != null && !normalized.isEmpty()) return normalized;
        String digits = digitsOnly(value);
        if (digits.startsWith("00") && digits.length() > 9)
            return "+" + digits.substring(2);
        return value.trim().startsWith("+") ? "+" + digits : digits;
    }

    private int matchingContactIndex(String number) {
        String key = contactMatchKey(number);
        if (key.isEmpty()) return -1;
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (key.equals(contactMatchKey(contact.number))) return i;
            ArrayList<String> numbers = contactNumbersById.get(contact.contactId);
            if (numbers != null) {
                for (String candidate : numbers)
                    if (key.equals(contactMatchKey(candidate))) return i;
            }
        }
        return -1;
    }

    /**
     * Keep the visible contact list deduplicated but make each phone-number row
     * independently actionable. Order is stable: primary number first, then
     * secondary Android data rows in the provider's result order.
     */
    private ArrayList<String> currentContactNumbers() {
        ArrayList<String> out = new ArrayList<>();
        if (contacts.isEmpty() || contactIndex < 0 || contactIndex >= contacts.size())
            return out;
        Contact contact = contacts.get(contactIndex);
        HashSet<String> seen = new HashSet<>();
        ArrayList<String> aliases = contactNumbersById.get(contact.contactId);
        if (contact.number != null && !contact.number.trim().isEmpty()) {
            out.add(contact.number);
            String canonical = contactMatchKey(contact.number);
            seen.add(canonical.isEmpty() ? contact.number.trim() : canonical);
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == null || alias.trim().isEmpty()) continue;
                String canonical = contactMatchKey(alias);
                if (seen.add(canonical.isEmpty() ? alias.trim() : canonical))
                    out.add(alias);
            }
        }
        return out;
    }

    private String selectedContactNumber() {
        ArrayList<String> numbers = currentContactNumbers();
        if (numbers.isEmpty()) return null;
        return numbers.get(Math.max(0, Math.min(sel, numbers.size() - 1)));
    }

    private void selectContactNumber(String preferred) {
        ArrayList<String> numbers = currentContactNumbers();
        String key = contactMatchKey(preferred);
        sel = 0;
        if (key.isEmpty()) return;
        for (int i = 0; i < numbers.size(); i++) {
            if (key.equals(contactMatchKey(numbers.get(i)))) {
                sel = i;
                break;
            }
        }
        invalidate();
    }

    private void callSelectedContactNumber() {
        String number = selectedContactNumber();
        if (number == null || digitsOnly(number).isEmpty())
            showNotice("No phone number selected");
        else startCall(number);
    }

    private void composeForContactNumber(String number) {
        if (number == null || digitsOnly(number).isEmpty()) {
            showNotice("No phone number selected");
            return;
        }
        recipient = number;
        messageBody = "";
        composeFocus = 1;
        inputMode = "T9";
        go("compose");
    }

    /** Inject one Nokia number action only where it is not already present. */
    private void addContextNumberOptions() {
        String number = selectedNumberForOptions();
        if (number == null || digitsOnly(number).length() < 3) return;
        ArrayList<String> menu = new ArrayList<>(Arrays.asList(optionItems));
        if ("call".equals(page) && !menu.contains("Send message")) {
            int index = menu.indexOf("Contacts");
            menu.add(index < 0 ? menu.size() : index, "Send message");
        }
        if (!menu.contains("Contact") && !menu.contains("Save a contact") &&
                !menu.contains("Save") && !menu.contains("Save to contacts")) {
            String choice = matchingContactIndex(number) >= 0 ? "Contact" : "Save a contact";
            int before = menu.indexOf("Add to contact");
            menu.add(before < 0 ? menu.size() : before, choice);
        }
        optionItems = menu.toArray(new String[0]);
    }

    private void openNumberContact(String number, boolean openExisting) {
        if (number == null || digitsOnly(number).length() < 3) {
            showNotice("No usable phone number");
            return;
        }
        if (openExisting) {
            // Re-resolve in case Android contacts were edited since Options opened.
            int index = matchingContactIndex(number);
            if (index < 0) {
                showNotice("Contact no longer available");
                return;
            }
            contactIndex = index;
            go("contactDetail");
            // When entering from an SMS/call to a secondary number, highlight
            // THAT number rather than silently defaulting to the primary.
            selectContactNumber(number);
            return;
        }
        // Only prefill. No device or app contact is written without Save.
        editKind = "contact";
        editIndex = -1;
        editBuffer = "\n" + number;
        editField = 0;
        inputMode = "abc";
        go("edit");
    }

    private void chooseOption() {
        String choice = optionItems.length == 0 ? "" : optionItems[optionSel];
        optionsOpen = false;
        syncCameraPreviewOverlay();
        String previousSubmenuTitle = optionSubmenuTitle;
        optionSubmenuTitle = "";

        if (!"smsUseDetails".equals(page) &&
                ("Contact".equals(choice) || "Save a contact".equals(choice))) {
            openNumberContact(selectedNumberForOptions(), "Contact".equals(choice));
            return;
        }
        if ("call".equals(page) && "Send message".equals(choice)) {
            String number = selectedNumberForOptions();
            if (number == null || digitsOnly(number).length() < 3) {
                showNotice("No SMS-capable number");
            } else {
                recipient = number;
                messageBody = "";
                composeFocus = 1;
                inputMode = "T9";
                go("compose");
            }
            return;
        }

        if ("cameraResult".equals(page)) {
            if ("Save".equals(choice)) showNotice(capturedPhotoSaveNotice());
            else if ("Send".equals(choice) && capturedPhotoUri != null &&
                    getContext() instanceof MainActivity)
                ((MainActivity) getContext()).shareCapturedPhoto(capturedPhotoUri);
            else if ("Delete".equals(choice)) requestConfirm("deleteCapturedPhoto", "Delete?");
            return;
        }

        if ("cameraViewfinder".equals(page)) {
            if ("Capture".equals(choice)) {
                // Options temporarily hides the preview; allow it to resume.
                postDelayed(() -> {
                    if ("cameraViewfinder".equals(page)) launchHardwareCapture(false);
                }, 250L);
            } else if ("Switch mode".equals(choice)) go("videoViewfinder");
            else if ("Help".equals(choice))
                showNotice("Photos are saved in C2 and Pictures");
            return;
        }
        if ("videoViewfinder".equals(page)) {
            if ("Record".equals(choice)) {
                // The preview was released while Options was open. Reopen
                // the surface before starting MediaRecorder.
                postDelayed(() -> {
                    if (isInVideoViewfinder() && !videoRecording() && !videoSaving())
                        launchHardwareCapture(true);
                }, 250L);
            } else if ("Switch to camera".equals(choice)) go("cameraViewfinder");
            else if ("Video clips".equals(choice)) go("galleryVideos");
            else if ("Help".equals(choice))
                showNotice("Record, then Stop to save video");
            return;
        }
        if ("settings".equals(page) && "Demo data (debug)".equals(choice)) {
            if (isDemoToolsAvailable()) go("demoTools");
            else showNotice("Debug tools unavailable");
            return;
        }

        if ("shortcuts".equals(page) || "shortcutNavigation".equals(page)) {
            if ("Assign".equals(choice)) openSelected();
            else if ("Restore defaults".equals(choice))
                requestConfirm("resetHomeShortcuts",
                        "Restore Nokia home selection and navigation keys to defaults?");
            else {
                detailTitle = "My shortcuts";
                detailText = "Assign actions to Home keys within this app."
                        + "\nThe Menu must remain accessible on one key."
                        + "\nDefault: Left Go to, Right Names, Centre Menu."
                        + "\nDirectional keys default to unassigned.";
                go("itemDetail");
            }
        } else if ("shortcutAssign".equals(page)) {
            if ("Assign".equals(choice)) openSelected();
        } else if ("demoTools".equals(page)) {
            if ("Select".equals(choice)) openSelected();
            else if ("What is included".equals(choice)) showAuditDemoDetails();
        } else if ("menu".equals(page)) {
            if ("Main menu view".equals(choice)) {
                go("menuViewPicker");
                sel = NativeMenuLayout.modeIndex(menuView);
            } else if ("Organise".equals(choice)) {
                menuOrganising = true;
                menuMovingFrom = -1;
                showNotice("Choose an icon to move");
            } else showNotice("Use directional keys and Select");
        } else if ("menuViewPicker".equals(page)) {
            if ("Select".equals(choice)) openSelected();
        } else if ("goto".equals(page)) {
            if ("Select".equals(choice)) openSelected();
            else if ("Organise".equals(choice)) go("gotoOrganise");
            else showNotice("Select Organise to show, hide and move shortcuts");
        } else if ("gotoOrganise".equals(page)) {
            if ("Toggle".equals(choice)) toggleGoToOrganiseSelection();
            else if ("Move up".equals(choice)) moveGoToShortcut(-1);
            else if ("Move down".equals(choice)) moveGoToShortcut(1);
            else if ("Restore defaults".equals(choice))
                requestConfirm("restoreGoTo", "Restore the original Go to shortcuts and their order?");
            else if ("Done".equals(choice)) {
                back();
                if ("goto".equals(page))
                    sel = Math.min(sel, goToShortcuts.enabledCount() - 1);
            }
        } else if ("messaging".equals(page) || "drafts".equals(page) || "outbox".equals(page) || "sent".equals(page) || "savedmessages".equals(page)) {
            if (("sent".equals(page) || "drafts".equals(page)) && "Open".equals(choice)) openSelected();
            else if ("savedmessages".equals(page) && "Use template".equals(choice))
                useSelectedSmsTemplate();
            else if ("savedmessages".equals(page) && "Details".equals(choice))
                showSelectedSmsTemplateDetails();
            else if ("savedmessages".equals(page) && "Delete template".equals(choice)) {
                if (sel >= 0 && sel < savedSmsTemplates.size())
                    requestConfirm("deleteSmsTemplate", "Delete this app-saved text template?");
                else showNotice("No saved template selected");
            } else if ("savedmessages".equals(page) && "Delete all templates".equals(choice)) {
                if (!savedSmsTemplates.isEmpty())
                    requestConfirm("deleteAllSmsTemplates",
                            "Delete all " + savedSmsTemplates.size() + " locally saved templates?");
                else showNotice("No saved templates");
            }
            else if ("drafts".equals(page) && "Delete draft".equals(choice)) {
                if (sel >= 0 && sel < drafts.size())
                    requestConfirm("deleteDraft", "Delete the selected saved draft?");
                else showNotice("No saved draft");
            } else if ("drafts".equals(page) && "Delete all drafts".equals(choice)) {
                if (!drafts.isEmpty())
                    requestConfirm("deleteAllDrafts", "Delete all " + drafts.size() + " saved drafts?");
                else showNotice("No saved drafts");
            }
            else if ("New message".equals(choice)) {
                recipient = "";
                messageBody = "";
                composeFocus = 0;
                inputMode = "T9";
                go("compose");
            } else if ("Inbox view".equals(choice)) go("inbox");
            else if ("Message log".equals(choice)) {
                showSmsMessageLog();
            } else if ("SIM messages".equals(choice)) {
                detailTitle = "SIM messages";
                detailText = "SIM-stored SMS are not accessible through this app.\n"
                        + "Whether SIM messages exist is unknown.";
                go("itemDetail");
            } else if ("Memory status".equals(choice)) {
                showSmsMemoryStatus();
            } else if ("Folder details".equals(choice)) {
                showSmsFolderDetails();
            }
            else if ("Add mailbox".equals(choice) || "New e-mail".equals(choice)) showNotice("E-mail not configured");
            else if ("IM messages".equals(choice)) go("ims");
            else showNotice("Not implemented: " + choice);
        } else if ("conversations".equals(page)) {
            if ("Call".equals(choice)) {
                if (sel < smsThreads.size()) startCall(smsThreads.get(sel).address);
            } else if ("Conversation details".equals(choice)) {
                if (sel >= 0 && sel < smsThreads.size()) {
                    showConversationDetails(smsThreads.get(sel).threadId);
                } else showNotice("No conversation selected");
            } else if ("Delete conversation".equals(choice)) {
                if (smsThreads.isEmpty()) {
                    showNotice("No conversation selected");
                } else {
                    int n = markedThreadIds.isEmpty() ? 1 : markedThreadIds.size();
                    requestConfirm("deleteThreads", "Hide " + n + (n == 1 ? " conversation" : " conversations") + " in this app? Phone SMS unchanged.");
                }
            } else if ("Inbox view >".equals(choice)) go("inbox");
            else if ("New message >".equals(choice)) {
                recipient = "";
                messageBody = "";
                composeFocus = 0;
                go("compose");
            } else if ("Add to contact".equals(choice) ||
                    "Copy contact".equals(choice) || "Move contact".equals(choice) ||
                    "Merge contact".equals(choice)) {
                showNotice(choice + " not yet implemented");
            } else if ("Mark >".equals(choice)) {
                if (sel < smsThreads.size()) {
                    long id = smsThreads.get(sel).threadId;
                    if (!markedThreadIds.add(id)) markedThreadIds.remove(id);
                    showNotice(markedThreadIds.contains(id) ? "Marked" : "Unmarked");
                }
            } else if ("Mark as read".equals(choice) || "Mark as unread".equals(choice)) {
                boolean unread = "Mark as unread".equals(choice);
                int changed = 0;
                if (!markedThreadIds.isEmpty()) {
                    for (Long id : markedThreadIds)
                        changed += setThreadSmsReadStateLocally(id, unread);
                } else if (sel >= 0 && sel < smsThreads.size()) {
                    changed = setThreadSmsReadStateLocally(smsThreads.get(sel).threadId, unread);
                }
                if (changed > 0) {
                    persistSmsReadOverrides();
                    showNotice(changed + " received SMS marked " + (unread ? "unread" : "read") + " in app");
                } else showNotice("No received SMS to mark");
            } else if ("Mark all".equals(choice)) {
                markedThreadIds.clear();
                for (SmsThread t : smsThreads) markedThreadIds.add(t.threadId);
                showNotice("All marked");
            } else if ("Unmark all".equals(choice)) {
                markedThreadIds.clear();
                showNotice("Marks cleared");
            }
        } else if ("smsUseDetails".equals(page)) {
            useSelectedSmsDetailOption(choice);
        } else if ("conversation".equals(page)) {
            AndroidBackend.DeviceSms m = conversationMessageAt(sel);
            if ("Reply".equals(choice) && m != null) {
                recipient = m.address;
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("Forward".equals(choice)) {
                forwardSmsCopy(m);
            } else if ("Copy as template".equals(choice)) {
                saveSmsAsTemplate(m);
            } else if ("Use detail".equals(choice)) {
                showSmsUseDetails(m);
            } else if ("Message details".equals(choice)) {
                showMessageDetails(m);
            } else if ("Delete".equals(choice)) {
                if (m != null || !markedSmsIds.isEmpty()) {
                    int n = markedSmsIds.isEmpty() ? 1 : markedSmsIds.size();
                    requestConfirm("deleteSms", "Hide " + n + (n == 1 ? " message" : " messages") + " in this app? Phone SMS unchanged.");
                } else showNotice("No message selected");
            } else if ("Call".equals(choice) && m != null) startCall(m.address);
            else if ("Go to Drafts".equals(choice)) go("drafts");
            else if ("Conversation details".equals(choice)) showConversationDetails(activeThreadId);
            else if ("Mark".equals(choice) && m != null) {
                if (!markedSmsIds.add(m.id)) markedSmsIds.remove(m.id);
                showNotice(markedSmsIds.contains(m.id) ? "Marked" : "Unmarked");
            } else if ("Mark as read".equals(choice) || "Mark as unread".equals(choice)) {
                boolean unread = "Mark as unread".equals(choice);
                // Apply to marked incoming SMS in THIS thread, or to the
                // highlighted incoming SMS if nothing is marked. Sent SMS
                // must never acquire an unread envelope or get modified.
                ArrayList<Long> targets = receivedConversationTargets(m);
                int changed = 0;
                for (Long id : targets)
                    if (setSmsReadStateLocally(smsById(id), unread)) changed++;
                if (changed > 0) {
                    persistSmsReadOverrides();
                    showNotice(changed + " received SMS marked " +
                            (unread ? "unread" : "read") + " in app");
                } else showNotice("Select received messages to mark");
            } else if ("Mark all".equals(choice)) {
                markedSmsIds.clear();
                for (AndroidBackend.DeviceSms item : conversationMessages())
                    markedSmsIds.add(item.id);
                showNotice("All messages marked");
            } else if ("Unmark all".equals(choice)) {
                markedSmsIds.clear();
                showNotice("Marks cleared");
            } else showNotice("Not implemented: " + choice);
        } else if ("inbox".equals(page) || "messageRead".equals(page)) {
            // An Inbox Options action must refer to the highlighted row, not a
            // message previously opened in another conversation or folder.
            AndroidBackend.DeviceSms selected;
            if ("inbox".equals(page)) {
                selected = sel >= 0 && sel < smsInbox.size() ? smsInbox.get(sel) : null;
                if (selected != null) {
                    inboxIndex = sel;
                    activeMessageId = selected.id;
                }
            } else selected = smsById(activeMessageId);
            if (selected == null) {
                showNotice("Message unavailable");
            } else if ("Reply".equals(choice) || "Reply as".equals(choice)) {
                if (selected.type != Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    showNotice("Sent message: use Forward");
                } else {
                    recipient = selected.address;
                    messageBody = "";
                    composeFocus = 1;
                    go("compose");
                }
            } else if ("Call".equals(choice)) startCall(selected.address);
            else if ("Copy as template".equals(choice)) saveSmsAsTemplate(selected);
            else if ("Use detail".equals(choice)) showSmsUseDetails(selected);
            else if ("Delete".equals(choice)) requestConfirm("deleteSms", "Hide this message in the app? Phone SMS unchanged.");
            else if ("Forward".equals(choice)) forwardSmsCopy(selected);
            else if ("Edit".equals(choice)) {
                // Editing a received provider SMS is not permitted; make a copy in Compose.
                forwardSmsCopy(selected);
                showNotice("Editing a copy");
            } else if ("New message".equals(choice)) {
                recipient = "";
                messageBody = "";
                composeFocus = 0;
                go("compose");
            } else if ("Conversation view".equals(choice)) {
                activeThreadId = selected.threadId;
                markedSmsIds.clear();
                // Entering a thread marks every received message read in the
                // app, not just a single message opened later.
                if (setThreadSmsReadStateLocally(activeThreadId, false) > 0)
                    persistSmsReadOverrides();
                go("conversation");
            } else if ("Message details".equals(choice)) showMessageDetails(selected);
            else if ("Mark as read".equals(choice) || "Mark as unread".equals(choice)) {
                boolean unread = "Mark as unread".equals(choice);
                if (setSmsReadStateLocally(selected, unread)) {
                    persistSmsReadOverrides();
                    showNotice(unread ? "Unread in this app" : "Read in this app");
                } else showNotice("Select a received SMS");
            } else showNotice("Not implemented: " + choice);
        } else if ("compose".equals(page)) {
            if ("Send".equals(choice)) sendMessage();
            else if ("Preview".equals(choice)) showComposePreview();
            else if ("Add recipient >".equals(choice)) go("recipientPicker");
            else if ("Insert symbol".equals(choice) || "Insert".equals(choice)) go("symbolPicker");
            else if ("Clear field".equals(choice)) messageBody = "";
            else if ("Save message >".equals(choice)) {
                saveCurrentDraft();
                showNotice("Draft saved");
            } else if ("Prediction options >".equals(choice)) {
                inputMode = "T9".equals(inputMode) ? "abc" : "T9";
                showNotice("Prediction " + ("T9".equals(inputMode) ? "on" : "off"));
            } else if ("Exit editor".equals(choice)) back();
            else showNotice("Not implemented: " + choice.replace(" >",""));
        } else if ("contacts".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("Add new".equals(choice)) beginContactEdit(-1);
            else if ("Search".equals(choice)) beginContactSearch();
            else if ("Memory status".equals(choice)) {
                detailTitle = "Contacts memory";
                detailText = "Visible contacts: " + contacts.size()
                        + "\nSource: " + (deviceContactsActive ? "Android contacts" : "App-local contacts")
                        + "\nSpeed dial assignments: " + speedDialNumbers.size()
                        + "\nSIM and cloud capacity unavailable";
                go("itemDetail");
            } else showNotice("Not implemented: " + choice);
        } else if ("contactGroups".equals(page)) {
            NativeContactGroups.Group group = contactGroups.at(sel);
            if ("Open".equals(choice)) openSelected();
            else if ("Create group".equals(choice)) beginGroupEdit(false);
            else if ("Memory status".equals(choice)) {
                int total = 0;
                for (NativeContactGroups.Group entry : contactGroups.groups)
                    total += entry.numbers.size();
                detailTitle = "Groups memory";
                detailText = "Groups: " + contactGroups.groups.size()
                        + "\nMemberships: " + total
                        + "\nLocal app storage; Android contacts unchanged";
                go("itemDetail");
            } else if (group == null) showNotice("Create a group first");
            else if ("Rename group".equals(choice)) {
                activeContactGroupId = group.id;
                go("groupMembers");
                beginGroupEdit(true);
            } else if ("Delete group".equals(choice)) {
                groupConfirmId = group.id;
                requestConfirm("deleteContactGroup", "Delete group '" + group.name
                        + "' from this app? Contacts remain unchanged.");
            } else if ("Group details".equals(choice)) {
                activeContactGroupId = group.id;
                showGroupDetails();
            }
        } else if ("groupMembers".equals(page)) {
            NativeContactGroups.Group group = contactGroups.byId(activeContactGroupId);
            String memberNumber = selectedGroupMemberNumber();
            if (group == null) showNotice("Group unavailable");
            else if ("Details".equals(choice)) {
                if (memberNumber == null) showGroupDetails();
                else openSelected();
            } else if ("Add member".equals(choice)) openGroupMemberPicker();
            else if ("Call".equals(choice)) {
                if (memberNumber == null) showNotice("Select a member first");
                else startCall(memberNumber);
            } else if ("Send message".equals(choice)) {
                if (memberNumber == null) showNotice("Select a member first");
                else {
                    recipient = memberNumber;
                    messageBody = "";
                    composeFocus = 1;
                    go("compose");
                }
            } else if ("Remove member".equals(choice)) {
                if (memberNumber == null) showNotice("Select a member first");
                else {
                    groupConfirmId = group.id;
                    groupConfirmNumber = memberNumber;
                    requestConfirm("removeGroupMember",
                            "Remove this member from the app group? Contact unchanged.");
                }
            } else if ("Rename group".equals(choice)) beginGroupEdit(true);
            else if ("Delete group".equals(choice)) {
                groupConfirmId = group.id;
                requestConfirm("deleteContactGroup", "Delete group '" + group.name
                        + "' from this app? Contacts remain unchanged.");
            } else if ("Group details".equals(choice)) showGroupDetails();
        } else if ("groupContactPicker".equals(page) || "groupContactAssign".equals(page)) {
            if ("Select".equals(choice)) openSelected();
        } else if ("speedDials".equals(page)) {
            int slot = sel + 2;
            String number = speedDialNumbers.get(slot);
            if ("Details".equals(choice)) showSpeedDialDetails(slot);
            else if ("Call".equals(choice)) {
                if (number == null || number.isEmpty()) showNotice("No speed dial assigned");
                else startCall(number);
            } else if ("Assign / Change".equals(choice)) {
                speedDialSlot = slot;
                if (!contacts.isEmpty()) go("speedDialPicker");
                else showNotice("Add a contact first");
            } else if ("Remove".equals(choice)) {
                if (number == null) showNotice("No assignment to remove");
                else requestConfirm("removeSpeedDial", "Remove speed dial " + slot
                        + " from this app? Contact remains unchanged.");
            } else if ("Memory status".equals(choice)) {
                detailTitle = "Speed dials";
                detailText = "Assigned slots: " + speedDialNumbers.size()
                        + " of 8\nStored within this APK only"
                        + "\nAndroid system speed dials are unchanged";
                go("itemDetail");
            }
        } else if ("speedDialPicker".equals(page) || "speedDialAssign".equals(page)) {
            if ("Select".equals(choice)) openSelected();
        } else if ("blackpool".equals(page) || "whitepool".equals(page)) {
            if ("Remove".equals(choice)) openSelected();
        } else if ("callsettings".equals(page) || "callfilter".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else showNotice("Not implemented: " + choice);
        } else if ("speedDialMode".equals(page)) {
            if ("Select".equals(choice)) openSelected();
        } else if ("contactSearch".equals(page)) {
            if ("Open".equals(choice)) openContactSearchResult();
            else if ("Call".equals(choice)) {
                Contact found = selectedContactSearchResult();
                if (found == null) showNotice("No matching contact");
                else startCall(found.number);
            } else if ("Send message".equals(choice)) {
                Contact found = selectedContactSearchResult();
                if (found == null) showNotice("No matching contact");
                else composeForContactNumber(found.number);
            } else if ("Clear search".equals(choice)) {
                contactSearchQuery = "";
                sel = 0;
                resetContactSearchTap();
            } else if ("Back".equals(choice)) back();
        } else if ("contactsNames".equals(page)) {
            if ("Search".equals(choice)) {
                beginContactSearch();
                return;
            }
            if (sel >= contacts.size()) {
                NativeContactGroups.Group group = contactGroups.at(sel - contacts.size());
                if (group != null) {
                    activeContactGroupId = group.id;
                    go("groupMembers");
                } else showNotice("Group unavailable");
                return;
            }
            if (contacts.isEmpty()) {
                if ("Add new >".equals(choice)) beginContactEdit(-1);
                else showNotice("No contact selected");
                return;
            }
            contactIndex = Math.max(0, Math.min(sel, contacts.size()-1));
            Contact ct = contacts.get(contactIndex);
            if ("Call >".equals(choice) || "Send message >".equals(choice) ||
                    "Add new >".equals(choice) || "Edit >".equals(choice)) {
                optionItems = "Call >".equals(choice)
                        ? new String[]{"Call selected"} :
                        "Send message >".equals(choice)
                        ? new String[]{"Message selected"} :
                        "Add new >".equals(choice)
                        ? new String[]{"New contact"} :
                        new String[]{"Edit selected"};
                optionSel = 0;
                optionSubmenuTitle = choice.substring(0, choice.length() - 2);
                optionsOpen = true;
                return;
            }
            if ("Call selected".equals(choice)) startCall(ct.number);
            else if ("Message selected".equals(choice)) {
                recipient = ct.number;
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("New contact".equals(choice)) beginContactEdit(-1);
            else if ("Edit selected".equals(choice)) beginContactEdit(contactIndex);
            else if ("Details".equals(choice)) openSelected();
            else if ("View conversations".equals(choice)) go("conversations");
            else if ("Edit contact".equals(choice)) beginContactEdit(contactIndex);
            else if ("Add to group".equals(choice)) {
                if (contactGroups.groups.isEmpty()) showNotice("Create a group first");
                else {
                    pendingGroupMemberNumber = ct.number;
                    go("groupContactAssign");
                }
            } else if ("Selecting options >".equals(choice)) {
                // Selection operations remain available under Nokia's nested menu.
                optionItems = new String[]{"Mark >","Mark all","Unmark all","Delete selected"};
                optionSubmenuTitle = "Selecting options";
                optionSel = 0;
                optionsOpen = true;
                return;
            } else if ("Call >".equals(choice)) startCall(ct.number);
            else if ("Send message >".equals(choice)) {
                recipient = ct.number;
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("Add new >".equals(choice)) beginContactEdit(-1);
            else if ("Edit >".equals(choice)) beginContactEdit(contactIndex);
            else if ("Delete contact".equals(choice)) {
                requestConfirm("deleteContact", "Delete this contact?");
            } else if ("Mark >".equals(choice)) {
                if (!markedContactIndices.add(contactIndex)) markedContactIndices.remove(contactIndex);
                showNotice(markedContactIndices.contains(contactIndex) ? "Marked" : "Unmarked");
            } else if ("Mark all".equals(choice)) {
                markedContactIndices.clear();
                for (int i = 0; i < contacts.size(); i++) markedContactIndices.add(i);
                showNotice("All contacts marked");
            } else if ("Unmark all".equals(choice)) {
                markedContactIndices.clear();
                showNotice("All marks cleared");
            } else if ("Delete selected".equals(choice)) {
                if (!markedContactIndices.isEmpty())
                    requestConfirm("deleteMarkedContacts", "Delete " + markedContactIndices.size() +
                            " selected contact(s) from the phone?");
                else showNotice("Mark contacts first");
            } else if ("Add to blacklist".equals(choice) || "Add to whitelist".equals(choice))
                addNumberToFilter("Add to blacklist".equals(choice), ct.number);
        } else if ("contactDetail".equals(page)) {
            if (contacts.isEmpty()) {
                showNotice("Contact unavailable");
                return;
            }
            String number = selectedContactNumber();
            if ("Call".equals(choice)) callSelectedContactNumber();
            else if ("Edit".equals(choice)) {
                // Existing editor changes the primary number only; it must
                // never overwrite another selected Android number by accident.
                beginContactEdit(contactIndex);
            } else if ("Delete".equals(choice)) {
                requestConfirm("deleteContact", "Delete this contact?");
            } else if ("Send message >".equals(choice)) {
                composeForContactNumber(number);
            } else if ("View conversations".equals(choice)) go("conversations");
            else if ("Use number".equals(choice)) {
                if (number == null) showNotice("No phone number selected");
                else { dial = number; go("dial"); }
            } else if ("Copy number".equals(choice)) copyNumberToClipboard(number);
            else if ("Add to group".equals(choice)) {
                if (number == null) showNotice("No phone number selected");
                else if (contactGroups.groups.isEmpty()) showNotice("Create a group under Contacts first");
                else {
                    pendingGroupMemberNumber = number;
                    go("groupContactAssign");
                }
            } else if ("Speed dial".equals(choice)) {
                if (number == null) showNotice("No phone number selected");
                else { speedDialAssignment = number; go("speedDialAssign"); }
            } else if ("Add to blacklist".equals(choice) || "Add to whitelist".equals(choice))
                addNumberToFilter("Add to blacklist".equals(choice), number);
            else showNotice(choice.replace(" >",""));
        } else if ("log".equals(page)) {
            if ("View".equals(choice)) openSelected();
            else if ("Call".equals(choice)) showNotice("Select a call from a call list");
            else if ("Clear lists".equals(choice)) {
                if (deviceCalls.isEmpty()) showNotice("No visible calls");
                else requestConfirm("hideAllCalls", "Hide all call records in this app? Android call log unchanged.");
            } else if ("Restore hidden calls".equals(choice)) {
                if (hiddenCallKeys.isEmpty()) showNotice("No hidden calls");
                else requestConfirm("restoreCalls", "Restore " + hiddenCallKeys.size()
                        + " app-hidden call records to the lists?");
            } else if ("Call timers".equals(choice)) {
                long total = 0;
                for (AndroidBackend.DeviceCall item : deviceCalls) total += item.duration;
                detailTitle = "Call timers";
                detailText = "Visible call records: " + deviceCalls.size()
                        + "\nStored call duration: " + formatSeconds(total)
                        + "\nAndroid call log unchanged";
                go("itemDetail");
            } else showNotice("Not implemented: " + choice);
        } else if ("missed".equals(page) || "received".equals(page) || "dialled".equals(page) || "calllog".equals(page)) {
            AndroidBackend.DeviceCall selectedCall = callRecordForPage(page, sel);
            if ("Mark >".equals(choice)) {
                optionSubmenuTitle = "Mark";
                optionItems = new String[]{"Mark", "Unmark"};
                optionSel = 0;
                optionsOpen = true;
                return;
            } else if ("Mark".equals(choice) || "Unmark".equals(choice)) {
                if (selectedCall == null) showNotice("No call selected");
                else {
                    String id = callRecordKey(selectedCall);
                    if ("Mark".equals(choice)) markedCallKeys.add(id);
                    else markedCallKeys.remove(id);
                    showNotice("Mark".equals(choice) ? "Marked" : "Unmarked");
                }
                return;
            } else if ("Mark all".equals(choice)) {
                for (int j = 0; j < callItemsFor(callFilterForPage(page)).length; j++) {
                    AndroidBackend.DeviceCall row = callRecordForPage(page, j);
                    if (row != null) markedCallKeys.add(callRecordKey(row));
                }
                showNotice("All marked");
                return;
            } else if ("Unmark all".equals(choice)) {
                markedCallKeys.clear();
                showNotice("Marks cleared");
                return;
            } else if ("Delete selected".equals(choice)) {
                if (markedCallKeys.isEmpty()) showNotice("Mark calls first");
                else requestConfirm("hideMarkedCalls", "Delete?");
                return;
            } else if ("Add to blacklist".equals(choice) || "Add to whitelist".equals(choice)) {
                if (selectedCall == null) showNotice("No call selected");
                else addNumberToFilter("Add to blacklist".equals(choice), selectedCall.number);
                return;
            }
            if ("View".equals(choice)) showCallDetails(page, sel);
            else if ("Call".equals(choice)) {
                if (selectedCall != null) startCall(selectedCall.number);
                else showNotice("No call selected");
            } else if ("Send message".equals(choice)) {
                if (selectedCall != null) composeToCall(selectedCall);
                else showNotice("No call selected");
            } else if ("Save to contacts".equals(choice)) {
                if (selectedCall != null) saveCallAsContact(selectedCall);
                else showNotice("No call selected");
            } else if ("Delete".equals(choice)) {
                if (selectedCall != null)
                    requestConfirm("hideCall", "Hide this call in the app? Android call log unchanged.");
                else showNotice("No call selected");
            } else if ("Clear list".equals(choice)) {
                if (callItemsFor(callFilterForPage(page)).length > 0 && selectedCall != null)
                    requestConfirm("hideCallList", "Hide all calls in this list? Android call log unchanged.");
                else showNotice("No calls in list");
            } else showNotice("Not implemented: " + choice);
        } else if ("gallerySort".equals(page)) {
            if ("Select".equals(choice)) openSelected();
        } else if ("gallery".equals(page) || "galleryImages".equals(page)
                || "galleryVideos".equals(page) || "galleryPreview".equals(page)) {
            boolean video = "galleryVideos".equals(page);
            if ("Open".equals(choice)) openSelected();
            else if ("View photos".equals(choice)) openGalleryFolder(false);
            else if ("View videos".equals(choice)) openGalleryFolder(true);
            else if ("Add from phone".equals(choice)) {
                pickGalleryFiles(video);
            } else if ("Sort".equals(choice)) {
                if ("gallery".equals(page) && sel != 1 && sel != 2) {
                    showNotice("Open Images or Video clips to sort");
                } else {
                    gallerySortVideos = "galleryVideos".equals(page) ||
                            ("gallery".equals(page) && sel == 2);
                    go("gallerySort");
                    sel = galleryStore.sortMode(gallerySortVideos);
                }
            } else if ("Details".equals(choice) || "Memory status".equals(choice)) {
                showGalleryDetails("Details".equals(choice), video);
            } else if ("Remove from gallery".equals(choice)) {
                if ("galleryPreview".equals(page)) {
                    if (!galleryPreviewUri.isEmpty())
                        requestConfirm("galleryRemovePreview", "Remove this image from the app list? Original unchanged.");
                } else if (galleryStore.at(video, sel) != null)
                    requestConfirm("galleryRemove", "Remove selected item from this app? Original unchanged.");
                else showNotice("No media selected");
            } else if ("Clear folder".equals(choice)) {
                if (!galleryStore.entries(video).isEmpty())
                    requestConfirm("galleryClear", "Clear this app's selection list? Device files unchanged.");
                else showNotice("Folder already empty");
            } else showNotice("Not implemented: " + choice);
        } else if ("voiceRecorder".equals(page)) {
            if ("Record".equals(choice)) beginVoiceRecording();
            else if ("Stop and save".equals(choice)) finishVoiceRecording();
            else if ("Recordings".equals(choice)) {
                if (voiceRecorder.isRecording()) finishVoiceRecording();
                go("voiceRecordings");
            } else if ("Memory status".equals(choice)) showVoiceRecordingMemory();
            else showNotice("Not implemented: " + choice);
        } else if ("voiceRecordings".equals(page)) {
            if ("Play/Stop".equals(choice)) playSelectedVoiceNote();
            else if ("Details".equals(choice)) voiceNoteDetails();
            else if ("Delete".equals(choice)) {
                if (voiceRecorder.at(sel) != null)
                    requestConfirm("deleteVoiceNote", "Delete this saved recording permanently?");
                else showNotice("No recording selected");
            } else if ("New recording".equals(choice)) {
                voiceRecorder.stopPlayback();
                go("voiceRecorder");
            } else if ("Memory status".equals(choice)) showVoiceRecordingMemory();
            else showNotice("Not implemented: " + choice);
        } else if ("media".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("Voice recorder".equals(choice)) go("voiceRecorder");
            else if ("View photos".equals(choice)) openGalleryFolder(false);
            else if ("View videos".equals(choice)) openGalleryFolder(true);
            else if ("Memory in use".equals(choice)) {
                detailTitle = "Media selection";
                detailText = "Images: " + galleryStore.images.size()
                        + "\nVideos: " + galleryStore.videos.size()
                        + "\nSystem storage figures unavailable";
                go("itemDetail");
            } else showNotice("Not implemented: " + choice);
        } else if ("settings".equals(page) || "organiser".equals(page) ||
                   "applications".equals(page) || "web".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("About APK".equals(choice) && "settings".equals(page)) go("aboutNative");
            else showNotice("Not implemented: " + choice);
        } else if ("profiles".equals(page)) {
            if ("Activate".equals(choice)) {
                profile = itemsFor(page)[sel];
                saveSettings();
                showNotice(profile + " activated");
            } else showNotice(choice);
        } else if ("notes".equals(page)) {
            if ("Make a note".equals(choice)) beginTextEdit("note",-1,"");
            else if ("Edit".equals(choice) && !notes.isEmpty()) beginTextEdit("note",sel,notes.get(sel));
            else if ("Delete".equals(choice)) {
                if (sel >= 0 && sel < notes.size()) requestConfirm("deleteNote", "Delete the selected note?");
                else showNotice("No note selected");
            } else if ("Delete all notes".equals(choice)) {
                if (!notes.isEmpty()) requestConfirm("deleteAllNotes", "Delete all " + notes.size() + " notes?");
                else showNotice("No saved notes");
            } else if ("Send note".equals(choice)) {
                if (sel >= 0 && sel < notes.size()) {
                    recipient = "";
                    messageBody = notes.get(sel);
                    inputMode = "T9";
                    composeFocus = 0;
                    go("compose");
                } else showNotice("No note selected");
            } else if ("Use detail >".equals(choice)) {
                if (sel >= 0 && sel < notes.size()) {
                    detailTitle = "Note details";
                    detailText = "Characters: " + notes.get(sel).length() + "\n" + notes.get(sel);
                    go("itemDetail");
                } else showNotice("No note selected");
            } else if ("Memory status".equals(choice)) {
                showOrganiserMemory("Notes", notes);
            } else showNotice("Not implemented: " + choice);
        } else if ("todo".equals(page)) {
            if ("Add".equals(choice)) beginTextEdit("todo",-1,"");
            else if ("Delete".equals(choice)) {
                if (sel >= 0 && sel < todos.size()) requestConfirm("deleteTodo", "Delete this to-do note?");
                else showNotice("No to-do note selected");
            } else if ("Go to calendar".equals(choice)) go("calendar");
            else if ("Open".equals(choice)) openSelected();
            else if ("Memory status".equals(choice)) showOrganiserMemory("To-do list", todos);
            else showNotice("Not implemented: " + choice);
        } else if ("calendar".equals(page)) {
            if ("Make a note".equals(choice)) beginTextEdit("calendar",-1,"");
            else if ("Edit".equals(choice)) {
                if (sel >= 0 && sel < calendar.size()) beginTextEdit("calendar",sel,calendar.get(sel));
                else showNotice("No calendar note selected");
            } else if ("Delete".equals(choice)) {
                if (sel >= 0 && sel < calendar.size()) requestConfirm("deleteCalendar", "Delete selected calendar note?");
                else showNotice("No calendar note selected");
            } else if ("Delete all notes".equals(choice)) {
                if (!calendar.isEmpty()) requestConfirm("deleteAllCalendar", "Delete all " + calendar.size() + " calendar notes?");
                else showNotice("No calendar notes");
            } else if ("Go to To-do list".equals(choice)) go("todo");
            else if ("View".equals(choice)) openSelected();
            else if ("Memory status".equals(choice)) showOrganiserMemory("Calendar", calendar);
            else if ("Go to today".equals(choice)) {
                detailTitle = "Today";
                detailText = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.UK).format(new Date())
                        + "\nCalendar notes: " + calendar.size()
                        + "\nThese saved notes do not have scheduled dates.";
                go("itemDetail");
            } else showNotice("Not implemented: " + choice);
        } else if ("dial".equals(page)) {
            if ("Call".equals(choice)) startCall(dial);
            else if ("Save".equals(choice) && !dial.isEmpty()) {
                editKind = "contact";
                editIndex = -1;
                editBuffer = "\n" + dial;
                editField = 0;
                go("edit");
            } else if ("Send message".equals(choice)) {
                recipient = dial;
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("Add to contact".equals(choice)) showNotice("Adding a number to an existing contact is not implemented");
        } else if ("call".equals(page)) {
            if ("Loudspeaker".equals(choice)) {
                toggleSpeaker();
            } else if ("Contacts".equals(choice)) go("contactsNames");
            else if ("Main menu".equals(choice)) go("menu");
            else if ("End call".equals(choice)) endCall();
            else showNotice(choice);
        } else if ("calculator".equals(page)) {
            if ("Scientific calculator".equals(choice)) go("scientific");
            else if ("Loan calculator".equals(choice)) go("loan");
            else if ("Exit".equals(choice)) {
                history.clear();
                page = "home";
                sel = 0;
            } else showNotice("Use keypad and navigation keys");
        } else if ("scientific".equals(page)) {
            if ("Standard calculator".equals(choice)) backTo("calculator");
            else if ("Loan calculator".equals(choice)) go("loan");
            else if ("Exit".equals(choice)) {
                history.clear();
                page = "home";
                sel = 0;
            } else showNotice("Scientific calculator");
        } else if ("loan".equals(page)) {
            if ("Calculate".equals(choice)) calculateLoan();
            else if ("Standard calculator".equals(choice)) go("calculator");
            else if ("Scientific calculator".equals(choice)) go("scientific");
            else if ("Exit".equals(choice)) {
                history.clear();
                page = "home";
                sel = 0;
            } else showNotice(choice);
        } else if ("music".equals(page) || "musicLibrary".equals(page) ||
                   "musicEqualiser".equals(page)) {
            if ("Music library".equals(choice)) go("musicLibrary");
            else if ("Add audio files".equals(choice)) pickAudioFiles();
            else if ("Shuffle".equals(choice))
                showNotice("Shuffle " + (musicEngine.toggleShuffle() ? "On" : "Off"));
            else if ("Repeat".equals(choice))
                showNotice("Repeat " + musicEngine.nextRepeatMode());
            else if ("Equaliser".equals(choice)) openMusicEqualiser();
            else if ("Play".equals(choice) || "Select".equals(choice)) openSelected();
            else if ("Remove from library".equals(choice)) {
                if (musicEngine.at(sel) != null)
                    requestConfirm("removeMusic", "Remove this track from the app? Original audio unchanged.");
                else showNotice("No track selected");
            } else if ("Track details".equals(choice)) {
                NativeMusicPlayer.Track item = "musicLibrary".equals(page)
                        ? musicEngine.at(sel) : musicEngine.at(musicEngine.index());
                if (item == null) showNotice("No audio loaded");
                else {
                    detailTitle = "Audio file details";
                    detailText = "Name: " + item.name +
                            "\nSource: user-selected Android document" +
                            "\nPlayback: " + (musicEngine.isPlaying() ? "Playing" : "Stopped") +
                            "\nOriginal file unchanged";
                    go("itemDetail");
                }
            } else showNotice("Not implemented: " + choice);
        } else if ("bluetooth".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("New search".equals(choice)) showNotice(bluetooth ? "No devices found" : "Switch Bluetooth on first");
            else showNotice(choice);
        } else if ("browser".equals(page)) {
            if ("Home".equals(choice)) {
                browserUrl = "https://www.nokia.com/";
                invalidate();
            } else if ("Bookmarks".equals(choice)) {
                browserUrl = "about:bookmarks";
                invalidate();
            } else if ("Go to address".equals(choice)) beginTextEdit("url",-1,"http://");
            else if ("Last web addr.".equals(choice)) showNotice(browserUrl);
            else showNotice(choice);
        } else if ("stopwatch".equals(page)) {
            if ("Split timing".equals(choice) || "Lap timing".equals(choice)) showNotice(choice);
        } else {
            if ("Open".equals(choice)) openSelected();
            else showNotice(choice);
        }
        invalidate();
    }

    private void requestConfirm(String action, String text) {
        confirmAction = action;
        confirmText = text;
        confirmOpen = true;
        optionsOpen = false;
        invalidate();
    }

    private void executeConfirmed() {
        String action = confirmAction;
        confirmOpen = false;
        confirmAction = "";
        confirmText = "";

        if ("deleteCapturedPhoto".equals(action)) {
            if (capturedPhotoUri != null && getContext() instanceof MainActivity &&
                    ((MainActivity) getContext()).deleteCapturedPhoto(capturedPhotoUri)) {
                galleryStore.removeUri(false, capturedPhotoUri.toString());
                capturedPhotoUri = null;
                if (capturedPhotoPreview != null) {
                    capturedPhotoPreview.recycle();
                    capturedPhotoPreview = null;
                }
                back();
                showNotice("Photo deleted");
                if (getContext() instanceof MainActivity)
                    post(() -> ((MainActivity) getContext()).showNativeCameraPreview(true));
            } else showNotice("Unable to delete photo");
            return;
        }
        if ("hideMarkedCalls".equals(action)) {
            hiddenCallKeys.addAll(markedCallKeys);
            markedCallKeys.clear();
            prefs.edit().putStringSet("call_hidden_v1", new HashSet<>(hiddenCallKeys)).apply();
            refreshCallsFromPhone();
            sel = 0;
            showNotice("Selected calls hidden in C2");
        } else if ("resetHomeShortcuts".equals(action)) {
            homeShortcuts.restoreDefaults();
            saveHomeShortcuts();
            showNotice("Home shortcuts restored");
        } else if ("clearAuditDemo".equals(action)) {
            changeAuditDemoMode(false);
        } else if ("restoreGoTo".equals(action)) {
            auditGoTo("restore confirmed YES");
            goToShortcuts.restoreDefaults();
            saveGoToShortcuts();
            sel = 0;
            showNotice("Go to shortcuts restored");
        } else if ("deleteContactGroup".equals(action)) {
            String id = groupConfirmId;
            groupConfirmId = "";
            if (contactGroups.delete(id)) {
                if ("groupMembers".equals(page)) back();
                sel = Math.max(0, Math.min(sel, contactGroups.groups.size() - 1));
                showNotice("App group deleted; contacts kept");
            } else showNotice("Group no longer available");
        } else if ("removeGroupMember".equals(action)) {
            String id = groupConfirmId, number = groupConfirmNumber;
            groupConfirmId = "";
            groupConfirmNumber = "";
            if (contactGroups.removeMember(id, number)) {
                NativeContactGroups.Group group = contactGroups.byId(id);
                sel = Math.max(0, Math.min(sel, group == null ? 0 : group.numbers.size()-1));
                showNotice("Removed from app group; contact kept");
            } else showNotice("Group member unavailable");
        } else if ("removeSpeedDial".equals(action)) {
            int slot = sel + 2;
            if (speedDialNumbers.remove(slot) != null) {
                saveSpeedDialSettings();
                showNotice("Speed dial removed from app");
            } else showNotice("Speed dial not assigned");
        } else if ("removeMusic".equals(action)) {
            if (musicEngine.remove(sel)) {
                sel = Math.min(sel, Math.max(0, musicEngine.tracks.size() - 1));
                showNotice("Removed from library; original kept");
            } else showNotice("Track not found");
        } else if ("deleteVoiceNote".equals(action)) {
            if (voiceRecorder.delete(sel)) {
                sel = Math.max(0, Math.min(sel, voiceRecorder.entries().size() - 1));
                showNotice("Recording deleted");
            } else showNotice("Unable to delete recording");
        } else if ("galleryRemove".equals(action) || "galleryRemovePreview".equals(action)) {
            boolean fromPreview = "galleryRemovePreview".equals(action);
            boolean video = "galleryVideos".equals(page);
            int idx = sel;
            if (fromPreview) {
                video = false;
                idx = -1;
                for (int i = 0; i < galleryStore.images.size(); i++) {
                    if (galleryStore.images.get(i).uri.equals(galleryPreviewUri)) { idx = i; break; }
                }
            }
            if (galleryStore.remove(video, idx)) {
                if (fromPreview) {
                    clearGalleryPreview();
                    back();
                } else sel = Math.max(0, Math.min(sel, galleryStore.entries(video).size()-1));
                showNotice("Removed from app; original kept");
            } else showNotice("File not found");
        } else if ("galleryClear".equals(action)) {
            galleryStore.clear("galleryVideos".equals(page));
            sel = 0;
            showNotice("App folder cleared; files unchanged");
        } else if ("deleteNote".equals(action)) {
            if (sel >= 0 && sel < notes.size()) {
                notes.remove(sel);
                saveStringList("notes", notes);
                sel = Math.min(sel, Math.max(0, notes.size() - 1));
                showNotice("Note deleted");
            }
        } else if ("deleteAllNotes".equals(action)) {
            notes.clear();
            saveStringList("notes", notes);
            sel = 0;
            showNotice("All notes deleted");
        } else if ("deleteTodo".equals(action)) {
            if (sel >= 0 && sel < todos.size()) {
                todos.remove(sel);
                saveStringList("todos", todos);
                sel = Math.min(sel, Math.max(0, todos.size() - 1));
                showNotice("To-do note deleted");
            }
        } else if ("deleteCalendar".equals(action)) {
            if (sel >= 0 && sel < calendar.size()) {
                calendar.remove(sel);
                saveStringList("calendar", calendar);
                sel = Math.min(sel, Math.max(0, calendar.size() - 1));
                showNotice("Calendar note deleted");
            }
        } else if ("deleteAllCalendar".equals(action)) {
            calendar.clear();
            saveStringList("calendar", calendar);
            sel = 0;
            showNotice("Calendar notes deleted");
        } else if ("restoreCalls".equals(action)) {
            int n = hiddenCallKeys.size();
            hiddenCallKeys.clear();
            persistHiddenCalls();
            refreshCallsFromPhone();
            sel = 0;
            showNotice(n + " call filters removed");
        } else if ("hideCall".equals(action)) {
            AndroidBackend.DeviceCall chosen = callRecordForPage(page, sel);
            if (chosen != null) {
                hiddenCallKeys.add(callRecordKey(chosen));
                persistHiddenCalls();
                refreshCallsFromPhone();
                sel = Math.min(sel, Math.max(0, callItemsFor(callFilterForPage(page)).length - 1));
                showNotice("Call hidden in app");
            }
        } else if ("hideCallList".equals(action)) {
            int typeFilter = callFilterForPage(page);
            int n = 0;
            for (AndroidBackend.DeviceCall item : deviceCalls) {
                if (typeFilter == 0 || typeFilter == item.type) {
                    hiddenCallKeys.add(callRecordKey(item));
                    n++;
                }
            }
            persistHiddenCalls();
            refreshCallsFromPhone();
            sel = 0;
            showNotice(n + " calls hidden in app");
        } else if ("hideAllCalls".equals(action)) {
            int n = deviceCalls.size();
            for (AndroidBackend.DeviceCall item : deviceCalls)
                hiddenCallKeys.add(callRecordKey(item));
            persistHiddenCalls();
            refreshCallsFromPhone();
            sel = 0;
            showNotice(n + " calls hidden in app");
        } else if ("deleteSmsTemplate".equals(action)) {
            if (sel >= 0 && sel < savedSmsTemplates.size()) {
                savedSmsTemplates.remove(sel);
                saveStringList("sms_templates_v1", savedSmsTemplates);
                sel = Math.min(sel, Math.max(0, savedSmsTemplates.size() - 1));
                showNotice("App template deleted");
            } else showNotice("Saved template no longer available");
        } else if ("deleteAllSmsTemplates".equals(action)) {
            int count = savedSmsTemplates.size();
            savedSmsTemplates.clear();
            saveStringList("sms_templates_v1", savedSmsTemplates);
            sel = 0;
            showNotice(count + " app template(s) deleted");
        } else if ("deleteDraft".equals(action)) {
            if (sel >= 0 && sel < drafts.size()) {
                drafts.remove(sel);
                editingDraftIndex = -1;
                saveDrafts();
                sel = Math.min(sel, Math.max(0, drafts.size() - 1));
                showNotice("Draft deleted");
            }
        } else if ("deleteAllDrafts".equals(action)) {
            drafts.clear();
            editingDraftIndex = -1;
            saveDrafts();
            sel = 0;
            showNotice("Drafts deleted");
        } else if ("removeFromFilter".equals(action)) {
            ArrayList<String> pool = "blackpool".equals(page) ? blacklistNumbers : whitelistNumbers;
            if (sel >= 0 && sel < pool.size()) {
                pool.remove(sel);
                saveFilterSettings();
                sel = Math.min(sel, Math.max(0, pool.size()-1));
                showNotice("Removed");
            }
        } else if ("deleteThreads".equals(action)) {
            HashSet<Long> targets = new HashSet<>();
            if (!markedThreadIds.isEmpty()) targets.addAll(markedThreadIds);
            else if (sel >= 0 && sel < smsThreads.size()) targets.add(smsThreads.get(sel).threadId);
            for (AndroidBackend.DeviceSms m : deviceSms) {
                if (targets.contains(m.threadId)) hiddenSmsIds.add(m.id);
            }
            markedThreadIds.clear();
            saveLongSet("sms_hidden", hiddenSmsIds);
            refreshSmsFromPhone();
            sel = Math.max(0, Math.min(sel, Math.max(0, smsThreads.size() - 1)));
            showNotice("Conversation hidden in app");
        } else if ("deleteSms".equals(action)) {
            HashSet<Long> targets = new HashSet<>();
            // Bulk marks only apply inside the active conversation. Deleting
            // one SMS from Inbox/MessageRead must never hide stale marked IDs.
            if ("conversation".equals(page) && !markedSmsIds.isEmpty()) {
                for (AndroidBackend.DeviceSms m : conversationMessages()) {
                    if (markedSmsIds.contains(m.id)) targets.add(m.id);
                }
            } else if ("conversation".equals(page)) {
                AndroidBackend.DeviceSms m = conversationMessageAt(sel);
                if (m != null) targets.add(m.id);
            } else if (activeMessageId > 0) targets.add(activeMessageId);
            hiddenSmsIds.addAll(targets);
            markedSmsIds.clear();
            saveLongSet("sms_hidden", hiddenSmsIds);
            refreshSmsFromPhone();
            if ("messageRead".equals(page)) back();
            showNotice("SMS hidden in app");
        } else if ("deleteMarkedContacts".equals(action)) {
            if (demoMode) {
                markedContactIndices.clear();
                showNotice("DEMO: Contacts are read-only");
                invalidate();
                return;
            }
            ArrayList<Integer> selected = new ArrayList<>(markedContactIndices);
            java.util.Collections.sort(selected, java.util.Collections.reverseOrder());
            int success = 0;
            int failures = 0;
            for (int idx : selected) {
                if (idx < 0 || idx >= contacts.size()) continue;
                Contact c = contacts.get(idx);
                if (deviceContactsActive) {
                    if (backend.deleteContact(c.contactId)) success++;
                    else failures++;
                } else {
                    contacts.remove(idx);
                    success++;
                }
            }
            if (deviceContactsActive) refreshContactsFromPhone();
            else saveContacts();
            markedContactIndices.clear();
            sel = Math.min(sel, Math.max(0, contacts.size()-1));
            showNotice(success + " deleted" + (failures > 0 ? "; " + failures + " failed" : ""));
        } else if ("deleteContact".equals(action)) {
            deleteContactAt(contactIndex);
            if ("contactDetail".equals(page)) back();
            sel = Math.max(0, sel - 1);
        }
        invalidate();
    }

    /** Read flags inside the emulator never write to the actual Android SMS provider. */
    private void markSmsReadLocally(long id) {
        if (setSmsReadStateLocally(smsById(id), false)) persistSmsReadOverrides();
    }

    private boolean setSmsReadStateLocally(AndroidBackend.DeviceSms message, boolean unread) {
        if (message == null || message.type != Telephony.Sms.MESSAGE_TYPE_INBOX) return false;
        if (unread) {
            localReadSmsIds.remove(message.id);
            localUnreadSmsIds.add(message.id);
        } else {
            localUnreadSmsIds.remove(message.id);
            localReadSmsIds.add(message.id);
        }
        return true;
    }

    private int setThreadSmsReadStateLocally(long threadId, boolean unread) {
        int count = 0;
        for (AndroidBackend.DeviceSms message : deviceSms)
            if (message.threadId == threadId && setSmsReadStateLocally(message, unread))
                count++;
        return count;
    }

    private void persistSmsReadOverrides() {
        saveLongSet("sms_local_read", localReadSmsIds);
        saveLongSet("sms_local_unread", localUnreadSmsIds);
        invalidate();
    }

    private boolean isSmsUnread(AndroidBackend.DeviceSms m) {
        return m != null
                && m.type == Telephony.Sms.MESSAGE_TYPE_INBOX
                && (localUnreadSmsIds.contains(m.id) ||
                        (!m.read && !localReadSmsIds.contains(m.id)));
    }

    private boolean threadUnread(long threadId) {
        for (AndroidBackend.DeviceSms m : deviceSms) {
            if (m.threadId == threadId && isSmsUnread(m)) return true;
        }
        return false;
    }

    private boolean hasUnreadSms() {
        for (AndroidBackend.DeviceSms m : deviceSms) if (isSmsUnread(m)) return true;
        return false;
    }

    /** IDs of received messages inside the open conversation, newest first. */
    private ArrayList<Long> receivedConversationMessageIds() {
        ArrayList<Long> ids = new ArrayList<>();
        for (AndroidBackend.DeviceSms sms : conversationMessages())
            if (sms.type == Telephony.Sms.MESSAGE_TYPE_INBOX) ids.add(sms.id);
        return ids;
    }

    /**
     * Use the shared CI-tested selection semantics for unread/read:
     * marked in-thread inbox entries first; otherwise the highlighted row.
     * Never send a read/unread operation to Android's SMS content provider.
     */
    private ArrayList<Long> receivedConversationTargets(AndroidBackend.DeviceSms highlighted) {
        return NativeSmsBulkSelection.targetReceivedIds(
                receivedConversationMessageIds(), markedSmsIds,
                highlighted == null ? -1L : highlighted.id);
    }

    private ArrayList<AndroidBackend.DeviceSms> conversationMessages() {
        ArrayList<AndroidBackend.DeviceSms> out = new ArrayList<>();
        if (activeThreadId < 0) return out;
        for (AndroidBackend.DeviceSms m : deviceSms) if (m.threadId == activeThreadId) out.add(m);
        return out;
    }

    private AndroidBackend.DeviceSms conversationMessageAt(int index) {
        ArrayList<AndroidBackend.DeviceSms> msgs = conversationMessages();
        return index >= 0 && index < msgs.size() ? msgs.get(index) : null;
    }

    private AndroidBackend.DeviceSms smsById(long id) {
        for (AndroidBackend.DeviceSms m : deviceSms) if (m.id == id) return m;
        return null;
    }

    private String activeMessageAddress() {
        AndroidBackend.DeviceSms m = smsById(activeMessageId);
        return m == null ? inboxAddress(inboxIndex) : m.address;
    }

    private void drawUnreadEnvelope(Canvas c, float x, float y, int color) {
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f);
        c.drawRect(x, y, x + 13, y + 9, p);
        c.drawLine(x, y, x + 6.5f, y + 5, p);
        c.drawLine(x + 13, y, x + 6.5f, y + 5, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawMessageEnvelope(Canvas c, float x, float y, boolean open, int color) {
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f);
        if (!open) {
            c.drawRect(x, y, x + 16, y + 11, p);
            c.drawLine(x, y, x + 8, y + 6, p);
            c.drawLine(x + 16, y, x + 8, y + 6, p);
        } else {
            c.drawLine(x, y + 5, x + 8, y, p);
            c.drawLine(x + 8, y, x + 16, y + 5, p);
            c.drawRect(x, y + 5, x + 16, y + 13, p);
            c.drawLine(x, y + 13, x + 8, y + 7, p);
            c.drawLine(x + 16, y + 13, x + 8, y + 7, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void showNotice(String text) {
        notice = text;
        noticeUntil = SystemClock.uptimeMillis() + 1800;
        invalidate();
    }

    private String inboxName(int i) {
        if (deviceSmsActive && i >= 0 && i < smsInbox.size()) return contactNameFor(smsInbox.get(i).address);
        return "SMS";
    }

    private String inboxAddress(int i) {
        if (deviceSmsActive && i >= 0 && i < smsInbox.size()) return smsInbox.get(i).address;
        return "";
    }

    private String inboxBody(int i) {
        if (deviceSmsActive && i >= 0 && i < smsInbox.size()) return smsInbox.get(i).body;
        return "Allow SMS permission to read phone messages.";
    }

    private String contactNameFor(String number) {
        int index = matchingContactIndex(number);
        return index >= 0 ? contacts.get(index).name :
                (number == null || number.isEmpty() ? "Unknown" : number);
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) b.append(ch);
        }
        return b.toString();
    }

    private void forwardSmsCopy(AndroidBackend.DeviceSms message) {
        if (message == null) {
            showNotice("Message unavailable");
            return;
        }
        // Preserve the original text, but do not copy a destination automatically.
        recipient = "";
        messageBody = message.body == null ? "" : message.body;
        composeFocus = 0;
        inputMode = "T9";
        go("compose");
    }

    private void showMessageDetails(AndroidBackend.DeviceSms message) {
        if (message == null) {
            showNotice("Message unavailable");
            return;
        }
        boolean received = message.type == Telephony.Sms.MESSAGE_TYPE_INBOX;
        String state = received ? (isSmsUnread(message) ? "Unread" : "Read in app")
                : "Stored outgoing (delivery unconfirmed)";
        detailTitle = "Message details";
        detailText = "Type: Text message\nDirection: " + (received ? "Received" : "Outgoing")
                + "\nStatus: " + state
                + "\n" + (received ? "From: " : "To: ") + message.address
                + "\nDate: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                .format(new Date(message.date))
                + "\nLength: " + (message.body == null ? 0 : message.body.length()) + " chars";
        go("itemDetail");
    }

    private void showConversationDetails(long threadId) {
        int received = 0, outgoing = 0, unread = 0;
        long latest = 0L;
        String number = "";
        for (AndroidBackend.DeviceSms sms : deviceSms) {
            if (sms.threadId != threadId) continue;
            if (number.isEmpty()) number = sms.address;
            if (sms.type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                received++;
                if (isSmsUnread(sms)) unread++;
            } else outgoing++;
            if (sms.date > latest) latest = sms.date;
        }
        if (received + outgoing == 0) {
            showNotice("Conversation unavailable");
            return;
        }
        detailTitle = "Conversation details";
        detailText = "Contact: " + contactNameFor(number)
                + "\nNumber: " + number
                + "\nMessages: " + (received + outgoing)
                + "\nReceived: " + received
                + "\nOutgoing: " + outgoing
                + "\nUnread in app: " + unread
                + "\nLast: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                        .format(new Date(latest));
        go("itemDetail");
    }

    private int countSmsType(int targetType) {
        int count = 0;
        for (AndroidBackend.DeviceSms message : deviceSms)
            if (message.type == targetType) count++;
        return count;
    }

    /** Counts refer to the loaded provider snapshot, not to all SMS on the phone. */
    private void showSmsMessageLog() {
        detailTitle = "Message log";
        if (!deviceSmsActive) {
            detailText = "SMS permission not granted.\nNo SMS were loaded.";
        } else {
            int received = 0, unread = 0, outgoing = 0, waiting = 0;
            for (AndroidBackend.DeviceSms m : deviceSms) {
                if (m.type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    received++;
                    if (isSmsUnread(m)) unread++;
                } else {
                    outgoing++;
                    if (m.type == Telephony.Sms.MESSAGE_TYPE_QUEUED ||
                            m.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX) waiting++;
                }
            }
            detailText = "SMS loaded: " + deviceSms.size()
                    + "\nReceived: " + received
                    + "\nUnread in app: " + unread
                    + "\nOther/outgoing: " + outgoing
                    + "\nQueue/outbox: " + waiting
                    + "\nConversations: " + smsThreads.size()
                    + "\nApp drafts: " + drafts.size()
                    + "\nQuery limit: 800 SMS"
                    + "\nLocally hidden SMS excluded."
                    + "\nDelivery not verified.";
        }
        go("itemDetail");
    }

    /**
     * Parse only the highlighted source message. Displaying results never
     * changes Android SMS records or starts any external action.
     */
    private void showSmsUseDetails(AndroidBackend.DeviceSms sms) {
        smsExtractedDetails.clear();
        if (!deviceSmsActive || sms == null) {
            showNotice("SMS unavailable");
            return;
        }
        smsExtractedDetails.addAll(NativeSmsDetails.extract(sms.address, sms.body));
        if (smsExtractedDetails.isEmpty()) {
            showNotice("No usable details in SMS");
            return;
        }
        go("smsUseDetails");
    }

    private NativeSmsDetails.Entry selectedSmsDetail() {
        if (!deviceSmsActive || sel < 0 || sel >= smsExtractedDetails.size())
            return null;
        return smsExtractedDetails.get(sel);
    }

    private void showSelectedSmsDetail() {
        NativeSmsDetails.Entry entry = selectedSmsDetail();
        if (entry == null) {
            showNotice("No detail selected");
            return;
        }
        detailTitle = entry.kind + " detail";
        detailText = entry.value;
        go("itemDetail");
    }

    private void copySmsDetail(String value) {
        if (value == null || value.trim().isEmpty()) {
            showNotice("Nothing to copy");
            return;
        }
        try {
            ClipboardManager clipboard = (ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                showNotice("Clipboard unavailable");
                return;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("SMS detail", value));
            showNotice("Detail copied");
        } catch (RuntimeException ex) {
            showNotice("Clipboard unavailable");
        }
    }

    private void useSelectedSmsDetailOption(String choice) {
        if ("Back".equals(choice)) {
            back();
            return;
        }
        NativeSmsDetails.Entry entry = selectedSmsDetail();
        if (entry == null) {
            showNotice("No detail selected");
            return;
        }
        if ("Details".equals(choice)) showSelectedSmsDetail();
        else if ("Copy".equals(choice)) copySmsDetail(entry.value);
        else if (NativeSmsDetails.Entry.PHONE.equals(entry.kind)) {
            if ("Call".equals(choice)) startCall(entry.value);
            else if ("Send message".equals(choice)) composeForContactNumber(entry.value);
            else if ("Save a contact".equals(choice)) openNumberContact(entry.value, false);
        } else if ("Compose e-mail".equals(choice) &&
                NativeSmsDetails.Entry.EMAIL.equals(entry.kind)) {
            openSmsDetailExternal(new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts("mailto", entry.value, null)));
        } else if ("Open link".equals(choice) &&
                NativeSmsDetails.Entry.WEB.equals(entry.kind)) {
            String url = entry.value.startsWith("www.") ? "https://" + entry.value : entry.value;
            if (url.startsWith("https://") || url.startsWith("http://"))
                openSmsDetailExternal(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            else showNotice("Unsupported link");
        }
    }

    private void openSmsDetailExternal(Intent intent) {
        try {
            getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            showNotice("No compatible Android app available");
        }
    }

    /**
     * Copy only the text from a received/sent provider SMS. Never mutate
     * Android's SMS database, and do not imply message delivery or relocation.
     */
    private void saveSmsAsTemplate(AndroidBackend.DeviceSms sms) {
        if (sms == null || sms.body == null || sms.body.trim().isEmpty()) {
            showNotice("No SMS text to copy");
            return;
        }
        if (savedSmsTemplates.size() >= 100) {
            showNotice("100-template limit reached");
            return;
        }
        savedSmsTemplates.add(0, sms.body);
        saveStringList("sms_templates_v1", savedSmsTemplates);
        showNotice("Copied to app Saved items");
    }

    private void useSelectedSmsTemplate() {
        if (sel < 0 || sel >= savedSmsTemplates.size()) {
            showNotice("No saved template selected");
            return;
        }
        messageBody = savedSmsTemplates.get(sel);
        recipient = "";
        composeFocus = 0;
        inputMode = "T9";
        // Nothing is sent until Send is explicitly chosen.
        go("compose");
    }

    private void showSelectedSmsTemplateDetails() {
        if (sel < 0 || sel >= savedSmsTemplates.size()) {
            showNotice("No saved template selected");
            return;
        }
        String body = savedSmsTemplates.get(sel);
        detailTitle = "Saved template";
        detailText = "App-local text copy\n"
                + "Characters: " + body.length()
                + "\nNo linked system SMS record\n\n" + body;
        go("itemDetail");
    }

    private void showSmsMemoryStatus() {
        detailTitle = "SMS memory status";
        detailText = "App drafts: " + drafts.size()
                + "\nSaved templates: " + savedSmsTemplates.size()
                + "\nLocally hidden: " + hiddenSmsIds.size()
                + (deviceSmsActive ? "\nSMS loaded: " + deviceSms.size()
                       + "\nQuery limit: 800" : "\nSMS permission missing")
                + "\nPhone storage capacity unknown."
                + "\nSIM capacity unknown.";
        go("itemDetail");
    }

    private void showSmsFolderDetails() {
        String folder = "messaging".equals(page)
                ? (sel >= 0 && sel < itemsFor(page).length
                        ? itemsFor(page)[sel] : "Messaging")
                : titleFor(page);
        int count;
        String note;
        switch (folder) {
            case "Drafts":
                count = drafts.size();
                note = "App-saved structured drafts";
                break;
            case "Sent items":
                count = countSmsType(Telephony.Sms.MESSAGE_TYPE_SENT);
                note = "Provider sent records; delivery unconfirmed";
                break;
            case "Outbox":
                count = countSmsType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                note = "Provider outbox snapshot";
                break;
            case "Saved items":
                count = savedSmsTemplates.size();
                note = "App-local copied text templates; no phone SMS moved";
                break;
            case "Conversations":
                count = smsThreads.size();
                note = "App-visible threads";
                break;
            default:
                count = deviceSms.size();
                note = "App-visible message snapshot";
        }
        detailTitle = "Folder details";
        if (!deviceSmsActive && !"Drafts".equals(folder) && !"Saved items".equals(folder)) {
            detailText = "Folder: " + folder + "\nSMS permission required.";
        } else {
            detailText = "Folder: " + folder + "\nLoaded entries: " + count
                    + "\n" + note + "\nNot full storage totals.";
        }
        go("itemDetail");
    }

    private void showComposePreview() {
        detailTitle = "Message preview";
        detailText = "To: " + (recipient.trim().isEmpty() ? "(not entered)" : recipient)
                + "\nCharacters: " + messageBody.length()
                + "\n\n" + (messageBody.isEmpty() ? "(empty message)" : messageBody);
        go("itemDetail");
    }

    private String buildThreadDetail(long threadId) {
        StringBuilder b = new StringBuilder();
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM HH:mm", Locale.UK);
        int count = 0;
        for (int i = deviceSms.size() - 1; i >= 0; i--) {
            AndroidBackend.DeviceSms m = deviceSms.get(i);
            if (m.threadId != threadId) continue;
            b.append(fmt.format(new Date(m.date))).append("  ");
            b.append(m.type == Telephony.Sms.MESSAGE_TYPE_SENT ? "Me: " : "");
            b.append(m.body).append("\n");
            count++;
            if (count >= 20) break;
        }
        return b.length() == 0 ? "(empty)" : b.toString().trim();
    }

    private AndroidBackend.DeviceSms sentMessageAt(int index) {
        if (index < 0) return null;
        int seen = 0;
        for (AndroidBackend.DeviceSms m : deviceSms) {
            if (m.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                if (seen++ == index) return m;
            }
        }
        return null;
    }

    private String[] sentSmsItems() {
        if (!deviceSmsActive) return new String[]{"Allow SMS permission"};
        ArrayList<String> out = new ArrayList<>();
        for (AndroidBackend.DeviceSms m : deviceSms) {
            if (m.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                out.add(contactNameFor(m.address));
                if (out.size() >= 100) break;
            }
        }
        return out.toArray(new String[0]);
    }

    private String[] callItemsFor(int filterType) {
        if (!deviceCallsActive) return new String[]{"Allow call log permission"};
        ArrayList<String> out = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.UK);
        for (AndroidBackend.DeviceCall c : deviceCalls) {
            if (filterType != 0 && c.type != filterType) continue;
            String label = c.name == null || c.name.isEmpty() ? contactNameFor(c.number) : c.name;
            out.add(label + "  " + fmt.format(new Date(c.date)));
        }
        return out.toArray(new String[0]);
    }

    private int callFilterForPage(String pg) {
        if ("missed".equals(pg)) return CallLog.Calls.MISSED_TYPE;
        if ("received".equals(pg)) return CallLog.Calls.INCOMING_TYPE;
        if ("dialled".equals(pg)) return CallLog.Calls.OUTGOING_TYPE;
        return 0;
    }

    private String callRecordKey(AndroidBackend.DeviceCall call) {
        return call.number + "|" + call.type + "|" + call.date + "|" + call.duration;
    }

    private void persistHiddenCalls() {
        prefs.edit().putStringSet("call_hidden_v1", new HashSet<>(hiddenCallKeys)).apply();
    }

    private void showCallDetails(String pg, int index) {
        AndroidBackend.DeviceCall item = callRecordForPage(pg, index);
        if (item == null) {
            showNotice(deviceCallsActive ? "No call selected" : "Allow call log permission");
            return;
        }
        String kind;
        if (item.type == CallLog.Calls.MISSED_TYPE) kind = "Missed";
        else if (item.type == CallLog.Calls.INCOMING_TYPE) kind = "Received";
        else if (item.type == CallLog.Calls.OUTGOING_TYPE) kind = "Dialled";
        else kind = "Other";
        detailTitle = "Call details";
        detailText = "Type: " + kind
                + "\nName: " + (item.name.isEmpty() ? contactNameFor(item.number) : item.name)
                + "\nNumber: " + (item.number.isEmpty() ? "Unavailable" : item.number)
                + "\nDate: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                    .format(new Date(item.date))
                + "\nDuration: " + formatSeconds(item.duration);
        go("itemDetail");
    }

    private void composeToCall(AndroidBackend.DeviceCall call) {
        if (digitsOnly(call.number).isEmpty()) {
            showNotice("No SMS-capable number");
            return;
        }
        recipient = call.number;
        messageBody = "";
        composeFocus = 1;
        inputMode = "T9";
        go("compose");
    }

    private void saveCallAsContact(AndroidBackend.DeviceCall call) {
        if (digitsOnly(call.number).isEmpty()) {
            showNotice("No number to save");
            return;
        }
        String display = call.name == null ? "" : call.name.trim();
        // Keep the name editable; the user must confirm Save before writing
        // to Android Contacts (and only if Contacts permission is granted).
        editKind = "contact";
        editIndex = -1;
        editBuffer = display + "\n" + call.number;
        editField = 0;
        inputMode = "abc";
        go("edit");
    }

    private AndroidBackend.DeviceCall callRecordForPage(String pg, int index) {
        int filter = 0;
        if ("missed".equals(pg)) filter = CallLog.Calls.MISSED_TYPE;
        else if ("received".equals(pg)) filter = CallLog.Calls.INCOMING_TYPE;
        else if ("dialled".equals(pg)) filter = CallLog.Calls.OUTGOING_TYPE;
        int n = 0;
        for (AndroidBackend.DeviceCall c : deviceCalls) {
            if (filter != 0 && c.type != filter) continue;
            if (n == index) return c;
            n++;
        }
        return null;
    }

    private String callNumberAt(String pg, int index) {
        AndroidBackend.DeviceCall c = callRecordForPage(pg, index);
        if (c != null) return c.number;
        return "";
    }

    private void deleteContactAt(int index) {
        if (demoMode) {
            showNotice("DEMO: Contacts are read-only");
            return;
        }
        if (index < 0 || index >= contacts.size()) {
            showNotice("Contact unavailable");
            return;
        }
        Contact c = contacts.get(index);
        if (deviceContactsActive) {
            boolean deleted = backend.deleteContact(c.contactId);
            // Refresh on success AND failure so stale rows cannot mislead the user.
            refreshContactsFromPhone();
            if (!deleted) {
                showNotice("Unable to delete contact");
                return;
            }
            for (Contact remaining : contacts) {
                if (remaining.contactId == c.contactId) {
                    showNotice("Contact still on device");
                    return;
                }
            }
        } else {
            contacts.remove(index);
            saveContacts();
        }
        sel = Math.max(0, Math.min(sel, contacts.size() - 1));
        showNotice("Contact deleted");
    }

    private String formatSeconds(long sec) {
        return String.format(Locale.UK, "%02d:%02d", sec / 60, sec % 60);
    }

    private String formatMillis(long ms) {
        long cs = (ms / 10) % 100;
        long sec = (ms / 1000) % 60;
        long min = (ms / 60000);
        return String.format(Locale.UK, "%02d:%02d.%02d", min, sec, cs);
    }

    private boolean isDigit(String key) {
        return key != null && key.length() == 1 && key.charAt(0) >= '0' && key.charAt(0) <= '9';
    }

    private boolean isNumberKey(String key) {
        return isDigit(key) || "*".equals(key) || "#".equals(key);
    }

    private void cancelPendingSpeedDialHold() {
        if (pendingSpeedDialHold != null) {
            removeCallbacks(pendingSpeedDialHold);
            pendingSpeedDialHold = null;
        }
    }

    private void cancelKeypadTouch() {
        cancelPendingSpeedDialHold();
        touchPointerId = -1;
        touchPressedKey = null;
        touchLongConsumed = false;
    }

    private void scheduleSpeedDialHold(String key) {
        if (!"home".equals(page) || locked || confirmOpen || optionsOpen ||
                key == null || key.length() != 1 ||
                key.charAt(0) < '2' || key.charAt(0) > '9')
            return;
        final int slot = key.charAt(0) - '0';
        final String originalKey = key;
        final Runnable hold = new Runnable() {
            @Override public void run() {
                if (pendingSpeedDialHold != this || touchPointerId < 0 ||
                        touchLongConsumed || !originalKey.equals(touchPressedKey) ||
                        !"home".equals(page) || locked || confirmOpen || optionsOpen)
                    return;
                pendingSpeedDialHold = null;
                touchLongConsumed = true; // release must NOT also enter the digit.
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                String number = speedDialNumbers.get(slot);
                if (!speedDiallingEnabled) {
                    showNotice("Speed dialling Off; enable under Call settings");
                } else if (number == null || number.trim().isEmpty()) {
                    showNotice("Speed dial " + slot + " not assigned");
                } else {
                    // Match the HTML's offline dial screen; a real call needs
                    // a second explicit Call key press.
                    dial = number;
                    go("dial");
                    showNotice("Speed dial " + slot + " ready; press Call");
                }
            }
        };
        pendingSpeedDialHold = hold;
        postDelayed(hold, SPEED_DIAL_HOLD_MS);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelKeypadTouch();
            touchPointerId = e.getPointerId(0);
            touchStartX = (e.getX(0) - offX) / fitScale;
            touchStartY = (e.getY(0) - offY) / fitScale;
            touchPressedKey = hitKey(touchStartX, touchStartY);
            scheduleSpeedDialHold(touchPressedKey);
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN ||
                action == MotionEvent.ACTION_CANCEL) {
            cancelKeypadTouch();
            return true;
        }
        if (touchPointerId < 0) return true;
        int pointer = e.findPointerIndex(touchPointerId);
        if (pointer < 0) {
            cancelKeypadTouch();
            return true;
        }
        float x = (e.getX(pointer) - offX) / fitScale;
        float y = (e.getY(pointer) - offY) / fitScale;
        if (action == MotionEvent.ACTION_MOVE) {
            if (Math.abs(x - touchStartX) > KEYPAD_DRAG_TOLERANCE ||
                    Math.abs(y - touchStartY) > KEYPAD_DRAG_TOLERANCE ||
                    !java.util.Objects.equals(hitKey(x, y), touchPressedKey))
                cancelKeypadTouch();
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            String key = touchPressedKey;
            boolean consumed = touchLongConsumed;
            boolean sameKey = java.util.Objects.equals(key, hitKey(x, y));
            cancelKeypadTouch();
            if (!consumed && sameKey && key != null) {
                // Home's on-screen centre Menu label must activate the grid.
                // The hardware OK button still honours remapped shortcuts.
                // A centre LCD softkey tap routes through the same Home
                // assignment as hardware OK. Do not require a literal "Menu"
                // label: assigned labels may differ from firmware defaults.
                press(key);
            }
            return true;
        }
        return true;
    }

    private String hitKey(float x, float y) {
        // The Home LCD centre label visually sits above the navigation
        // controls. Provide a generous *Home-only* target so tapping the
        // visible Menu text cannot land in the small inactive bezel gap.
        // Physical OK remains in its own d-pad rectangle below y=455.
        if ("home".equals(page) && x >= SX + SW / 3f &&
                x <= SX + 2f * SW / 3f &&
                y >= SY + SH * 276f / 320f && y < 432f)
            return "OK";
        // Touch-screen representation of the centre Nokia softkey shares
        // the Home Menu binding with the physical OK button.
        // The LCD softkey labels are touch targets on EVERY page.
        // Previously only Home's centre label was mapped, leaving End,
        // Details, Select and other centre softkeys inert.
        if (x >= SX && x <= SX + SW &&
                y >= SY + SH * 286f / 320f && y <= SY + SH) {
            if (x < SX + SW / 3f) return "LSK";
            if (x > SX + 2f * SW / 3f) return "RSK";
            return "OK";
        }
        if (new RectF(55, 402, 125, 430).contains(x,y)) return "LSK";
        if (new RectF(235, 402, 305, 430).contains(x,y)) return "RSK";
        if (new RectF(55, 448, 137, 497).contains(x,y)) return "CALL";
        if (new RectF(223, 448, 305, 497).contains(x,y)) return "END";

        if (new RectF(137, 432, 223, 515).contains(x,y)) {
            if (new RectF(160, 455, 200, 493).contains(x,y)) return "OK";
            float dx = x - 180, dy = y - 474;
            if (Math.abs(dx) > Math.abs(dy)) return dx < 0 ? "LEFT" : "RIGHT";
            return dy < 0 ? "UP" : "DOWN";
        }

        float[] xs = {48,138,228};
        float[] ys = {535,588,641,694};
        String[] ks = {"1","2","3","4","5","6","7","8","9","*","0","#"};
        for (int r = 0; r < 4; r++) {
            for (int col = 0; col < 3; col++) {
                if (new RectF(xs[col], ys[r], xs[col] + 84, ys[r] + 45).contains(x,y))
                    return ks[r*3+col];
            }
        }
        return null;
    }

    private static class PageState {
        final String page;
        final int sel;
        PageState(String page, int sel) {
            this.page = page;
            this.sel = sel;
        }
    }

    private static class SmsThread {
        final long threadId;
        final String address;
        final String body;
        final long date;
        SmsThread(long threadId, String address, String body, long date) {
            this.threadId = threadId;
            this.address = address == null ? "" : address;
            this.body = body == null ? "" : body;
            this.date = date;
        }
    }

    private static class DraftMessage {
        final String recipient;
        final String body;
        final String inputMode;
        final long createdAt;
        DraftMessage(String recipient, String body, String inputMode, long createdAt) {
            this.recipient = recipient == null ? "" : recipient;
            this.body = body == null ? "" : body;
            this.inputMode = inputMode == null || inputMode.isEmpty() ? "T9" : inputMode;
            this.createdAt = createdAt;
        }
    }

    private static class Contact {
        final String name;
        final String number;
        final long contactId;
        final long rawContactId;
        final long phoneDataId;
        final boolean device;

        Contact(String name, String number) {
            this(name, number, -1, -1, -1, false);
        }

        Contact(String name, String number, long contactId, long rawContactId, long phoneDataId, boolean device) {
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
            this.contactId = contactId;
            this.rawContactId = rawContactId;
            this.phoneDataId = phoneDataId;
            this.device = device;
        }
    }
}
