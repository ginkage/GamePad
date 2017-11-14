package com.ginkage.gamepad.bluetooth;

import android.bluetooth.BluetoothDevice;
import java.util.List;

/** Wrapper for BluetoothInputHost profile that manages paired HID Host devices. */
public interface HidHostProfile {

  /**
   * Check if a device supports HID Host profile.
   *
   * @param device Device to check.
   * @return {@code true} if the HID Host profile is supported, {@code false} otherwise.
   */
  boolean isProfileSupported(final BluetoothDevice device);

  /**
   * Examine the device for current connection status.
   *
   * @param device Remote Bluetooth device to examine.
   * @return A Bluetooth profile connection state.
   */
  int getConnectionState(final BluetoothDevice device);

  /**
   * Initiate the connection to the profile proxy service.
   *
   * @param listener Callback that will receive the profile proxy object.
   */
  void register(ServiceStateListener listener);

  /** Close the profile service connection. */
  void unregister();

  /**
   * Initiate the connection to the remote HID Host device.
   *
   * @param device Device to connect to.
   */
  void connect(BluetoothDevice device);

  /**
   * Close the connection with the remote HID Host device.
   *
   * @param device Device to disconnect from.
   */
  void disconnect(BluetoothDevice device);

  /**
   * Get all devices that are in the "Connected" state.
   *
   * @return Connected devices list.
   */
  List<BluetoothDevice> getConnectedDevices();

  /**
   * Get all devices that match one of the specified connection states.
   *
   * @param states List of states we are interested in.
   * @return List of devices that match one of the states.
   */
  List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states);
}
