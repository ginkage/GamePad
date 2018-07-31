package com.ginkage.gamepad.ui;

import android.os.Bundle;

/** Main settings activity menu, mostly static. */
public class MainSettingsActivity extends CommonPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startPreferenceFragment(new PairedDevicesFragment(), false);
    }
}
