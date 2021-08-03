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

import android.Manifest;
import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

/** Main settings activity menu, mostly static. */
public class MainSettingsActivity extends CommonPreferenceActivity {

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPreferenceFragment(new PairedDevicesFragment(), false);
                return;
            }
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            List<String> missingPermissions = new ArrayList<>();
            if (checkSelfPermission(permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission.BLUETOOTH_ADVERTISE);
            }
            if (checkSelfPermission(permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission.BLUETOOTH_CONNECT);
            }

            if (!missingPermissions.isEmpty()) {
                requestPermissions(missingPermissions.toArray(new String[0]), 1);
                return;
            }
        }

        startPreferenceFragment(new PairedDevicesFragment(), false);
    }
}
