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

import android.Manifest.permission;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import java.util.Map;

/** Main settings activity menu, mostly static. */
public class MainSettingsActivity extends CommonPreferenceActivity {

    ActivityResultLauncher<String[]> launcher =
        registerForActivityResult(new RequestMultiplePermissions(),
            result -> {
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!entry.getValue()) {
                        finish();
                    }
                }
                startPreferenceFragment(new PairedDevicesFragment(), false);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            launcher.launch(new String[] {
                permission.BLUETOOTH_SCAN,
                permission.BLUETOOTH_ADVERTISE,
                permission.BLUETOOTH_CONNECT
            });
        } else {
            startPreferenceFragment(new PairedDevicesFragment(), false);
        }
    }
}
