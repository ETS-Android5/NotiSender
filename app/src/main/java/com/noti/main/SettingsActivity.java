package com.noti.main;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import com.noti.main.ui.options.MainPreference;
import com.noti.main.utils.BillingHelper;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    public static BillingHelper mBillingHelper;

    @SuppressLint("StaticFieldLeak")
    public static Fragment mFragment;

    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);

        if (savedInstanceState == null) {
            attachFragment(this, new MainPreference());
        } else {
            mFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if(mFragment != null) attachFragment(this, mFragment);
            else attachFragment(this, mFragment = new MainPreference());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        //Christmas Event!!!
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        int date = calendar.get(Calendar.DATE);
        if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER && date < 26 && date > 17) {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setNavigationIcon(R.drawable.hat);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBillingHelper.Destroy();
    }

    public static void attachFragment(FragmentActivity activity, Fragment fragment) {
        Bundle bundle = new Bundle(0);
        fragment.setArguments(bundle);

        SettingsActivity.mFragment = fragment;
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, SettingsActivity.mFragment)
                .commit();
    }
}