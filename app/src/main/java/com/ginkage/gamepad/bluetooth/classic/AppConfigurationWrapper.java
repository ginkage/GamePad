package com.ginkage.gamepad.bluetooth.classic;

import com.google.android.clockwork.bluetooth.HidDeviceAppConfiguration;

/** Wrapper around the final HidDeviceAppConfiguration class to make it mockable. */
class AppConfigurationWrapper {
  private final HidDeviceAppConfiguration config;

  AppConfigurationWrapper(HidDeviceAppConfiguration config) {
    this.config = config;
  }

  HidDeviceAppConfiguration unwrap() {
    return config;
  }
}
