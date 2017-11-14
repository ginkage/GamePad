package com.ginkage.gamepad.bluetooth;

import android.bluetooth.BluetoothProfile;

/** Used to call back when a profile proxy connection state has changed. */
public interface ServiceStateListener {
  /**
   * Callback to receive the new profile proxy object.
   *
   * @param proxy Profile proxy object or {@code null} if the service was disconnected.
   */
  void onServiceStateChanged(BluetoothProfile proxy);
}
