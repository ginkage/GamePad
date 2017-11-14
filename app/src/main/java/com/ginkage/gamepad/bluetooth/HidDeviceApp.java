package com.ginkage.gamepad.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.MainThread;
import javax.annotation.Nullable;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
public interface HidDeviceApp extends GamepadDataSender, BatteryDataSender {
  /**
   * Register the HID Device's SDP record.
   *
   * @param inputHost Interface for managing the paired HID Host devices and sending the data.
   */
  @MainThread
  void registerApp(BluetoothProfile inputHost);

  /** Unregister the HID Device's SDP record. */
  @MainThread
  void unregisterApp();

  /**
   * Notify that we have a new HID Host to send the data to.
   *
   * @param device New device or {@code null} if we should stop sending any data.
   */
  @MainThread
  void setDevice(@Nullable BluetoothDevice device);
}
