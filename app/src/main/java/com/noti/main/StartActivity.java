package com.noti.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.installations.FirebaseInstallations;
import com.noti.main.ui.SettingsActivity;
import com.noti.main.utils.ui.ToastHelper;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StartActivity extends AppCompatActivity {

    SharedPreferences prefs;
    ExtendedFloatingActionButton Start_App;
    MaterialButton Permit_Notification;
    MaterialButton Permit_Overlay;
    MaterialButton Permit_Battery;
    MaterialButton Permit_File;
    MaterialButton Permit_Alarm;

    ActivityResultLauncher<Intent> startOverlayPermit = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if(Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) {
            setButtonCompleted(this, Permit_Overlay);
            checkPermissionsAndEnableComplete();
        }
    });

    ActivityResultLauncher<Intent> startBatteryOptimizations = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT < 23 || pm.isIgnoringBatteryOptimizations(getPackageName())) {
            setButtonCompleted(this, Permit_Battery);
            checkPermissionsAndEnableComplete();
        }
    });

    ActivityResultLauncher<Intent> startAlarmAccessPermit = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Set<String> sets = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (sets.contains(getPackageName())) {
            setButtonCompleted(this, Permit_Alarm);
            checkPermissionsAndEnableComplete();
        }
    });

    @SuppressLint({"BatteryLife", "HardwareIds"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);

        prefs = getSharedPreferences("com.noti.main_preferences", MODE_PRIVATE);
        Permit_Notification = findViewById(R.id.Permit_Notification);
        Permit_Overlay = findViewById(R.id.Permit_Overlay);
        Permit_Battery = findViewById(R.id.Permit_Battery);
        Permit_File = findViewById(R.id.Permit_File);
        Permit_Alarm = findViewById(R.id.Permit_Alarm);
        Start_App = findViewById(R.id.Start_App);

        int count = 0;

        if(Build.VERSION.SDK_INT < 31 || ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled()) {
            setButtonCompleted(this, Permit_Notification);
            count++;
        }

        if(Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)) {
            setButtonCompleted(this, Permit_Overlay);
            count++;
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT < 23 || pm.isIgnoringBatteryOptimizations(getPackageName())) {
            setButtonCompleted(this, Permit_Battery);
            count++;
        }

        if(Build.VERSION.SDK_INT > 28 || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            setButtonCompleted(this, Permit_File);
            count++;
        }

        Set<String> sets = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (sets.contains(getPackageName())) {
            setButtonCompleted(this, Permit_Alarm);
            count++;
        }

        if(prefs.getString("FirebaseIIDPrefix", "").isEmpty()) {
            FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    prefs.edit().putString("FirebaseIIDPrefix", task.getResult()).apply();
            });
        }

        if(prefs.getString("AndroidIDPrefix", "").isEmpty()) {
            prefs.edit().putString("AndroidIDPrefix", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)).apply();
        }

        if(prefs.getString("GUIDPrefix", "").isEmpty()) {
            prefs.edit().putString("GUIDPrefix", UUID.randomUUID().toString()).apply();
        }

        if(prefs.getString("MacIDPrefix", "").isEmpty()) {
            String interfaceName = "wlan0";
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                    byte[] mac = intf.getHardwareAddress();
                    if (mac == null) {
                        prefs.edit().putString("MacIDPrefix","unknown").apply();
                        break;
                    }
                    StringBuilder buf = new StringBuilder();
                    for (byte b : mac) buf.append(String.format("%02X:", b));
                    if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                    prefs.edit().putString("MacIDPrefix",buf.toString()).apply();
                    break;
                }
            } catch (Exception e) {
                prefs.edit().putString("MacIDPrefix","unknown").apply();
            }
        }

        if(count >= 5) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }

        Permit_Notification.setOnClickListener((v) -> ActivityCompat.requestPermissions(this, new String[] { "android.permission.POST_NOTIFICATIONS" }, 100));
        Permit_Overlay.setOnClickListener((v) -> {
            if (Build.VERSION.SDK_INT > 22) startOverlayPermit.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
        Permit_Battery.setOnClickListener((v) -> {
            if (Build.VERSION.SDK_INT > 22) startBatteryOptimizations.launch(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName())));
        });
        Permit_Alarm.setOnClickListener((v) -> startAlarmAccessPermit.launch(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        Permit_File.setOnClickListener((v) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101));
        Start_App.setOnClickListener((v) -> {
            if(Permit_Notification.isEnabled() || Permit_Battery.isEnabled() || Permit_File.isEnabled() || Permit_Overlay.isEnabled() || Permit_Alarm.isEnabled()) {
                ToastHelper.show(this, "Please complete all section!", ToastHelper.LENGTH_SHORT);
            } else {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            }
        });
    }

    void setButtonCompleted(Context context, MaterialButton button) {
        button.setEnabled(false);
        button.setText(context.getString(R.string.Start_Activity_Completed));
        button.setIcon(AppCompatResources.getDrawable(context, com.microsoft.fluent.mobile.icons.R.drawable.ic_fluent_checkmark_24_regular));
    }

    void checkPermissionsAndEnableComplete() {
        if(!Permit_Notification.isEnabled() && !Permit_Battery.isEnabled() && !Permit_File.isEnabled() && !Permit_Overlay.isEnabled() && !Permit_Alarm.isEnabled()) {
            Start_App.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int foo : grantResults) {
            if (foo == PackageManager.PERMISSION_GRANTED) {
                if(requestCode == 100) setButtonCompleted(this, Permit_Notification);
                else if(requestCode == 101) setButtonCompleted(this, Permit_File);
                checkPermissionsAndEnableComplete();
            }
        }
    }
}