package com.ginkage.gamepad.bluetooth;

import android.bluetooth.BluetoothDevice;

/** Used to call back when a device connection state has changed. */
public interface DeviceStateListener {
  /**
   * Callback that receives the new device connection state.
   *
   * @param device Device that was connected or disconnected.
   * @param state New connection state, see {@link BluetoothProfile.EXTRA_STATE}.
   */
  void onDeviceStateChanged(BluetoothDevice device, int state);
}
