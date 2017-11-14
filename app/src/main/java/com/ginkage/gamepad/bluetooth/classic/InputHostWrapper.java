package com.ginkage.gamepad.bluetooth.classic;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import com.google.android.clockwork.bluetooth.HidDeviceAppQosSettings;
import com.google.android.clockwork.bluetooth.HidDeviceAppSdpSettings;
import com.google.android.clockwork.bluetooth.InputHost;

/** Wrapper around the final InputHost class to make it mockable. */
class InputHostWrapper {
  private static final String SDP_NAME = "Android Gamepad";
  private static final String SDP_DESCRIPTION = "Android HID Device";
  private static final String SDP_PROVIDER = "Google Inc.";
  private static final int QOS_TOKEN_RATE = 800; // 9 bytes * 1000000 us / 11250 us
  private static final int QOS_TOKEN_BUCKET_SIZE = 9;
  private static final int QOS_PEAK_BANDWIDTH = 0;
  private static final int QOS_LATENCY = 11250;

  private final HidDeviceAppSdpSettings sdp =
      new HidDeviceAppSdpSettings(
          SDP_NAME,
          SDP_DESCRIPTION,
          SDP_PROVIDER,
          InputHost.SUBCLASS1_COMBO,
          Constants.HIDD_REPORT_DESC);

  private final HidDeviceAppQosSettings qos =
      new HidDeviceAppQosSettings(
          HidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
          QOS_TOKEN_RATE,
          QOS_TOKEN_BUCKET_SIZE,
          QOS_PEAK_BANDWIDTH,
          QOS_LATENCY,
          HidDeviceAppQosSettings.MAX);

  private final InputHost inputHost;

  InputHostWrapper(BluetoothProfile inputHost) {
    this.inputHost = new InputHost(inputHost);
  }

  boolean registerApp(CallbackWrapper callback) {
    return inputHost.registerApp(sdp, null, qos, callback.wrap());
  }

  boolean unregisterApp(AppConfigurationWrapper config) {
    return inputHost.unregisterApp(config.unwrap());
  }

  boolean sendReport(BluetoothDevice device, int id, byte[] data) {
    return inputHost.sendReport(device, id, data);
  }

  boolean replyReport(BluetoothDevice device, byte type, byte id, byte[] data) {
    return inputHost.replyReport(device, type, id, data);
  }

  boolean reportError(BluetoothDevice device, byte error) {
    return inputHost.reportError(device, error);
  }
}
