package com.ginkage.gamepad.bluetooth;

import java.util.Arrays;

public class GamepadReport {
  private final byte[] gamepadData = "BDLLRRTT".getBytes();

  public GamepadReport() {
    Arrays.fill(gamepadData, (byte) 0);
  }

  public byte[] setValue(GamepadState s) {
    // 11 buttons: A, B, X, Y, L1, R1, L3, R3, Power, Back, Home (1 bit per button), 1 bit padding
    // 4 bits for D-pad rotation values 0-7 -> 0-315 (360 - 45)
    // 6x 8-bit values for LX/LY, RX/RY, L2/R2
    gamepadData[0] = 0;
    gamepadData[0] |= (byte) (s.a ? 0x01 : 0);
    gamepadData[0] |= (byte) (s.b ? 0x02 : 0);
    gamepadData[0] |= (byte) (s.x ? 0x04 : 0);
    gamepadData[0] |= (byte) (s.y ? 0x08 : 0);
    gamepadData[0] |= (byte) (s.l1 ? 0x10 : 0);
    gamepadData[0] |= (byte) (s.r1 ? 0x20 : 0);
    gamepadData[0] |= (byte) (s.l3 ? 0x40 : 0);
    gamepadData[0] |= (byte) (s.r3 ? 0x80 : 0);
    gamepadData[1] = 0;
    gamepadData[1] |= (byte) (s.power ? 0x01 : 0);
    gamepadData[1] |= (byte) (s.back ? 0x02 : 0);
    gamepadData[1] |= (byte) (s.home ? 0x04 : 0);
    gamepadData[1] |= (byte) (s.dpad << 4);
    gamepadData[2] = (byte) s.lx;
    gamepadData[3] = (byte) s.ly;
    gamepadData[4] = (byte) s.rx;
    gamepadData[5] = (byte) s.ry;
    gamepadData[6] = (byte) s.l2;
    gamepadData[7] = (byte) s.r2;
    return gamepadData;
  }

  public byte[] getReport() {
    return gamepadData;
  }
}