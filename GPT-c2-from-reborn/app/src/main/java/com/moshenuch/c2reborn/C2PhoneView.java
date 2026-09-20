package com.moshenuch.c2reborn;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.List;
import java.util.Locale;

public class C2PhoneView extends View {
    private static final float DW = 360f;
    private static final float DH = 780f;
    private static final float SX = 58f;
    private static final float SY = 77f;
    private static final float SW = 244f;
    private static final float SH = 325.33f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final Deque<PageState> history = new ArrayDeque<>();

    private float fitScale = 1f, offX = 0f, offY = 0f;
    private String page = "home";
    private int sel = 0;
    private String notice = "";
    private long noticeUntil = 0L;

    private String profile = "General";
    private String theme = "Nokia";
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
    private String[] optionItems = new String[0];

    private final String[] mainMenu = {
            "Messaging", "Contacts", "Log",
            "Settings", "Gallery", "Media",
            "Organiser", "Applications", "Web"
    };

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
        setBackgroundColor(Color.rgb(9, 9, 11));
        setFocusable(true);
        setFocusableInTouchMode(true);
        prefs = context.getSharedPreferences("c2_state", Context.MODE_PRIVATE);
        loadState();
    }

    private void loadState() {
        profile = prefs.getString("profile", "General");
        theme = prefs.getString("theme", "Nokia");
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
        p.setShader(new LinearGradient(0, 0, 0, DH, Color.rgb(45,46,50), Color.rgb(11,11,13), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(30, 8, 330, 772), 42, 42, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(Color.rgb(94, 95, 100));
        c.drawRoundRect(new RectF(31, 9, 329, 771), 41, 41, p);
        p.setStyle(Paint.Style.FILL);

        p.setColor(Color.rgb(18,18,20));
        c.drawRoundRect(new RectF(48, 60, 312, 411), 12, 12, p);
        p.setColor(Color.rgb(190,190,194));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(18);
        c.drawText("NOKIA", 180, 39, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setColor(Color.rgb(82,82,86));
        c.drawRoundRect(new RectF(150, 47, 210, 52), 3, 3, p);

        c.save();
        c.clipRect(SX, SY, SX + SW, SY + SH);
        c.translate(SX, SY);
        float s = SW / 240f;
        c.scale(s, s);
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
        if ("Dark".equals(theme)) return Color.rgb(39, 45, 49);
        if ("Silver".equals(theme)) return Color.rgb(226, 230, 231);
        if ("Blue".equals(theme)) return Color.rgb(222, 234, 246);
        return Color.rgb(238, 241, 237);
    }

    private int themeText() {
        return "Dark".equals(theme) ? Color.WHITE : Color.rgb(20, 25, 27);
    }

    private int accent() {
        if ("Silver".equals(theme)) return Color.rgb(76, 87, 93);
        if ("Dark".equals(theme)) return Color.rgb(65, 123, 148);
        if ("Blue".equals(theme)) return Color.rgb(45, 102, 153);
        return Color.rgb(40, 118, 145);
    }

    private void drawStatus(Canvas c, String title) {
        p.setColor("Dark".equals(theme) ? Color.rgb(25,32,35) : Color.rgb(213, 224, 221));
        c.drawRect(0, 0, 240, 18, p);

        p.setColor(themeText());
        p.setStrokeWidth(1.5f);
        for (int i = 0; i < 4; i++) {
            float h = 3 + i * 2.5f;
            c.drawRect(5 + i * 4, 14 - h, 7 + i * 4, 14, p);
        }

        p.setStyle(Paint.Style.STROKE);
        c.drawRect(216, 5, 231, 13, p);
        c.drawRect(232, 8, 234, 10, p);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(218, 7, 228, 11, p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(11);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(title, 120, 13, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    private void drawHome(Canvas c) {
        p.setShader(new LinearGradient(0, 0, 240, 300,
                Color.rgb(40, 91, 149), Color.rgb(103, 49, 137), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 240, 296, p);
        p.setShader(null);

        p.setColor(Color.argb(65, 255,255,255));
        c.drawCircle(196, 86, 58, p);
        c.drawCircle(34, 218, 76, p);
        p.setColor(Color.argb(38, 255,255,255));
        c.drawCircle(125, 166, 95, p);

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
        drawStatus(c, "Menu");
        String[] icons = {"✉","☏","↗","⚙","▧","♪","◷","◆","◎"};
        for (int i = 0; i < mainMenu.length; i++) {
            int col = i % 3, row = i / 3;
            float x = 7 + col * 78;
            float y = 28 + row * 83;
            RectF r = new RectF(x, y, x + 70, y + 72);
            if (i == sel) {
                p.setColor(accent());
                c.drawRoundRect(r, 6, 6, p);
            }
            p.setColor(i == sel ? Color.WHITE : themeText());
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(28);
            c.drawText(icons[i], x + 35, y + 34, p);
            p.setTextSize(10);
            p.setTypeface(i == sel ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            c.drawText(mainMenu[i], x + 35, y + 58, p);
            p.setTypeface(android.graphics.Typeface.DEFAULT);
        }
    }

    private void drawListPage(Canvas c) {
        String title = titleFor(page);
        drawStatus(c, title);
        String[] items = itemsFor(page);

        if (items.length == 0) {
            p.setColor(themeText());
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(14);
            c.drawText("(empty)", 120, 120, p);
            return;
        }

        int start = Math.max(0, Math.min(sel - 2, Math.max(0, items.length - 6)));
        for (int row = 0; row < 6 && start + row < items.length; row++) {
            int idx = start + row;
            float y = 22 + row * 43;
            if (idx == sel) {
                p.setColor(accent());
                c.drawRect(3, y, 235, y + 39, p);
            } else {
                p.setColor("Dark".equals(theme) ? Color.rgb(45,52,55) : Color.rgb(246,248,247));
                c.drawRect(3, y, 235, y + 39, p);
            }
            p.setColor(idx == sel ? Color.WHITE : themeText());
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(13);
            p.setTypeface(idx == sel ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            c.drawText(items[idx], 12, y + 24, p);
            p.setTypeface(android.graphics.Typeface.DEFAULT);

            String sub = subtitleFor(page, idx);
            if (!sub.isEmpty()) {
                p.setTextAlign(Paint.Align.RIGHT);
                p.setTextSize(10);
                c.drawText(sub, 226, y + 24, p);
            }
        }

        if (items.length > 6) {
            p.setColor(Color.rgb(180,185,185));
            c.drawRect(236, 22, 239, 280, p);
            float h = Math.max(20, 258f * 6f / items.length);
            float y = 22 + (258f - h) * sel / Math.max(1, items.length - 1);
            p.setColor(accent());
            c.drawRect(236, y, 239, y + h, p);
        }
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
        p.setColor("Dark".equals(theme) ? Color.rgb(18,24,27) : Color.rgb(208,218,216));
        c.drawRect(0, 296, 240, 320, p);
        String[] soft = softLabels();
        p.setColor("Dark".equals(theme) ? Color.WHITE : Color.rgb(12, 27, 31));
        p.setTextSize(11);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(soft[0], 6, 313, p);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(soft[1], 120, 313, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(soft[2], 234, 313, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    private String[] softLabels() {
        if ("home".equals(page)) return new String[]{"Go to", "Menu", "Names"};
        if ("compose".equals(page)) return new String[]{"Options", "Send", "Back"};
        if ("dial".equals(page)) return new String[]{"Options", "Call", dial.isEmpty() ? "Back" : "Clear"};
        if ("call".equals(page)) return new String[]{"Options", "End", "Loudsp."};
        if ("calculator".equals(page)) return new String[]{"Options", "=", "Back"};
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
        float w = 170;
        float h = Math.min(230, 24 + optionItems.length * 31);
        float x = 5;
        float y = 292 - h;
        p.setColor(Color.rgb(248, 249, 247));
        c.drawRect(x, y, x + w, y + h, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(Color.rgb(45,50,52));
        c.drawRect(x, y, x + w, y + h, p);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < optionItems.length; i++) {
            float ry = y + 4 + i * 31;
            if (i == optionSel) {
                p.setColor(accent());
                c.drawRect(x + 2, ry, x + w - 2, ry + 29, p);
            }
            p.setColor(i == optionSel ? Color.WHITE : Color.rgb(20,23,24));
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(12);
            c.drawText(optionItems[i], x + 9, ry + 20, p);
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
        drawSoftKey(c, new RectF(55, 422, 123, 458), true);
        drawSoftKey(c, new RectF(237, 422, 305, 458), false);

        p.setColor(Color.rgb(43,44,47));
        c.drawOval(new RectF(139, 444, 221, 526), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(Color.rgb(118,119,123));
        c.drawOval(new RectF(142, 447, 218, 523), p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(28,29,31));
        c.drawCircle(180, 485, 18, p);
        p.setColor(Color.rgb(205,205,208));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(12);
        c.drawText("OK", 180, 489, p);

        p.setColor(Color.rgb(41,42,45));
        c.drawRoundRect(new RectF(50, 466, 122, 508), 14,14,p);
        c.drawRoundRect(new RectF(238, 466, 310, 508), 14,14,p);
        p.setColor(Color.rgb(66,170,85));
        p.setTextSize(21);
        c.drawText("☏", 86, 493, p);
        p.setColor(Color.rgb(205,62,62));
        c.drawText("☏", 274, 493, p);

        String[][] labels = {
                {"1","◉◉"},{"2","abc"},{"3","def"},
                {"4","ghi"},{"5","jkl"},{"6","mno"},
                {"7","pqrs"},{"8","tuv"},{"9","wxyz"},
                {"*","+"},{"0","↻"},{"#","⌂"}
        };
        float[] xs = {58, 149, 240};
        float[] ys = {538, 590, 642, 694};
        for (int r = 0; r < 4; r++) {
            for (int col = 0; col < 3; col++) {
                int i = r * 3 + col;
                RectF kr = new RectF(xs[col], ys[r], xs[col] + 62, ys[r] + 40);
                p.setShader(new LinearGradient(0, kr.top, 0, kr.bottom,
                        Color.rgb(68,69,73), Color.rgb(28,29,32), Shader.TileMode.CLAMP));
                c.drawRoundRect(kr, 9,9,p);
                p.setShader(null);
                p.setColor(Color.rgb(226,226,229));
                p.setTextAlign(Paint.Align.CENTER);
                p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                p.setTextSize(16);
                c.drawText(labels[i][0], kr.centerX(), kr.top + 18, p);
                p.setTypeface(android.graphics.Typeface.DEFAULT);
                p.setTextSize(8);
                c.drawText(labels[i][1], kr.centerX(), kr.top + 31, p);
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
            default: return "GPT c2 from reborn";
        }
    }

    private String[] itemsFor(String pg) {
        switch (pg) {
            case "messaging":
                return new String[]{"Create message","Inbox","Conversations","Sent items","Drafts","E-mail","Chat","Message settings"};
            case "inbox": {
                String[] a = new String[24];
                for (int i = 0; i < a.length; i++) a[i] = inboxName(i);
                return a;
            }
            case "conversations":
                return new String[]{"Alex Morgan","Nokia service","Demo contact","Demo contact 4","Demo contact 5","Demo contact 6"};
            case "sent":
                return new String[]{"Alex Morgan","Demo contact","Nokia service"};
            case "drafts":
                return drafts.isEmpty() ? new String[]{"(empty)"} : drafts.toArray(new String[0]);
            case "contacts":
                return new String[]{"Names","Add new contact","Groups","Speed dials","Service numbers","My numbers","Contact settings"};
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
                return new String[]{"Profiles","Themes","Tones","Display","Date and time","My shortcuts","Connectivity","Call","Phone","Accessories","Configuration","Security","Restore factory set."};
            case "profiles":
                return new String[]{"General","Silent","Meeting","Outdoor","My style 1","My style 2"};
            case "themes":
                return new String[]{"Nokia","Blue","Silver","Dark"};
            case "connectivity":
                return new String[]{"Bluetooth","Packet data","USB data cable","Sync and backup"};
            case "bluetooth":
                return new String[]{"Bluetooth","Connect audio acc.","Paired devices","Active devices","My phone's visibility","My phone's name"};
            case "shortcuts":
                return new String[]{"Left selection key","Right selection key","Navigation key","Home screen key"};
            case "gallery":
                return new String[]{"Photos","Video clips","Music files","Graphics","Tones","Recordings","Received files","Memory card"};
            case "media":
                return new String[]{"Camera","Video","Media player","Radio","Voice recorder","Equaliser","Stereo widening"};
            case "organiser":
                return new String[]{"Alarm clock","Calendar","To-do list","Notes","Calculator","Countdown timer","Stopwatch","Loan calculator"};
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
            String[] routes = {"messaging","contacts","log","settings","gallery","media","organiser","applications","web"};
            go(routes[Math.max(0, Math.min(sel, routes.length - 1))]);
            return;
        }

        if ("messaging".equals(page)) {
            String[] routes = {"compose","inbox","conversations","sent","drafts","itemDetail","itemDetail","itemDetail"};
            if (sel == 0) {
                recipient = "";
                messageBody = "";
                composeFocus = 0;
                inputMode = "T9";
                go("compose");
            } else if (sel <= 4) go(routes[sel]);
            else {
                detailTitle = itemsFor(page)[sel];
                detailText = sel == 5 ? "E-mail is not configured." :
                        sel == 6 ? "Chat service is not configured." :
                                "Text, multimedia and service-message settings.";
                go("itemDetail");
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
            detailText = "16-09-2026\n00:59  Can you test the native version?\n01:00  Yes - checking it now.";
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
                detailText = "Native contact feature placeholder.";
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
            else if (sel == 6) go("connectivity");
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
            detailText = sel == 7 ? "Memory card\nFree memory: 1.8 GB" : "Folder is empty.";
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
            else if (sel == 2) go("todo");
            else if (sel == 3) go("notes");
            else if (sel == 4) go("calculator");
            else if (sel == 5) go("countdown");
            else if (sel == 6) go("stopwatch");
            else {
                detailTitle = "Loan calculator";
                detailText = "Loan calculator is included as a native organiser screen. Further finance fields can be added without any web layer.";
                go("itemDetail");
            }
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
            else if ("LSK".equals(key)) {
                page = "shortcuts";
                history.push(new PageState("home",0));
                sel = 0;
                invalidate();
            } else if ("RSK".equals(key)) go("contactsNames");
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
            messageBody += ".";
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
        if ("compose".equals(page)) optionItems = new String[]{"Send","Save as draft","Input options","Clear text"};
        else if ("contactDetail".equals(page)) optionItems = new String[]{"Call","Edit contact","Delete contact"};
        else if ("notes".equals(page)) optionItems = new String[]{"New note","Edit","Delete"};
        else if ("todo".equals(page)) optionItems = new String[]{"Add","Edit","Delete"};
        else if ("messageRead".equals(page)) optionItems = new String[]{"Reply","Delete","Message details"};
        else if ("calculator".equals(page)) optionItems = new String[]{"Clear","Scientific info"};
        else if ("stopwatch".equals(page)) optionItems = new String[]{"Start/Stop","Split time","Reset"};
        else if ("music".equals(page)) optionItems = new String[]{"Play/Pause","Next track","Previous track"};
        else if ("dial".equals(page)) optionItems = new String[]{"Call","Save number","Clear"};
        else if ("call".equals(page)) optionItems = new String[]{"End call","Loudspeaker"};
        else optionItems = new String[]{"Open","Details","Help"};
        optionSel = 0;
        optionsOpen = true;
        invalidate();
    }

    private void chooseOption() {
        String choice = optionItems.length == 0 ? "" : optionItems[optionSel];
        optionsOpen = false;

        if ("compose".equals(page)) {
            if ("Send".equals(choice)) sendMessage();
            else if ("Save as draft".equals(choice)) {
                if (!messageBody.trim().isEmpty() && !drafts.contains(messageBody)) {
                    drafts.add(0, messageBody);
                    saveStringList("drafts", drafts);
                    showNotice("Saved to Drafts");
                }
            } else if ("Input options".equals(choice)) {
                if ("T9".equals(inputMode)) inputMode = "abc"; else inputMode = "T9";
                showNotice("Input " + inputMode);
            } else if ("Clear text".equals(choice)) messageBody = "";
        } else if ("contactDetail".equals(page)) {
            if ("Call".equals(choice)) startCall(contacts.get(contactIndex).number);
            else if ("Edit contact".equals(choice)) beginContactEdit(contactIndex);
            else if ("Delete contact".equals(choice)) {
                contacts.remove(contactIndex);
                saveContacts();
                back();
                showNotice("Contact deleted");
            }
        } else if ("notes".equals(page)) {
            if ("New note".equals(choice)) beginTextEdit("note",-1,"");
            else if ("Edit".equals(choice) && !notes.isEmpty()) beginTextEdit("note",sel,notes.get(sel));
            else if ("Delete".equals(choice) && !notes.isEmpty()) {
                notes.remove(sel);
                saveStringList("notes",notes);
                sel = Math.max(0, Math.min(sel, notes.size() - 1));
                showNotice("Deleted");
            }
        } else if ("todo".equals(page)) {
            if ("Add".equals(choice)) beginTextEdit("todo",-1,"");
            else if ("Edit".equals(choice) && !todos.isEmpty()) beginTextEdit("todo",sel,todos.get(sel));
            else if ("Delete".equals(choice) && !todos.isEmpty()) {
                todos.remove(sel);
                saveStringList("todos",todos);
                sel = Math.max(0, Math.min(sel, todos.size() - 1));
                showNotice("Deleted");
            }
        } else if ("messageRead".equals(page)) {
            if ("Reply".equals(choice)) {
                recipient = inboxName(inboxIndex);
                messageBody = "";
                composeFocus = 1;
                go("compose");
            } else showNotice(choice);
        } else if ("calculator".equals(page)) {
            if ("Clear".equals(choice)) {
                calc = "0";
                calcStored = null;
                calcOp = 0;
            } else showNotice("D-pad: × − + ÷, OK =");
        } else if ("stopwatch".equals(page)) {
            if ("Start/Stop".equals(choice)) toggleStopwatch();
            else if ("Split time".equals(choice)) {
                long ms = stopwatchAccum + (stopwatchRunning ? SystemClock.elapsedRealtime() - stopwatchStarted : 0);
                stopwatchSplits.add(formatMillis(ms));
            } else {
                stopwatchRunning = false;
                stopwatchAccum = 0;
                stopwatchSplits.clear();
            }
        } else if ("music".equals(page)) {
            if ("Play/Pause".equals(choice)) toggleMusic();
            else if ("Next track".equals(choice)) changeTrack(1);
            else changeTrack(-1);
        } else if ("dial".equals(page)) {
            if ("Call".equals(choice)) startCall(dial);
            else if ("Save number".equals(choice) && !dial.isEmpty()) {
                editKind = "contact";
                editIndex = -1;
                editBuffer = "\n" + dial;
                editField = 0;
                go("edit");
            } else if ("Clear".equals(choice)) dial = "";
        } else if ("call".equals(page)) {
            if ("End call".equals(choice)) endCall();
            else loudspeaker = !loudspeaker;
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
        if (new RectF(50, 416, 130, 462).contains(x,y)) return "LSK";
        if (new RectF(230, 416, 310, 462).contains(x,y)) return "RSK";
        if (new RectF(45, 462, 128, 513).contains(x,y)) return "CALL";
        if (new RectF(232, 462, 315, 513).contains(x,y)) return "END";

        float dx = x - 180, dy = y - 485;
        float d = (float)Math.sqrt(dx*dx + dy*dy);
        if (d <= 42) {
            if (d <= 19) return "OK";
            if (Math.abs(dx) > Math.abs(dy)) return dx < 0 ? "LEFT" : "RIGHT";
            return dy < 0 ? "UP" : "DOWN";
        }

        float[] xs = {58,149,240};
        float[] ys = {538,590,642,694};
        String[] ks = {"1","2","3","4","5","6","7","8","9","*","0","#"};
        for (int r = 0; r < 4; r++) {
            for (int col = 0; col < 3; col++) {
                if (new RectF(xs[col], ys[r], xs[col] + 62, ys[r] + 40).contains(x,y))
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
