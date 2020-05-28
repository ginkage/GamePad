package com.ginkage.gamepad.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class PairedDeviceDialog extends PreferenceDialogFragmentCompat {
    public static final String TAG = "PairedDeviceDialog";

    private static final String ARG_TITLE = "title";
    private int whichButtonClicked = DialogInterface.BUTTON_NEUTRAL;

    public static PairedDeviceDialog newInstance(Preference preference) {
        PairedDeviceDialog fragment = new PairedDeviceDialog();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, preference.getKey());
        args.putCharSequence(ARG_TITLE, preference.getTitle());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        whichButtonClicked = which;
        super.onClick(dialog, which);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (whichButtonClicked != DialogInterface.BUTTON_NEUTRAL) {
            ((BluetoothDevicePreference) getPreference()).onDialogClosed(positiveResult);
        }
    }
}
