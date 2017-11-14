package com.ginkage.gamepad.bluetooth;

public class GamepadState {
  public boolean a;
  public boolean b;
  public boolean x;
  public boolean y;
  public boolean l1;
  public boolean r1;
  public boolean l3;
  public boolean r3;
  public boolean start;
  public boolean back;
  public boolean home;

  // 0=up, 2=right, 4=down, 6=left, 8=release
  public int dpad;

  // Sticks: Up=0, Down=255, Left=0, Right=255, Center=128
  public int lx;
  public int ly;
  public int rx;
  public int ry;

  // Triggers: Released=0, Pressed=255
  public int l2;
  public int r2;
}
