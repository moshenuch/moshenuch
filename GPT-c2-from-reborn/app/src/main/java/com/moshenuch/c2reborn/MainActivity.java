package com.moshenuch.c2reborn;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private static final int REQ_PHONE_DATA = 201;
    private static final int REQ_PICK_IMAGES = 301;
    private static final int REQ_PICK_VIDEOS = 302;
    private C2PhoneView phoneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        phoneView = new C2PhoneView(this);
        setContentView(phoneView);
        requestPhonePermissions();
    }

    private void requestPhonePermissions() {
        if (Build.VERSION.SDK_INT < 23) return;
        String[] permissions = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.READ_CALL_LOG
        };
        boolean missing = false;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missing = true;
                break;
            }
        }
        if (missing) requestPermissions(permissions, REQ_PHONE_DATA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_PHONE_DATA && phoneView != null) phoneView.onPermissionsChanged();
    }

    /** Uses Android's scoped Storage Access Framework. No broad storage permission. */
    public void requestGalleryFiles(boolean video) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(video ? "video/*" : "image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, video ? REQ_PICK_VIDEOS : REQ_PICK_IMAGES);
        } catch (android.content.ActivityNotFoundException ex) {
            if (phoneView != null) phoneView.onGalleryPickerError();
        }
    }

    /** Delegate playback to an installed Android video player. No WebView used. */
    public boolean viewGalleryVideo(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if (!"content".equals(uri.getScheme())) return false;
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "video/*");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(view);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGES && requestCode != REQ_PICK_VIDEOS) return;
        if (resultCode != RESULT_OK || data == null || phoneView == null) return;
        ArrayList<Uri> chosen = new ArrayList<>();
        ClipData clips = data.getClipData();
        if (clips != null) {
            for (int i = 0; i < clips.getItemCount() && i < 150; i++)
                chosen.add(clips.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            chosen.add(data.getData());
        }
        for (Uri uri : chosen) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
        }
        phoneView.onGalleryPicked(requestCode == REQ_PICK_VIDEOS, chosen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (phoneView != null) phoneView.onPermissionsChanged();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (phoneView == null || !phoneView.handleAndroidBack()) {
            super.onBackPressed();
        }
    }
}
