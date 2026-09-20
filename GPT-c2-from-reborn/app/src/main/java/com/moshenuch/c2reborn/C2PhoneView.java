package com.moshenuch.c2reborn;

import android.content.Context;
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
import java.util.List;
import java.util.Locale;

public class C2PhoneView extends View {
    private static final float DW = 360f;
    private static final float DH = 780f;
    private static final float SX = 73f;
    private static final float SY = 96f;
    private static final float SW = 214f;
    private static final float SH = 286f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap wallpaper;
    private final Bitmap[] menuIcons = new Bitmap[9];
    private final HashMap<String, Bitmap> listIcons = new HashMap<>();
    private final SharedPreferences prefs;
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

    private final ArrayList<Contact> contacts = new ArrayList<>();
    private final ArrayList<String> notes = new ArrayList<>();
    private final ArrayList<String> todos = new ArrayList<>();
    private final ArrayList<String> calendar = new ArrayList<>();
    private final ArrayList<String> drafts = new ArrayList<>();

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

    private boolean musicPlaying = false;
    private int musicTrack = 0;
    private long musicStarted = 0L;
    private long musicElapsed = 0L;

    private String browserUrl = "http://www.nokia.com/";
    private String detailTitle = "";
    private String detailText = "";
    private int inboxIndex = 0;
    private int contactIndex = 0;

    private boolean optionsOpen = false;
    private int optionSel = 0;
    private boolean locked = false;
    private int unlockStep = 0;
    private String[] optionItems = new String[0];

    private final String[] mainMenu = {
            "Contacts", "Organiser", "Media",
            "Gallery", "Messaging", "Apps.",
            "Log", "Settings", "STORE"
    };
    private final int[] mainMenuIconIndex = {1,6,5,4,0,7,2,3,8};

    private final String[] tracks = {
            "Nokia Tune", "Demo track", "Acoustic sample", "Voice recording"
    };

    private final String[] t9Words = {
            "a","am","an","and","are","as","at","be","call","can","come","day","do","for",
            "good","hello","home","how","i","in","is","it","later","love","me","meeting",
            "message","my","no","nokia","now","of","ok","okay","on","one","phone","please",
            "see","send","test","thanks","the","this","time","to","today","tomorrow","we",
            "will","with","world","yes","you","your"
    };

    public C2PhoneView(Context context) {
        super(context);
        wallpaper = BitmapFactory.decodeResource(getResources(), R.drawable.screen_wallpaper);
        for (int i = 0; i < menuIcons.length; i++) menuIcons[i] = RebornAssets.main(i);
        String[] ids = {"0595","0540","0513","0628","0715","0703","0558","0599","0593","0922","0917","0562","0570","1066","0415","0724"};
        for (String id : ids) listIcons.put(id, RebornAssets.list(id));
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        setFocusableInTouchMode(true);
        prefs = context.getSharedPreferences("c2_state", Context.MODE_PRIVATE);
        loadState();
    }

