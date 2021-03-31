/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
