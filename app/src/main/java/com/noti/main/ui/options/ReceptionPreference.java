package com.noti.main.ui.options;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.kieronquinn.monetcompat.core.MonetCompat;
import com.noti.main.R;

public class ReceptionPreference extends PreferenceFragmentCompat {

    final int RC_OPEN_AUDIO = 104;
    SharedPreferences prefs;
    MonetCompat monet;
    Activity mContext;

    Preference SetImportance;
    Preference ImportanceWarning;
    Preference CustomRingtone;
    Preference ResetCustomRingtone;
    Preference RingtoneRunningTime;
    Preference VibrationRunningTime;

    Preference ReceiveDeadline;
    Preference ReceiveCustomDeadline;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MonetCompat.setup(requireContext());
        monet = MonetCompat.getInstance();
        monet.updateMonetColors();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        monet = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) mContext = (Activity) context;
        else throw new RuntimeException("Can't get Activity instanceof Context!");
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.reception_preferences, rootKey);
        prefs = mContext.getSharedPreferences("com.noti.main_preferences", MODE_PRIVATE);

        SetImportance = findPreference("importance");
        ImportanceWarning = findPreference("ImportanceWarning");
        CustomRingtone = findPreference("CustomRingtone");
        ResetCustomRingtone = findPreference("ResetCustomRingtone");
        RingtoneRunningTime = findPreference("RingtoneRunningTime");
        VibrationRunningTime = findPreference("VibrationRunningTime");
        ReceiveDeadline = findPreference("ReceiveDeadline");
        ReceiveCustomDeadline = findPreference("ReceiveCustomDeadline");

        boolean isCustomRingtoneEnabled = prefs.getString("importance","Default").equals("Custom…");
        if (Build.VERSION.SDK_INT >= 26) {
            ImportanceWarning.setVisible(false);
            CustomRingtone.setVisible(isCustomRingtoneEnabled);
            RingtoneRunningTime.setVisible(isCustomRingtoneEnabled);
            VibrationRunningTime.setVisible(isCustomRingtoneEnabled);
            ResetCustomRingtone.setVisible(isCustomRingtoneEnabled && !prefs.getString("CustomRingtone","").isEmpty());
            SetImportance.setSummary("Now : " + prefs.getString("importance", ""));
            SetImportance.setOnPreferenceChangeListener(((p, n) -> {
                SetImportance.setSummary("Now : " + n);

                boolean foo = n.equals("Custom…");
                CustomRingtone.setVisible(foo);
                RingtoneRunningTime.setVisible(foo);
                VibrationRunningTime.setVisible(foo);
                ResetCustomRingtone.setVisible(foo && !prefs.getString("CustomRingtone","").isEmpty());
                return true;
            }));
        } else {
            SetImportance.setEnabled(false);
            SetImportance.setSummary("Now : Custom…");
            ResetCustomRingtone.setVisible(!prefs.getString("CustomRingtone","").isEmpty());
        }

        String RingtoneRunningTimeValue = prefs.getString("RingtoneRunningTime", "3 sec");
        RingtoneRunningTime.setSummary("Now : " + RingtoneRunningTimeValue + (RingtoneRunningTimeValue.equals("3 sec") ? " (Default)" : ""));

        int VibrationRunningTimeValue = prefs.getInt("VibrationRunningTime", 1000);
        VibrationRunningTime.setSummary("Now : " + VibrationRunningTimeValue + (VibrationRunningTimeValue == 1000 ? " ms (Default)" : " ms"));

        if(ImportanceWarning.isEnabled() || isCustomRingtoneEnabled) {
            String s = prefs.getString("CustomRingtone","");
            DocumentFile AudioMedia = DocumentFile.fromSingleUri(mContext, Uri.parse(s));
            if(AudioMedia != null && AudioMedia.exists()) {
                CustomRingtone.setSummary("Now : " + AudioMedia.getName());
            }
        }

        String DeadlineValue = prefs.getString("ReceiveDeadline", "No deadline");
        String CustomDeadlineValue = prefs.getString("DeadlineCustomValue", "5 min");
        ReceiveCustomDeadline.setVisible(DeadlineValue.equals("Custom…"));
        ReceiveCustomDeadline.setSummary("Now : " + CustomDeadlineValue + (CustomDeadlineValue.equals("5 min") ? " (Default)" : ""));
        ReceiveDeadline.setSummary("Now : " + DeadlineValue + (DeadlineValue.equals("No deadline") ? " (Default)" : ""));
        ReceiveDeadline.setOnPreferenceChangeListener((p, n) -> {
            ReceiveDeadline.setSummary("Now : " + n + (n.equals("No deadline") ? " (Default)" : ""));
            ReceiveCustomDeadline.setVisible(n.equals("Custom…"));
            return true;
        });
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        MaterialAlertDialogBuilder dialog;
        EditText editText;
        LinearLayout parentLayout;
        LinearLayout.LayoutParams layoutParams;

        switch (preference.getKey()) {
            case "CustomRingtone":
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                startActivityForResult(intent, RC_OPEN_AUDIO);
                break;

            case "RingtoneRunningTime":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setCancelable(false);
                dialog.setTitle("Input value");
                dialog.setMessage("input receive deadline value. max is 65535.");

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_receive_deadline, null);
                TextInputEditText dialogEditValue = dialogView.findViewById(R.id.editValue);
                MaterialAutoCompleteTextView dialogUnitSpinner = dialogView.findViewById(R.id.unitSpinner);

                String[] dialogRawValues = prefs.getString("RingtoneRunningTime", "3 sec").split(" ");
                dialogEditValue.setText(dialogRawValues[0]);
                dialogUnitSpinner.setText(dialogRawValues[1]);
                dialogUnitSpinner.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.deadline_units)));

                dialog.setPositiveButton("Apply", (d, w) -> {
                    String value = dialogEditValue.getText().toString();
                    if (value.equals("")) {
                        Toast.makeText(mContext, "Please Input Value", Toast.LENGTH_SHORT).show();
                    } else {
                        int IntValue = Integer.parseInt(value);
                        if (IntValue > 65535) {
                            Toast.makeText(mContext, "Value must be lower than 65535", Toast.LENGTH_SHORT).show();
                        } else {
                            final String finalValue = value + " " + dialogUnitSpinner.getText().toString();
                            prefs.edit().putString("RingtoneRunningTime", finalValue).apply();
                            RingtoneRunningTime.setSummary("Now : " + finalValue + (finalValue.equals("3 sec") ? " (Default)" : ""));
                        }
                    }
                });

                dialog.setNeutralButton("Reset Default", (d, w) -> {
                    prefs.edit().putString("RingtoneRunningTime", "3 sec").apply();
                    RingtoneRunningTime.setSummary("Now : 3 sec (Default)");
                });
                dialog.setNegativeButton("Cancel", (d, w) -> {
                });
                dialog.setView(dialogView);
                dialog.show();
                break;

            case "VibrationRunningTime":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setCancelable(false);
                dialog.setTitle("Input Vibration playing time (ms)");
                dialog.setMessage("The playing time maximum limit is 65535 ms.");

                editText = new EditText(mContext);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setHint("Input Limit Value");
                editText.setGravity(Gravity.CENTER);
                editText.setText(String.valueOf(prefs.getInt("VibrationRunningTime", 1000)));

                parentLayout = new LinearLayout(mContext);
                layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(30, 16, 30, 16);
                editText.setLayoutParams(layoutParams);
                parentLayout.addView(editText);
                dialog.setView(parentLayout);

                dialog.setPositiveButton("Apply", (d, w) -> {
                    String value = editText.getText().toString();
                    if (value.equals("")) {
                        Toast.makeText(mContext, "Please Input Value", Toast.LENGTH_SHORT).show();
                    } else {
                        int IntValue = Integer.parseInt(value);
                        if (IntValue > 65535) {
                            Toast.makeText(mContext, "Value must be lower than 65535", Toast.LENGTH_SHORT).show();
                        } else {
                            prefs.edit().putInt("VibrationRunningTime", IntValue).apply();
                            VibrationRunningTime.setSummary("Now : " + IntValue + (IntValue == 1000 ? " ms (Default)" : " ms"));
                        }
                    }
                });
                dialog.setNeutralButton("Reset Default", (d, w) -> {
                    prefs.edit().putInt("VibrationRunningTime", 1000).apply();
                    VibrationRunningTime.setSummary("Now : " + 1000 + " ms (Default)");
                });
                dialog.setNegativeButton("Cancel", (d, w) -> {
                });
                dialog.show();
                break;

            case "ResetCustomRingtone":
                CustomRingtone.setSummary("Now : system default");
                mContext.getContentResolver()
                        .releasePersistableUriPermission(Uri.parse(prefs.getString("CustomRingtone","")),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                prefs.edit().remove("CustomRingtone").apply();
                Toast.makeText(mContext, "Selection reset done!", Toast.LENGTH_SHORT).show();
                ResetCustomRingtone.setVisible(false);
                break;

            case "ReceiveCustomDeadline":
                dialog = new MaterialAlertDialogBuilder(new ContextThemeWrapper(mContext, R.style.MaterialAlertDialog_Material3));
                dialog.setCancelable(false);
                dialog.setTitle("Input value");
                dialog.setMessage("input receive deadline value. max is 65535.");

                View view = getLayoutInflater().inflate(R.layout.dialog_receive_deadline, null);
                TextInputEditText editValue = view.findViewById(R.id.editValue);
                MaterialAutoCompleteTextView unitSpinner = view.findViewById(R.id.unitSpinner);

                String[] rawValues = prefs.getString("DeadlineCustomValue", "5 min").split(" ");
                editValue.setText(rawValues[0]);
                unitSpinner.setText(rawValues[1]);
                unitSpinner.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.deadline_units)));

                dialog.setPositiveButton("Apply", (d, w) -> {
                    String value = editValue.getText().toString();
                    if (value.equals("")) {
                        Toast.makeText(mContext, "Please Input Value", Toast.LENGTH_SHORT).show();
                    } else {
                        int IntValue = Integer.parseInt(value);
                        if (IntValue > 65535) {
                            Toast.makeText(mContext, "Value must be lower than 65535", Toast.LENGTH_SHORT).show();
                        } else {
                            final String finalValue = value + " " + unitSpinner.getText().toString();
                            prefs.edit().putString("DeadlineCustomValue", finalValue).apply();
                            ReceiveCustomDeadline.setSummary("Now : " + finalValue + (finalValue.equals("5 min") ? " (Default)" : ""));
                        }
                    }
                });

                dialog.setNeutralButton("Reset Default", (d, w) -> {
                    prefs.edit().putString("DeadlineCustomValue", "5 min").apply();
                    ReceiveCustomDeadline.setSummary("Now : 5 min (Default)");
                });
                dialog.setNegativeButton("Cancel", (d, w) -> {
                });
                dialog.setView(view);
                dialog.show();
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_OPEN_AUDIO) {
            Uri AudioMedia = (resultCode == Activity.RESULT_CANCELED || data == null ? null : data.getData());
            if(AudioMedia == null) Toast.makeText(mContext, "Please choose audio file!", Toast.LENGTH_SHORT).show();
            else {
                DocumentFile file = DocumentFile.fromSingleUri(mContext, AudioMedia);
                CustomRingtone.setSummary("Now : " + (file == null ? "system default" : file.getName()));
                mContext.getContentResolver().takePersistableUriPermission(AudioMedia, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                prefs.edit().putString("CustomRingtone", AudioMedia.toString()).apply();
                ResetCustomRingtone.setVisible(true);
            }
        }
    }
}
