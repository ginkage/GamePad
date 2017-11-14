package com.ginkage.gamepad.bluetooth;

import android.support.annotation.MainThread;

/** Helper class to register for the device state broadcasts. */
public interface DeviceStateBus {
  /**
   * Start listening for device connection state changes.
   *
   * @param listener Callback that will receive the new device connection state.
   */
  @MainThread
  void register(DeviceStateListener listener);

  /** Stop listening for device connection state changes. */
  @MainThread
  void unregister();
}
