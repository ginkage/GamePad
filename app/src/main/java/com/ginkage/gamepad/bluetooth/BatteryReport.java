package com.ginkage.gamepad.bluetooth;

/** Helper class to store the battery state and retrieve the binary report. */
public class BatteryReport {
  private final byte[] batteryData = new byte[] {0};

  /**
   * Store the current battery level in the report.
   *
   * @param level Battery level, must be in the [0.0, 1.0] interval
   * @return Byte array that represents the report
   */
  public byte[] setValue(float level) {
    int val = (int) Math.ceil(level * 255);
    batteryData[0] = (byte) (val & 0xff);
    return batteryData;
  }

  public byte[] getReport() {
    return batteryData;
  }
}
