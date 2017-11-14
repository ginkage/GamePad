package com.ginkage.gamepad.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.GamepadState;

public class GamepadFragment extends Fragment {
  private final GamepadState gamepadState = new GamepadState();

  Button buttonA;
  Button buttonB;
  Button buttonX;
  Button buttonY;
  Button buttonL1;
  Button buttonR1;
  ImageButton buttonL3;
  ImageButton buttonR3;
  Button buttonPower;
  Button buttonBack;
  Button buttonHome;
  ImageView dPad;
  ImageView stickLeft;
  ImageView stickRight;
  SeekBar seekbarL2;
  SeekBar seekbarR2;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.layout_gamepad, container, false);
    root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN);

    buttonA = root.findViewById(R.id.button_a);
    buttonB = root.findViewById(R.id.button_b);
    buttonX = root.findViewById(R.id.button_x);
    buttonY = root.findViewById(R.id.button_y);
    buttonL1 = root.findViewById(R.id.button_l1);
    buttonR1 = root.findViewById(R.id.button_r1);
    buttonL3 = root.findViewById(R.id.button_l3);
    buttonR3 = root.findViewById(R.id.button_r3);
    buttonPower = root.findViewById(R.id.button_power);
    buttonBack = root.findViewById(R.id.button_back);
    buttonHome = root.findViewById(R.id.button_home);
    dPad = root.findViewById(R.id.dpad);
    stickLeft = root.findViewById(R.id.stick_left);
    stickRight = root.findViewById(R.id.stick_right);
    seekbarL2 = root.findViewById(R.id.seekbar_l2);
    seekbarR2 = root.findViewById(R.id.seekbar_r2);

    buttonA.setOnTouchListener(this::onTouch);
    buttonB.setOnTouchListener(this::onTouch);
    buttonX.setOnTouchListener(this::onTouch);
    buttonY.setOnTouchListener(this::onTouch);
    buttonL1.setOnTouchListener(this::onTouch);
    buttonR1.setOnTouchListener(this::onTouch);
    buttonL3.setOnTouchListener(this::onTouch);
    buttonR3.setOnTouchListener(this::onTouch);
    buttonPower.setOnTouchListener(this::onTouch);
    buttonBack.setOnTouchListener(this::onTouch);
    buttonHome.setOnTouchListener(this::onTouch);

    return root;
  }

  public boolean onTouch(View v, MotionEvent event) {
    int action = event.getActionMasked();
    boolean state = !(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP);
    switch (v.getId()) {
      case R.id.button_a:
        gamepadState.a = state;
        break;
      case R.id.button_b:
        gamepadState.b = state;
        break;
      case R.id.button_x:
        gamepadState.x = state;
        break;
      case R.id.button_y:
        gamepadState.y = state;
        break;
      case R.id.button_l1:
        gamepadState.l1 = state;
        break;
      case R.id.button_r1:
        gamepadState.r1 = state;
        break;
      case R.id.button_l3:
        gamepadState.l3 = state;
        break;
      case R.id.button_r3:
        gamepadState.r3 = state;
        break;
      case R.id.button_power:
        gamepadState.power = state;
        break;
      case R.id.button_back:
        gamepadState.back = state;
        break;
      case R.id.button_home:
        gamepadState.home = state;
        break;
      default:
        return false;
    }
    return true;
  }
}
