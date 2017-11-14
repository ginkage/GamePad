package com.ginkage.gamepad.bluetooth;

/** Interface to send the Mouse data with. */
public interface GamepadDataSender {
  /**
   * Send the Gamepad data to the connected HID Host device.
   */
  void sendGamepad(GamepadState state);
}
