package com.ginkage.gamepad.bluetooth.classic;

import android.bluetooth.BluetoothDevice;
import com.google.android.clockwork.bluetooth.HidDeviceAppConfiguration;
import com.google.android.clockwork.bluetooth.HidDeviceCallback;

/** Wrapper around the HidDeviceCallback class to make it testable. */
abstract class CallbackWrapper {
  abstract void onAppStatusChanged(
      BluetoothDevice pluggedDevice, AppConfigurationWrapper config, boolean registered);

  abstract void onConnectionStateChanged(BluetoothDevice device, int state);

  abstract void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize);

  abstract void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data);

  HidDeviceCallback wrap() {
    return new HidDeviceCallback() {
      @Override
      public void onAppStatusChanged(
          BluetoothDevice pluggedDevice, HidDeviceAppConfiguration config, boolean registered) {
        super.onAppStatusChanged(pluggedDevice, config, registered);
        CallbackWrapper.this.onAppStatusChanged(
            pluggedDevice, new AppConfigurationWrapper(config), registered);
      }

      @Override
      public void onConnectionStateChanged(BluetoothDevice device, int state) {
        super.onConnectionStateChanged(device, state);
        CallbackWrapper.this.onConnectionStateChanged(device, state);
      }

      @Override
      public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
        super.onGetReport(device, type, id, bufferSize);
        CallbackWrapper.this.onGetReport(device, type, id, bufferSize);
      }

      @Override
      public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
        super.onSetReport(device, type, id, data);
        CallbackWrapper.this.onSetReport(device, type, id, data);
      }
    };
  }
}
