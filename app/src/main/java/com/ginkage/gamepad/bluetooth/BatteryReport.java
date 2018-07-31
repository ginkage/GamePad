package com.ginkage.gamepad.bluetooth;

/** Helper class to store the battery state and retrieve the binary report. */
class BatteryReport {
    private final byte[] batteryData = new byte[] {0};

    /**
     * Store the current battery level in the report.
     *
     * @param level Battery level, must be in the [0.0, 1.0] interval
     */
    void setValue(float level) {
        int val = (int) Math.ceil(level * 255);
        batteryData[0] = (byte) (val & 0xff);
    }

    byte[] getReport() {
    return batteryData;
  }
}