    private void loadState() {
        profile = prefs.getString("profile", "General");
        theme = prefs.getString("theme", "Dark");
        if ("Nokia".equals(theme)) theme = "Dark";
        bluetooth = prefs.getBoolean("bluetooth", false);
        alarmTime = prefs.getString("alarmTime", "07:00");
        alarmOn = prefs.getBoolean("alarmOn", false);
        alarmRepeat = prefs.getBoolean("alarmRepeat", false);

        loadStringList("notes", notes);
        loadStringList("todos", todos);
        loadStringList("calendar", calendar);
        loadStringList("drafts", drafts);

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

    private void saveStringList(String key, List<String> values) {
        JSONArray a = new JSONArray();
        for (String s : values) a.put(s);
        prefs.edit().putString(key, a.toString()).apply();
    }

    private void saveContacts() {
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
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(18);
        c.drawText("NOKIA", 180, 61, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
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
        else if ("calculator".equals(page)) drawCalculator(c);
        else if ("loan".equals(page)) drawLoan(c);
        else if ("stopwatch".equals(page)) drawStopwatch(c);
        else if ("countdown".equals(page)) drawCountdown(c);
        else if ("music".equals(page)) drawMusic(c);
        else if ("browser".equals(page)) drawBrowser(c);
        else if ("messageRead".equals(page)) drawMessageRead(c);
        else if ("contactDetail".equals(page)) drawContactDetail(c);
        else if ("edit".equals(page)) drawEditor(c);
        else if ("alarmEdit".equals(page)) drawAlarmEdit(c);
        else if ("itemDetail".equals(page)) drawItemDetail(c);
        else drawListPage(c);

        drawSoftBar(c);

        if (optionsOpen) drawOptions(c);
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
        p.setColor(Color.WHITE);
        p.setStrokeWidth(1.4f);
        for (int i = 0; i < 4; i++) {
            float h = 3 + i * 2.3f;
            c.drawRect(8 + i * 4, 15 - h, 10 + i * 4, 15, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f);
        c.drawRect(28, 7, 38, 14, p);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(30, 9, 35, 12, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(14);
        c.drawText(new SimpleDateFormat("HH:mm", Locale.UK).format(new Date()), 232, 17, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(16);
        c.drawText(title, 6, 37, p);
    }

    private void drawHome(Canvas c) {
        if (wallpaper != null) {
            c.drawBitmap(wallpaper, null, new RectF(0, 0, 240, 296), p);
        } else {
            p.setShader(new LinearGradient(0, 0, 240, 300,
                    Color.rgb(40, 91, 149), Color.rgb(103, 49, 137), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 240, 296, p);
            p.setShader(null);
        }
        p.setColor(Color.argb(28, 0, 0, 0));
        c.drawRect(0, 0, 240, 296, p);

        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(10);
        c.drawText(profile, 8, 15, p);

        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(10);
        c.drawText(bluetooth ? "ᛒ  ▮▮▮" : "▮▮▮", 232, 15, p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(38);
        c.drawText(new SimpleDateFormat("HH:mm", Locale.UK).format(new Date()), 120, 94, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(13);
        c.drawText(new SimpleDateFormat("EEE, d MMM", Locale.UK).format(new Date()), 120, 117, p);

        if (alarmOn) {
            p.setTextSize(11);
            c.drawText("Alarm " + alarmTime, 120, 142, p);
        }

        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(11);
        c.drawText("GPT c2 from reborn", 10, 281, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Native", 230, 281, p);
    }

    private void drawMenu(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        drawStatus(c, "Menu");
        for (int i = 0; i < mainMenu.length; i++) {
            int col = i % 3, row = i / 3;
            float x = 4 + col * 78;
            float y = 43 + row * 78;
            RectF r = new RectF(x, y, x + 75, y + 72);
            if (i == sel) {
                p.setShader(new LinearGradient(0, y, 0, y + 72, Color.rgb(92,92,96), Color.rgb(18,18,20), Shader.TileMode.CLAMP));
                c.drawRoundRect(r, 5, 5, p);
                p.setShader(null);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1);
                p.setColor(Color.LTGRAY);
                c.drawRoundRect(r, 5, 5, p);
                p.setStyle(Paint.Style.FILL);
            }
            Bitmap icon = menuIcons[mainMenuIconIndex[i]];
            if (icon != null) c.drawBitmap(icon, null, new RectF(x + 20, y + 6, x + 55, y + 41), p);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(11);
            p.setTypeface(android.graphics.Typeface.DEFAULT);
            c.drawText(mainMenu[i], x + 37.5f, y + 60, p);
        }
    }

    private void drawListPage(Canvas c) {
        if (wallpaper != null) c.drawBitmap(wallpaper, null, new RectF(0, 18, 240, 296), p);
        String title = "conversations".equals(page) ? "Conversations " + (sel + 1) + "/179" : titleFor(page);
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
                && (page.equals("messaging") || page.equals("settings") || page.equals("contacts") || page.equals("gallery") || page.equals("media"));
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
            Bitmap icon = showIcons ? iconForRow(page, idx) : null;
            if (icon != null) c.drawBitmap(icon, null, new RectF(10, y + 8, 40, y + 38), p);
            p.setColor(textColor);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(15);
            p.setTypeface(selected ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            c.drawText(items[idx], textX, y + 24, p);
            p.setTypeface(android.graphics.Typeface.DEFAULT);
            String sub = subtitleFor(page, idx);
            if (!sub.isEmpty()) {
                p.setColor(selected ? Color.DKGRAY : Color.LTGRAY);
                p.setTextSize(11);
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

    private Bitmap iconForRow(String pg, int idx) {
        String id = null;
        if ("messaging".equals(pg)) {
            String[] ids = {"0570","1066","0415","0724","0570","0599","0570","0570","0570","0570","0570","0570","0570","0595"};
            if (idx < ids.length) id = ids[idx];
        } else if ("settings".equals(pg)) {
            String[] ids = {"0595","0540","0513","0628","0715","0703","0558","0599"};
            if (idx < ids.length) id = ids[idx];
        } else if ("contacts".equals(pg)) {
            String[] ids = {"0593","0922","0917","0599","0562","0558","0589","0572"};
            if (idx < ids.length) id = ids[idx];
        }
        Bitmap b = id == null ? null : listIcons.get(id);
        if (b != null) return b;
        if ("messaging".equals(pg)) return menuIcons[0];
        if ("contacts".equals(pg)) return menuIcons[1];
        if ("settings".equals(pg)) return menuIcons[3];
        if ("gallery".equals(pg)) return menuIcons[4];
        if ("media".equals(pg)) return menuIcons[5];
        return null;
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
        if ("conversations".equals(pg)) {
            if (idx == 0) return "Sent 13:12";
            if (idx == 1) return "Received 11:57";
            if (idx == 2) return "Sent 12:49";
            if (idx == 3) return "Received 13:03";
        }
        if ("messaging".equals(pg)) {
            if (idx == 1) return "179 conversations";
            if (idx == 2) return "44 messages";
        }
        if ("settings".equals(pg) && idx == 1) return "Dark.nth";
        if ("drafts".equals(pg) && idx < drafts.size()) return "Draft";
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
        c.drawText(inputMode + "  " + messageBody.length() + "/160", 229, 288, p);
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
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(20);
        c.drawText(activeCall, 120, 86, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(13);
        long secs = Math.max(0, (SystemClock.elapsedRealtime() - callStarted) / 1000L);
        c.drawText(formatSeconds(secs), 120, 119, p);
        p.setTextSize(12);
        c.drawText(loudspeaker ? "Loudspeaker on" : "Handset", 120, 151, p);
        p.setColor(Color.rgb(66,156,74));
        c.drawCircle(120, 210, 26, p);
        p.setColor(Color.WHITE);
        p.setTextSize(22);
        c.drawText("☏", 120, 218, p);
    }

    private void drawMessageRead(Canvas c) {
        String name = inboxName(inboxIndex);
        drawStatus(c, name);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(12);
        c.drawText("Received", 8, 38, p);
        p.setTextSize(14);
        drawWrappedText(c, inboxBody(inboxIndex), 8, 66, 220, 18, 11);
    }

    private void drawContactDetail(Canvas c) {
        Contact ct = contacts.get(Math.max(0, Math.min(contactIndex, contacts.size() - 1)));
        drawStatus(c, "Contact details");
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(40);
        c.drawText("●", 120, 92, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(18);
        c.drawText(ct.name, 120, 135, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(15);
        c.drawText(ct.number, 120, 165, p);
        p.setTextSize(11);
        c.drawText("Press green key to call", 120, 210, p);
    }

    private void drawEditor(Canvas c) {
        String ttl = "Edit";
        if ("note".equals(editKind)) ttl = editIndex >= 0 ? "Edit note" : "New note";
        if ("todo".equals(editKind)) ttl = "To-do note";
        if ("calendar".equals(editKind)) ttl = "Calendar note";
        if ("contact".equals(editKind)) ttl = editIndex >= 0 ? "Edit contact" : "New contact";
        drawStatus(c, ttl);

        if ("contact".equals(editKind)) {
            String[] parts = editBuffer.split("\\n", -1);
            String name = parts.length > 0 ? parts[0] : "";
            String num = parts.length > 1 ? parts[1] : "";
            drawEditorField(c, "Name", name, 28, editField == 0);
            drawEditorField(c, "Number", num, 92, editField == 1);
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
        p.setColor("Dark".equals(theme) ? Color.rgb(25,31,33) : Color.WHITE);
        c.drawRect(8, 30, 232, 95, p);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(28);
        c.drawText(calc, 223, 73, p);

        String[] ops = {"▲ ×","◀ −","OK =","▶ +","▼ ÷","* .   # ±"};
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(13);
        for (int i = 0; i < ops.length; i++) c.drawText(ops[i], 120, 125 + i * 24, p);
    }

    private void drawLoan(Canvas c) {
        drawStatus(c, "Loan calculator");
        String[] labels = {"Loan amount", "Interest %", "Months"};
        String[] values = {loanPrincipal, loanRate, loanMonths};
        for (int i = 0; i < 3; i++) {
            float y = 35 + i * 62;
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
        c.drawText("♪", 120, 92, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(16);
        c.drawText(tracks[musicTrack], 120, 128, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(11);
        c.drawText((musicTrack + 1) + "/" + tracks.length, 120, 148, p);

        long elapsed = musicElapsed;
        if (musicPlaying) elapsed += SystemClock.elapsedRealtime() - musicStarted;
        long sec = (elapsed / 1000) % 240;
        p.setTextSize(13);
        c.drawText(formatSeconds(sec) + " / 04:00", 120, 184, p);

        p.setColor(Color.rgb(185,190,190));
        c.drawRect(30, 200, 210, 205, p);
        p.setColor(accent());
        c.drawRect(30, 200, 30 + 180 * sec / 240f, 205, p);

        p.setColor(themeText());
        p.setTextSize(12);
        c.drawText("◀     " + (musicPlaying ? "Pause" : "Play") + "     ▶", 120, 240, p);
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

    private void drawItemDetail(Canvas c) {
        drawStatus(c, detailTitle.isEmpty() ? "Details" : detailTitle);
        p.setColor(themeText());
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(13);
        drawWrappedText(c, detailText, 10, 48, 215, 18, 12);
    }

    private void drawSoftBar(Canvas c) {
        p.setColor(Color.BLACK);
        c.drawRect(0, 286, 240, 320, p);
        String[] soft = softLabels();
        p.setColor(Color.WHITE);
        p.setTextSize(14);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(soft[0], 6, 307, p);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(soft[1], 120, 307, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(soft[2], 234, 307, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    private String[] softLabels() {
        if (locked) return new String[]{"Unlock", "", ""};
        if ("home".equals(page)) return new String[]{"Go to", "Menu", "Names"};
        if ("menu".equals(page)) return new String[]{"Options", "Select", "Exit"};
        if ("conversations".equals(page)) return new String[]{"Options", "Open", "Back"};
        if ("compose".equals(page)) return new String[]{"Options", "Send", "Back"};
        if ("dial".equals(page)) return new String[]{"Options", "Call", dial.isEmpty() ? "Back" : "Clear"};
        if ("call".equals(page)) return new String[]{"Options", "End", "Loudsp."};
        if ("calculator".equals(page)) return new String[]{"Options", "=", "Back"};
        if ("loan".equals(page)) return new String[]{"Calculate", "", "Back"};
        if ("stopwatch".equals(page)) return new String[]{"Split", stopwatchRunning ? "Stop" : "Start", "Back"};
        if ("countdown".equals(page)) return new String[]{"Options", "Start", "Back"};
        if ("music".equals(page)) return new String[]{"Options", musicPlaying ? "Pause" : "Play", "Back"};
        if ("messageRead".equals(page)) return new String[]{"Options", "Reply", "Back"};
        if ("contactDetail".equals(page)) return new String[]{"Options", "Call", "Back"};
        if ("edit".equals(page) || "alarmEdit".equals(page)) return new String[]{"Save", "", editBuffer.isEmpty() ? "Back" : "Clear"};
        if ("browser".equals(page)) return new String[]{"Options", "Go", "Back"};
        if ("itemDetail".equals(page)) return new String[]{"Options", "", "Back"};
        return new String[]{"Options", "Select", "Back"};
    }

    private void drawOptions(Canvas c) {
        p.setColor(Color.rgb(17,25,35));
        c.drawRect(0, 0, 240, 286, p);

        p.setColor(Color.BLACK);
        c.drawRect(0, 0, 240, 28, p);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(18);
        c.drawText("Options", 6, 21, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(13);
        c.drawText((optionSel + 1) + "-" + optionItems.length, 233, 20, p);

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

    private void drawNotice(Canvas c) {
        p.setColor(Color.argb(225, 20, 25, 27));
        c.drawRoundRect(new RectF(35, 123, 205, 177), 8, 8, p);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(12);
        drawWrappedText(c, notice, 48, 145, 144, 15, 2);
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
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("OK", 180, 477, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);

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
                p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                p.setTextSize(16);
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText(labels[i][0], kr.centerX(), kr.top + 26, p);
                p.setTypeface(android.graphics.Typeface.DEFAULT);
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
            case "sent": return "Sent items";
            case "drafts": return "Drafts";
            case "outbox": return "Outbox";
            case "savedmessages": return "Saved items";
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
            case "contacts": return "Contacts";
            case "contactsNames": return "Names";
            case "log": return "Log";
            case "settings": return "Settings";
            case "profiles": return "Profiles";
            case "themes": return "Themes";
            case "connectivity": return "Connectivity";
            case "bluetooth": return "Bluetooth";
            case "gallery": return "Gallery";
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
            case "web": return "Web";
            case "calllog": return "All calls";
            case "missed": return "Missed calls";
            case "received": return "Received calls";
            case "dialled": return "Dialled numbers";
            case "shortcuts": return "My shortcuts";
            case "goto": return "Go to";
            case "web": return "STORE";
            default: return "GPT c2 from reborn";
        }
    }

    private String[] itemsFor(String pg) {
        switch (pg) {
            case "messaging":
                return new String[]{"Create message","Conversations","Drafts","Outbox","Sent items","Saved items","Delivery reports","E-mail","IMs","Voice messages","Info messages","Serv. commands","Delete messages","Message settings"};
            case "inbox": {
                String[] a = new String[24];
                for (int i = 0; i < a.length; i++) a[i] = inboxName(i);
                return a;
            }
            case "conversations": {
                String[] a = new String[179];
                for (int i = 0; i < a.length; i++) {
                    if (i == 0) a[i] = "Alex Morgan";
                    else if (i == 1) a[i] = "Nokia service";
                    else if (i == 2) a[i] = "Demo contact";
                    else a[i] = "Conversation " + (i + 1);
                }
                return a;
            }
            case "sent":
                return new String[]{"Alex Morgan","Demo contact","Nokia service"};
            case "drafts":
                return drafts.isEmpty() ? new String[]{"(empty)"} : drafts.toArray(new String[0]);
            case "outbox":
                return new String[]{"(empty)"};
            case "savedmessages":
                return new String[]{"Saved message 1","Saved message 2","Saved message 3"};
            case "reports":
                return new String[]{"Alex Morgan  Delivered","Demo contact  Delivered","Nokia service  Sent"};
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
                String[] a = new String[contacts.size()];
                for (int i = 0; i < contacts.size(); i++) a[i] = contacts.get(i).name;
                return a;
            }
            case "symbolPicker":
                return new String[]{".",",","?","!","@","#","%","&","(",")","-","+","/",";",":","'"};
            case "emojiPicker":
                return new String[]{"☺","☹","♥","★","♪","✓","☀","☕"};
            case "contacts":
                return new String[]{"Names","Add new","Synchronise all","Settings","Groups","Speed dials","Service numbers","Del. all contacts"};
            case "contactsNames": {
                String[] a = new String[contacts.size()];
                for (int i = 0; i < contacts.size(); i++) a[i] = contacts.get(i).name;
                return a;
            }
            case "log":
                return new String[]{"All calls","Missed calls","Received calls","Dialled numbers","Message recipients","Call duration","Packet data counter","Packet data timer"};
            case "calllog":
                return new String[]{"Alex Morgan  13:12","Demo contact  11:47","Nokia service  09:03"};
            case "missed":
                return new String[]{"Unknown  08:42","Demo contact  Yesterday"};
            case "received":
                return new String[]{"Alex Morgan  13:12","Demo contact  Yesterday"};
            case "dialled":
                return new String[]{"Demo contact  14:03","Alex Morgan  12:51","12345  Yesterday"};
            case "settings":
                return new String[]{"Profiles","Themes","Tones","Display","Date and time","My shortcuts","Sync and backup","Connectivity"};
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
            case "goto":
                return new String[]{"Lock keypad","Profiles","Alarm clock","Camera","Video recorder","Calculator","Nokia Browser","Media player","Conversations"};
            case "gallery":
                return new String[]{"Memory card","Images","Video clips","Music files"};
            case "media":
                return new String[]{"Camera","Video camera","Media player","Radio","Voice recorder","Equaliser"};
            case "organiser":
                return new String[]{"Alarm clock","Calendar","Maps","To-do list","Notes","Calculator","Countdown timer","Stopwatch"};
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
            String[] routes = {"contacts","organiser","media","gallery","messaging","applications","log","settings","web"};
            go(routes[Math.max(0, Math.min(sel, routes.length - 1))]);
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

        if ("inbox".equals(page)) {
            inboxIndex = sel;
            go("messageRead");
            return;
        }

        if ("conversations".equals(page)) {
            detailTitle = itemsFor(page)[sel];
            detailText = "16-09-2026\n00:59  Can you test the native version?\n01:00  Yes - checking it now.\n\nConversation " + (sel + 1) + " of 179";
            go("itemDetail");
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

        if (Arrays.asList("outbox","savedmessages","reports","email","ims","voicemessages","infomessages","servicecommands","deletemessages","messagesettings").contains(page)) {
            detailTitle = itemsFor(page)[sel];
            if ("outbox".equals(page)) detailText = "No messages waiting to be sent.";
            else if ("reports".equals(page)) detailText = "Delivery information for this message.";
            else if ("email".equals(page)) detailText = "E-mail account is not configured.";
            else if ("ims".equals(page)) detailText = "Instant messaging service is offline.";
            else if ("voicemessages".equals(page)) detailText = "Voice mailbox service.";
            else if ("infomessages".equals(page)) detailText = "Network information message settings.";
            else if ("servicecommands".equals(page)) detailText = "Enter a service command for the network.";
            else if ("deletemessages".equals(page)) detailText = "Delete selected message group.";
            else if ("messagesettings".equals(page)) detailText = "Messaging configuration.";
            else detailText = "Saved message.";
            go("itemDetail");
            return;
        }

        if ("drafts".equals(page)) {
            if (!drafts.isEmpty()) {
                messageBody = drafts.get(sel);
                recipient = "";
                composeFocus = 1;
                go("compose");
            }
            return;
        }

        if ("contacts".equals(page)) {
            if (sel == 0) go("contactsNames");
            else if (sel == 1) beginContactEdit(-1);
            else {
                detailTitle = itemsFor(page)[sel];
                if (sel == 2) detailText = "Synchronisation complete.";
                else if (sel == 4) detailText = "No groups defined.";
                else if (sel == 5) detailText = "No speed dials assigned.";
                else if (sel == 6) detailText = "Service numbers from SIM.";
                else if (sel == 7) detailText = "Delete all contacts.";
                else detailText = "Contact settings.";
                go("itemDetail");
            }
            return;
        }

        if ("contactsNames".equals(page) && !contacts.isEmpty()) {
            contactIndex = sel;
            go("contactDetail");
            return;
        }

        if ("log".equals(page)) {
            String[] routes = {"calllog","missed","received","dialled","itemDetail","itemDetail","itemDetail","itemDetail"};
            if (sel < 4) go(routes[sel]);
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = "No data.";
                go("itemDetail");
            }
            return;
        }

        if (Arrays.asList("calllog","missed","received","dialled").contains(page)) {
            String item = itemsFor(page)[sel];
            int cut = item.indexOf("  ");
            dial = cut > 0 ? item.substring(0, cut) : item;
            showNotice("Ready to call");
            return;
        }

        if ("settings".equals(page)) {
            if (sel == 0) go("profiles");
            else if (sel == 1) go("themes");
            else if (sel == 5) go("shortcuts");
            else if (sel == 6) {
                detailTitle = "Sync and backup";
                detailText = "Phone switch\nCreate backup\nRestore backup\nData transfer\nSynchronise all";
                go("itemDetail");
            }
            else if (sel == 7) go("connectivity");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = "Setting available in this native recreation.";
                go("itemDetail");
            }
            return;
        }

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
            detailTitle = itemsFor(page)[sel];
            detailText = sel == 0 ? "Memory card\nFree memory: 1.8 GB\nUsed: 214 MB" : "Folder is empty.";
            go("itemDetail");
            return;
        }

        if ("media".equals(page)) {
            if (sel == 2) go("music");
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = sel == 0 ? "Native camera screen placeholder." :
                        sel == 3 ? "Radio requires a wired headset on the original C2-01." :
                                "Media function recreated as a native screen.";
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
            detailTitle = itemsFor(page)[sel];
            detailText = "Shortcut assignment is simulated natively.";
            go("itemDetail");
            return;
        }

        if ("goto".equals(page)) {
            switch (sel) {
                case 0:
                    locked = true;
                    unlockStep = 0;
                    history.clear();
                    page = "home";
                    sel = 0;
                    showNotice("Keypad locked");
                    break;
                case 1: go("profiles"); break;
                case 2: go("alarm"); break;
                case 3:
                    detailTitle = "Camera";
                    detailText = "Camera";
                    go("itemDetail");
                    break;
                case 4:
                    detailTitle = "Video recorder";
                    detailText = "Video recorder";
                    go("itemDetail");
                    break;
                case 5: go("calculator"); break;
                case 6:
                    browserUrl = "https://www.nokia.com/";
                    go("browser");
                    break;
                case 7: go("music"); break;
                case 8: go("conversations"); break;
            }
            return;
        }

        String[] items = itemsFor(page);
        if (items.length > 0) {
            detailTitle = items[sel];
            detailText = "Details";
            go("itemDetail");
        }
    }

    private void beginTextEdit(String kind, int index, String text) {
        editKind = kind;
        editIndex = index;
        editBuffer = text == null ? "" : text;
        editField = 0;
        inputMode = "abc";
        go("edit");
    }

    private void beginContactEdit(int index) {
        editKind = "contact";
        editIndex = index;
        if (index >= 0 && index < contacts.size()) {
            Contact ct = contacts.get(index);
            editBuffer = ct.name + "\n" + ct.number;
        } else editBuffer = "\n";
        editField = 0;
        inputMode = "abc";
        go("edit");
    }

    private void saveEdit() {
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
            if (editIndex >= 0 && editIndex < contacts.size()) contacts.set(editIndex, new Contact(name, num));
            else contacts.add(0, new Contact(name, num));
            saveContacts();
            backTo("contactsNames");
            showNotice("Contact saved");
            return;
        }

        if (txt.isEmpty()) {
            showNotice("Enter text");
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
            calendar.add(0, txt);
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
        optionsOpen = false;
        invalidate();
    }

    private void startCall(String who) {
        if (who == null || who.trim().isEmpty()) {
            showNotice("Enter number");
            return;
        }
        activeCall = who;
        callStarted = SystemClock.elapsedRealtime();
        loudspeaker = false;
        go("call");
    }

    private void endCall() {
        activeCall = "";
        callStarted = 0;
        history.clear();
        page = "home";
        sel = 0;
        showNotice("Call ended");
        invalidate();
    }

    private void sendMessage() {
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
        drafts.remove(messageBody);
        saveStringList("drafts", drafts);
        history.clear();
        page = "home";
        sel = 0;
        showNotice("Message sent");
        recipient = "";
        messageBody = "";
        invalidate();
    }

    private void saveDraftAndBack() {
        if (!messageBody.trim().isEmpty() && !drafts.contains(messageBody)) {
            drafts.add(0, messageBody);
            saveStringList("drafts", drafts);
        }
        back();
    }

    private void go(String target) {
        history.push(new PageState(page, sel));
        page = target;
        sel = 0;
        optionsOpen = false;
        resetTap();
        invalidate();
    }

    private void back() {
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
        invalidate();
    }

    private void saveDraftAndBackPlain() {
        if (!messageBody.trim().isEmpty() && !drafts.contains(messageBody)) {
            drafts.add(0, messageBody);
            saveStringList("drafts", drafts);
        }
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
        if (optionsOpen) {
            optionsOpen = false;
            invalidate();
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
            else if ("RSK".equals(key) || "END".equals(key)) optionsOpen = false;
            invalidate();
            return;
        }

        if ("END".equals(key)) {
            if ("call".equals(page)) endCall();
            else {
                if ("compose".equals(page)) {
                    if (!messageBody.trim().isEmpty() && !drafts.contains(messageBody)) {
                        drafts.add(0, messageBody);
                        saveStringList("drafts", drafts);
                    }
                }
                history.clear();
                page = "home";
                sel = 0;
                dial = "";
                invalidate();
            }
            return;
        }

        if ("CALL".equals(key)) {
            if ("dial".equals(page)) startCall(dial);
            else if ("contactDetail".equals(page)) startCall(contacts.get(contactIndex).number);
            else if ("home".equals(page)) go("dialled");
            else if (Arrays.asList("calllog","missed","received","dialled").contains(page)) {
                String item = itemsFor(page)[sel];
                int cut = item.indexOf("  ");
                startCall(cut > 0 ? item.substring(0, cut) : item);
            }
            return;
        }

        if ("home".equals(page)) {
            if ("OK".equals(key)) go("menu");
            else if ("LSK".equals(key)) go("goto");
            else if ("RSK".equals(key)) go("contactsNames");
            else if (isNumberKey(key) && !"#".equals(key)) {
                dial = "*".equals(key) ? "*" : key;
                go("dial");
            }
            return;
        }

        if ("menu".equals(page)) {
            if ("UP".equals(key)) sel = (sel + 6) % 9;
            else if ("DOWN".equals(key)) sel = (sel + 3) % 9;
            else if ("LEFT".equals(key)) sel = sel % 3 == 0 ? sel + 2 : sel - 1;
            else if ("RIGHT".equals(key)) sel = sel % 3 == 2 ? sel - 2 : sel + 1;
            else if ("OK".equals(key)) openSelected();
            else if ("RSK".equals(key)) {
                history.clear();
                page = "home";
                sel = 0;
            } else if ("LSK".equals(key)) openOptionsForPage();
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
            if ("OK".equals(key)) loudspeaker = !loudspeaker;
            else if ("RSK".equals(key)) loudspeaker = !loudspeaker;
            else if ("LSK".equals(key)) openOptionsForPage();
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
                recipient = inboxName(inboxIndex);
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("contactDetail".equals(page)) {
            if ("OK".equals(key)) startCall(contacts.get(contactIndex).number);
            else if ("RSK".equals(key)) back();
            else if ("LSK".equals(key)) openOptionsForPage();
            invalidate();
            return;
        }

        if ("browser".equals(page) || "itemDetail".equals(page)) {
            if ("RSK".equals(key)) back();
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

        if ("RSK".equals(key)) back();
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
            resetTap();
        } else if ("OK".equals(key) || "LSK".equals(key)) saveEdit();
        else if ("RSK".equals(key)) {
            resetTap();
            if ("contact".equals(editKind)) deleteContactEditorChar();
            else if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            else back();
        } else if ("#".equals(key)) {
            if ("abc".equals(inputMode)) inputMode = "ABC";
            else if ("ABC".equals(inputMode)) inputMode = "123";
            else inputMode = "abc";
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
        if ("ABC".equals(inputMode)) set = set.toUpperCase(Locale.UK);

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
        if (musicPlaying) {
            musicElapsed += SystemClock.elapsedRealtime() - musicStarted;
            musicPlaying = false;
        } else {
            musicStarted = SystemClock.elapsedRealtime();
            musicPlaying = true;
        }
    }

    private void changeTrack(int delta) {
        if (musicPlaying) {
            musicElapsed = 0;
            musicStarted = SystemClock.elapsedRealtime();
        } else musicElapsed = 0;
        musicTrack = (musicTrack + delta + tracks.length) % tracks.length;
    }

    private void openOptionsForPage() {
        if ("menu".equals(page)) optionItems = new String[]{"Main menu view","Organise","Help"};
        else if ("messaging".equals(page)) {
            if (sel == 1) optionItems = new String[]{"New message","Inbox view","Message log","SIM messages","Memory status"};
            else if (sel == 2) optionItems = new String[]{"New message","Inbox view","Folder details","Message log","SIM messages","Memory status"};
            else if (sel >= 3 && sel <= 5) optionItems = new String[]{"New message","Folder details","Message log","Add mailbox","IM messages","Memory status"};
            else if (sel == 7) optionItems = new String[]{"New message","New e-mail","Add mailbox","Message log","IM messages","Memory status"};
            else optionItems = new String[]{"New message","Message log","Add mailbox","IM messages","Memory status"};
        }
        else if ("drafts".equals(page) || "outbox".equals(page) || "sent".equals(page) || "savedmessages".equals(page))
            optionItems = new String[]{"New message","Inbox view","Folder details","Message log","SIM messages","Memory status"};
        else if ("inbox".equals(page) || "messageRead".equals(page))
            optionItems = new String[]{"Reply","Reply as","Delete","Call","Use detail","Forward","Edit","Move","Copy as template","Message details","Conversation view","New message"};
        else if ("conversations".equals(page))
            optionItems = new String[]{"Call","Conversation details","Delete conversation","Inbox view >","New message >","Mark >","Mark all"};
        else if ("compose".equals(page))
            optionItems = new String[]{"Send","Preview","Insert","Add recipient >","Add subject","Clear field","Insert contact detail","Insert symbol","Editing options >","Writing language >","Prediction options >","Change to multim.","Save message >","Sending options >","Exit editor"};
        else if ("contacts".equals(page))
            optionItems = new String[]{"Open","Search","Add new","Memory status"};
        else if ("contactsNames".equals(page))
            optionItems = new String[]{"Search","Call >","Send message >","Add new >","Edit >","Delete contact","Mark >"};
        else if ("contactDetail".equals(page))
            optionItems = new String[]{"Add detail >","Call","Edit","Delete","Send message >","View conversations","Add image >","Use number","Set as default","Change type >","Copy number","Send business card >","Add to group","Speed dial"};
        else if ("log".equals(page))
            optionItems = new String[]{"View","Call","Send message","Save","Delete","Clear lists","Call timers"};
        else if ("missed".equals(page) || "received".equals(page) || "dialled".equals(page) || "calllog".equals(page))
            optionItems = new String[]{"Call","Send message","Save to contacts","Delete","Clear list"};
        else if ("settings".equals(page))
            optionItems = new String[]{"Open","Search","Help"};
        else if ("profiles".equals(page))
            optionItems = new String[]{"Activate","Personalise","Timed"};
        else if ("gallery".equals(page))
            optionItems = new String[]{"Downloads","Mem. card options","Details","Type of view","Sort","Add folder","Memory status"};
        else if ("media".equals(page))
            optionItems = new String[]{"Open","View photos","View videos","Settings","Memory in use"};
        else if ("organiser".equals(page))
            optionItems = new String[]{"Open","Make a note","Week view","Go to date","Go to today","Settings","Memory status"};
        else if ("calendar".equals(page))
            optionItems = new String[]{"View","Make a note","Delete","Week view","Go to date","Go to today","Memory status","Go to To-do list"};
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
        else if ("dial".equals(page))
            optionItems = new String[]{"Call","Save","Send message","Add to contact"};
        else if ("call".equals(page))
            optionItems = new String[]{"Loudspeaker","Mute","Hold","Contacts","Main menu","End call"};
        else if ("music".equals(page))
            optionItems = new String[]{"Music library","Now playing","Shuffle","Repeat","Equaliser","Settings"};
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

        optionSel = 0;
        optionsOpen = true;
        invalidate();
    }

"Call","Use detail","Forward","Edit","Move","Copy as template","Message details","Conversation view","New message"};
        else if ("conversations".equals(page))
            optionItems = new String[]{"Call","Conversation details","Delete conversation","Inbox view >","New message >","Mark >","Mark all"};
        else if ("compose".equals(page))
            optionItems = new String[]{"Send","Preview","Insert","Add recipient >","Add subject","Clear field","Insert contact detail","Insert symbol","Editing options >","Writing language >","Prediction options >","Change to multim.","Save message >","Sending options >","Exit editor"};
        else if ("contacts".equals(page))
            optionItems = new String[]{"Open","Search","Add new","Memory status"};
        else if ("contactsNames".equals(page))
            optionItems = new String[]{"Search","Call >","Send message >","Add new >","Edit >","Delete contact","Mark >"};
        else if ("contactDetail".equals(page))
            optionItems = new String[]{"Add detail >","Call","Edit","Delete","Send message >","View conversations","Add image >","Use number","Set as default","Change type >","Copy number","Send business card >","Add to group","Speed dial"};
        else if ("log".equals(page))
            optionItems = new String[]{"View","Call","Send message","Save","Delete","Clear lists","Call timers"};
        else if ("missed".equals(page) || "received".equals(page) || "dialled".equals(page) || "calllog".equals(page))
            optionItems = new String[]{"Call","Send message","Save to contacts","Delete","Clear list"};
        else if ("settings".equals(page))
            optionItems = new String[]{"Open","Search","Help"};
        else if ("profiles".equals(page))
            optionItems = new String[]{"Activate","Personalise","Timed"};
        else if ("gallery".equals(page))
            optionItems = new String[]{"Downloads","Mem. card options","Details","Type of view","Sort","Add folder","Memory status"};
        else if ("media".equals(page))
            optionItems = new String[]{"Open","View photos","View videos","Settings","Memory in use"};
        else if ("organiser".equals(page))
            optionItems = new String[]{"Open","Make a note","Week view","Go to date","Go to today","Settings","Memory status"};
        else if ("calendar".equals(page))
            optionItems = new String[]{"View","Make a note","Delete","Week view","Go to date","Go to today","Memory status","Go to To-do list"};
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
        else if ("dial".equals(page))
            optionItems = new String[]{"Call","Save","Send message","Add to contact"};
        else if ("call".equals(page))
            optionItems = new String[]{"Loudspeaker","Mute","Hold","Contacts","Main menu","End call"};
        else if ("music".equals(page))
            optionItems = new String[]{"Music library","Now playing","Shuffle","Repeat","Equaliser","Settings"};
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

        optionSel = 0;
        optionsOpen = true;
        invalidate();
    }

    private void chooseOption() {
        String choice = optionItems.length == 0 ? "" : optionItems[optionSel];
        optionsOpen = false;

        if ("menu".equals(page)) {
            if ("Main menu view".equals(choice)) showNotice("Grid with labels");
            else if ("Organise".equals(choice)) showNotice("Organise menu");
            else showNotice("Help");
        } else if ("goto".equals(page)) {
            if ("Select".equals(choice)) openSelected();
            else if ("Organise".equals(choice)) showNotice("Organise shortcuts");
            else showNotice("Help");
        } else if ("messaging".equals(page) || "drafts".equals(page) || "outbox".equals(page) || "sent".equals(page) || "savedmessages".equals(page)) {
            if ("New message".equals(choice)) {
                recipient = ""; messageBody = ""; composeFocus = 0; inputMode = "T9"; go("compose");
            } else if ("Inbox view".equals(choice)) go("inbox");
            else if ("Message log".equals(choice)) { detailTitle="Message log"; detailText="Sent messages\nReceived messages\nDraft messages"; go("itemDetail"); }
            else if ("SIM messages".equals(choice)) { detailTitle="SIM messages"; detailText="No SIM messages"; go("itemDetail"); }
            else if ("Memory status".equals(choice)) { detailTitle="Memory status"; detailText="Phone\nMemory card"; go("itemDetail"); }
            else if ("Folder details".equals(choice)) showNotice("Folder details");
            else if ("Add mailbox".equals(choice) || "New e-mail".equals(choice)) showNotice("E-mail not configured");
            else if ("IM messages".equals(choice)) go("ims");
        } else if ("conversations".equals(page)) {
            if ("Call".equals(choice)) {
                if (sel == 0 && !contacts.isEmpty()) startCall(contacts.get(0).number);
                else showNotice("No number");
            } else if ("Conversation details".equals(choice)) {
                detailTitle="Message details";
                detailText="Type: Conversation\nContact: " + itemsFor("conversations")[sel];
                go("itemDetail");
            } else if ("Delete conversation".equals(choice)) showNotice("Conversation deleted");
            else if ("Inbox view >".equals(choice)) go("inbox");
            else if ("New message >".equals(choice)) { recipient=""; messageBody=""; composeFocus=0; go("compose"); }
            else if ("Mark >".equals(choice) || "Mark all".equals(choice)) showNotice(choice.replace(" >",""));
        } else if ("inbox".equals(page) || "messageRead".equals(page)) {
            if ("Reply".equals(choice) || "Reply as".equals(choice)) {
                recipient = inboxName(inboxIndex);
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else if ("Call".equals(choice)) startCall(inboxName(inboxIndex));
            else if ("Delete".equals(choice)) showNotice("Message deleted");
            else if ("Forward".equals(choice) || "Edit".equals(choice) || "New message".equals(choice)) {
                recipient = ""; messageBody = "Forwarded message"; composeFocus = 0; go("compose");
            } else if ("Conversation view".equals(choice)) go("conversations");
            else if ("Message details".equals(choice)) {
                detailTitle="Message details"; detailText="Type: Text message\nStatus: Read"; go("itemDetail");
            } else showNotice(choice);
        } else if ("compose".equals(page)) {
            if ("Send".equals(choice)) sendMessage();
            else if ("Add recipient >".equals(choice)) go("recipientPicker");
            else if ("Insert symbol".equals(choice) || "Insert".equals(choice)) go("symbolPicker");
            else if ("Clear field".equals(choice)) messageBody = "";
            else if ("Save message >".equals(choice)) {
                if (!messageBody.trim().isEmpty() && !drafts.contains(messageBody)) {
                    drafts.add(0, messageBody);
                    saveStringList("drafts", drafts);
                }
                showNotice("Message saved");
            } else if ("Prediction options >".equals(choice)) {
                inputMode = "T9".equals(inputMode) ? "abc" : "T9";
                showNotice("Prediction " + ("T9".equals(inputMode) ? "on" : "off"));
            } else if ("Exit editor".equals(choice)) back();
            else showNotice(choice.replace(" >",""));
        } else if ("contacts".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("Add new".equals(choice)) beginContactEdit(-1);
            else if ("Search".equals(choice)) showNotice("Search");
            else { detailTitle="Memory status"; detailText="Phone memory\nSIM card"; go("itemDetail"); }
        } else if ("contactsNames".equals(page)) {
            if (contacts.isEmpty()) return;
            contactIndex = Math.max(0, Math.min(sel, contacts.size()-1));
            Contact ct = contacts.get(contactIndex);
            if ("Call >".equals(choice)) startCall(ct.number);
            else if ("Send message >".equals(choice)) { recipient=ct.number; messageBody=""; composeFocus=1; go("compose"); }
            else if ("Add new >".equals(choice)) beginContactEdit(-1);
            else if ("Edit >".equals(choice)) beginContactEdit(contactIndex);
            else if ("Delete contact".equals(choice)) { contacts.remove(contactIndex); saveContacts(); sel=Math.max(0,sel-1); showNotice("Contact deleted"); }
            else if ("Search".equals(choice)) showNotice("Search");
            else if ("Mark >".equals(choice)) showNotice("Mark");
        } else if ("contactDetail".equals(page)) {
            Contact ct = contacts.get(Math.max(0, Math.min(contactIndex, contacts.size()-1)));
            if ("Call".equals(choice)) startCall(ct.number);
            else if ("Edit".equals(choice)) beginContactEdit(contactIndex);
            else if ("Delete".equals(choice)) { contacts.remove(contactIndex); saveContacts(); back(); showNotice("Contact deleted"); }
            else if ("Send message >".equals(choice)) { recipient=ct.number; messageBody=""; composeFocus=1; go("compose"); }
            else if ("View conversations".equals(choice)) go("conversations");
            else if ("Use number".equals(choice) || "Copy number".equals(choice)) showNotice(ct.number);
            else showNotice(choice.replace(" >",""));
        } else if ("log".equals(page)) {
            if ("View".equals(choice)) openSelected();
            else if ("Call".equals(choice)) openSelected();
            else if ("Clear lists".equals(choice)) showNotice("Call lists cleared");
            else if ("Call timers".equals(choice)) { detailTitle="Call timers"; detailText="Call duration\nPacket data counter\nPacket data timer"; go("itemDetail"); }
            else showNotice(choice);
        } else if ("missed".equals(page) || "received".equals(page) || "dialled".equals(page) || "calllog".equals(page)) {
            if ("Call".equals(choice)) {
                String item=itemsFor(page)[sel]; int cut=item.indexOf("  "); startCall(cut>0?item.substring(0,cut):item);
            } else if ("Clear list".equals(choice)) showNotice("List cleared");
            else showNotice(choice);
        } else if ("settings".equals(page) || "gallery".equals(page) || "media".equals(page) || "organiser".equals(page) || "applications".equals(page) || "web".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else showNotice(choice);
        } else if ("profiles".equals(page)) {
            if ("Activate".equals(choice)) { profile=itemsFor(page)[sel]; saveSettings(); showNotice(profile+" activated"); }
            else showNotice(choice);
        } else if ("notes".equals(page)) {
            if ("Make a note".equals(choice)) beginTextEdit("note",-1,"");
            else if ("Edit".equals(choice) && !notes.isEmpty()) beginTextEdit("note",sel,notes.get(sel));
            else if ("Delete".equals(choice) && !notes.isEmpty()) { notes.remove(sel); saveStringList("notes",notes); sel=Math.max(0,Math.min(sel,notes.size()-1)); showNotice("Deleted"); }
            else if ("Delete all notes".equals(choice)) { notes.clear(); saveStringList("notes",notes); sel=0; showNotice("Notes deleted"); }
            else showNotice(choice.replace(" >",""));
        } else if ("todo".equals(page)) {
            if ("Add".equals(choice)) beginTextEdit("todo",-1,"");
            else if ("Delete".equals(choice) && !todos.isEmpty()) { todos.remove(sel); saveStringList("todos",todos); sel=Math.max(0,Math.min(sel,todos.size()-1)); }
            else if ("Go to calendar".equals(choice)) go("calendar");
            else if ("Open".equals(choice)) openSelected();
            else showNotice(choice);
        } else if ("calendar".equals(page)) {
            if ("Make a note".equals(choice)) beginTextEdit("calendar",-1,"");
            else if ("Go to To-do list".equals(choice)) go("todo");
            else if ("View".equals(choice)) openSelected();
            else showNotice(choice);
        } else if ("dial".equals(page)) {
            if ("Call".equals(choice)) startCall(dial);
            else if ("Save".equals(choice) && !dial.isEmpty()) { editKind="contact"; editIndex=-1; editBuffer="\n"+dial; editField=0; go("edit"); }
            else if ("Send message".equals(choice)) { recipient=dial; messageBody=""; composeFocus=1; go("compose"); }
            else if ("Add to contact".equals(choice)) showNotice("Add to contact");
        } else if ("call".equals(page)) {
            if ("Loudspeaker".equals(choice)) { loudspeaker=!loudspeaker; showNotice(loudspeaker?"Loudspeaker on":"Loudspeaker off"); }
            else if ("Contacts".equals(choice)) go("contactsNames");
            else if ("Main menu".equals(choice)) go("menu");
            else if ("End call".equals(choice)) endCall();
            else showNotice(choice);
        } else if ("calculator".equals(page)) {
            if ("Scientific calculator".equals(choice)) go("scientific");
            else if ("Loan calculator".equals(choice)) go("loan");
            else if ("Exit".equals(choice)) { history.clear(); page="home"; sel=0; }
            else showNotice("Use keypad and navigation keys");
        } else if ("scientific".equals(page)) {
            if ("Standard calculator".equals(choice)) backTo("calculator");
            else if ("Loan calculator".equals(choice)) go("loan");
            else if ("Exit".equals(choice)) { history.clear(); page="home"; sel=0; }
            else showNotice("Scientific calculator");
        } else if ("loan".equals(page)) {
            if ("Calculate".equals(choice)) calculateLoan();
            else if ("Standard calculator".equals(choice)) go("calculator");
            else if ("Scientific calculator".equals(choice)) go("scientific");
            else if ("Exit".equals(choice)) { history.clear(); page="home"; sel=0; }
            else showNotice(choice);
        } else if ("music".equals(page)) {
            if ("Now playing".equals(choice)) showNotice(tracks[musicTrack]);
            else if ("Equaliser".equals(choice)) showNotice("Normal");
            else if ("Shuffle".equals(choice) || "Repeat".equals(choice)) showNotice(choice);
            else if ("Music library".equals(choice)) showNotice("Music library");
            else showNotice(choice);
        } else if ("bluetooth".equals(page)) {
            if ("Open".equals(choice)) openSelected();
            else if ("New search".equals(choice)) showNotice(bluetooth ? "No devices found" : "Switch Bluetooth on first");
            else showNotice(choice);
        } else if ("browser".equals(page)) {
            if ("Home".equals(choice)) { browserUrl="https://www.nokia.com/"; invalidate(); }
            else if ("Bookmarks".equals(choice)) { browserUrl="about:bookmarks"; invalidate(); }
            else if ("Go to address".equals(choice)) beginTextEdit("url",-1,"http://");
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

    private void showNotice(String text) {
        notice = text;
        noticeUntil = SystemClock.uptimeMillis() + 1800;
        invalidate();
    }

    private String inboxName(int i) {
        if (i == 0) return "Alex Morgan";
        if (i == 1) return "Nokia service";
        if (i == 2) return "Demo contact";
        return "Demo contact " + (i + 1);
    }

    private String inboxBody(int i) {
        if (i == 0) return "Can you test the native version?";
        if (i == 1) return "Welcome to your Nokia C2-01 recreation.";
        if (i == 2) return "This message is stored locally in the APK simulation.";
        return "Demo message " + (i + 1) + ".";
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

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) return true;
        float x = (e.getX() - offX) / fitScale;
        float y = (e.getY() - offY) / fitScale;
        String key = hitKey(x, y);
        if (key != null) press(key);
        return true;
    }

    private String hitKey(float x, float y) {
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

    private static class Contact {
        final String name;
        final String number;
        Contact(String name, String number) {
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
        }
    }
}
