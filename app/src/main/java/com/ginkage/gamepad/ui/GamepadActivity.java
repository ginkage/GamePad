/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.gamepad.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.GamepadState;
import com.ginkage.gamepad.bluetooth.HidDataSender;

public class GamepadActivity extends AppCompatActivity {
    private static final int[] eightWay = {2, 1, 1, 0, 0, 7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2};

    private final GamepadState gamepadState = new GamepadState();
    private HidDataSender hidDataSender;
    private HidDataSender.ProfileListener profileListener = new HidDataSender.ProfileListener() {
        @Override
        @MainThread
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_DISCONNECTED) {
                finish();
            }
        }

        @Override
        @MainThread
        public void onAppStatusChanged(boolean registered) {
            if (!registered) {
                finish();
            }
        }

        @Override
        @MainThread
        public void onServiceStateChanged(BluetoothHidDevice proxy) {}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_gamepad);

        hidDataSender = HidDataSender.getInstance();
        hidDataSender.register(this, profileListener);

        Button buttonA = findViewById(R.id.button_a);
        Button buttonB = findViewById(R.id.button_b);
        Button buttonX = findViewById(R.id.button_x);
        Button buttonY = findViewById(R.id.button_y);
        Button buttonL1 = findViewById(R.id.button_l1);
        Button buttonR1 = findViewById(R.id.button_r1);
        Button buttonL3 = findViewById(R.id.button_l3);
        Button buttonR3 = findViewById(R.id.button_r3);
        Button buttonStart = findViewById(R.id.button_start);
        Button buttonBack = findViewById(R.id.button_back);
        Button buttonHome = findViewById(R.id.button_home);
        ImageView dPad = findViewById(R.id.dpad);
        ImageView stickLeft = findViewById(R.id.stick_left);
        ImageView stickRight = findViewById(R.id.stick_right);
        SeekBar seekbarL2 = findViewById(R.id.seekbar_l2);
        SeekBar seekbarR2 = findViewById(R.id.seekbar_r2);

        buttonA.setOnTouchListener(this::onTouchButton);
        buttonB.setOnTouchListener(this::onTouchButton);
        buttonX.setOnTouchListener(this::onTouchButton);
        buttonY.setOnTouchListener(this::onTouchButton);
        buttonL1.setOnTouchListener(this::onTouchButton);
        buttonR1.setOnTouchListener(this::onTouchButton);
        buttonL3.setOnTouchListener(this::onTouchButton);
        buttonR3.setOnTouchListener(this::onTouchButton);
        buttonStart.setOnTouchListener(this::onTouchButton);
        buttonBack.setOnTouchListener(this::onTouchButton);
        buttonHome.setOnTouchListener(this::onTouchButton);

        dPad.setOnTouchListener(this::onTouchStick);
        stickLeft.setOnTouchListener(this::onTouchStick);
        stickRight.setOnTouchListener(this::onTouchStick);

        SeekBar.OnSeekBarChangeListener listener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int id = seekBar.getId();
                        if (id == R.id.seekbar_l2) {
                            gamepadState.l2 = progress;
                        } else if (id == R.id.seekbar_r2) {
                            gamepadState.r2 = progress;
                        }
                        send();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(0);
                        send();
                    }
                };

        seekbarL2.setOnSeekBarChangeListener(listener);
        seekbarR2.setOnSeekBarChangeListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(android.R.id.content)
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hidDataSender.unregister(this, profileListener);
    }

    public boolean onTouchButton(View v, MotionEvent event) {
        int action = event.getActionMasked();
        boolean state =
                !(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP);
        int id = v.getId();
        if (id == R.id.button_a) {
            gamepadState.a = state;
        } else if (id == R.id.button_b) {
            gamepadState.b = state;
        } else if (id == R.id.button_x) {
            gamepadState.x = state;
        } else if (id == R.id.button_y) {
            gamepadState.y = state;
        } else if (id == R.id.button_l1) {
            gamepadState.l1 = state;
        } else if (id == R.id.button_r1) {
            gamepadState.r1 = state;
        } else if (id == R.id.button_l3) {
            gamepadState.l3 = state;
        } else if (id == R.id.button_r3) {
            gamepadState.r3 = state;
        } else if (id == R.id.button_start) {
            gamepadState.start = state;
        } else if (id == R.id.button_back) {
            gamepadState.back = state;
        } else if (id == R.id.button_home) {
            gamepadState.home = state;
      } else {
            return false;
      }
        send();
        return true;
    }

    public boolean onTouchStick(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();
        float x = Math.min(Math.max(event.getX(), 0), w);
        float y = Math.min(Math.max(event.getY(), 0), h);

        boolean state =
                !(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP);
        int id = v.getId();
        if (id == R.id.dpad) {
            if (state) {
                int cx = w / 2;
                int cy = h / 2;
                float dx = x - cx;
                float dy = y - cy;
                double theta = Math.atan2(-dy, dx);
                if (theta < 0) {
                    theta += 2 * Math.PI;
                }
                int area = (int) (theta / (Math.PI / 8));
                gamepadState.dpad = eightWay[area];
            } else {
                gamepadState.dpad = 8;
            }
        } else if (id == R.id.stick_left) {
            if (state) {
                gamepadState.lx = Math.round(255 * x / w);
                gamepadState.ly = Math.round(255 * y / h);
            } else {
                gamepadState.lx = gamepadState.ly = 128;
            }
        } else if (id == R.id.stick_right) {
            if (state) {
                gamepadState.rx = Math.round(255 * x / w);
                gamepadState.ry = Math.round(255 * y / h);
            } else {
                gamepadState.rx = gamepadState.ry = 128;
            }
      } else {
            return false;
      }

        send();
        return true;
    }

    private void send() {
        hidDataSender.sendGamepad(gamepadState);
    }
}
