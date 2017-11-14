package com.ginkage.gamepad.bluetooth;

/** Interface to send the Battery data with. */
interface BatteryDataSender {
  /**
   * Send the Battery data to the connected HID Host device.
   *
   * @param level Current battery level
   */
  void sendBatteryLevel(float level);
}
