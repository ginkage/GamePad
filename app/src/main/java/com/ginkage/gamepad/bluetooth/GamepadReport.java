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

package com.ginkage.gamepad.bluetooth;

import java.util.Arrays;

/** Helper class to store the gamepad state and retrieve the binary report. */
class GamepadReport {
    private final byte[] gamepadData = "BDLLRRTT".getBytes();

    GamepadReport() {
        Arrays.fill(gamepadData, (byte) 0);
    }

    /**
     * Convert the state structure to the binary representation.
     *
     * @param s The gamepad state to serialize
     */
    byte[] setValue(GamepadState s) {
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
        gamepadData[1] |= (byte) (s.start ? 0x01 : 0);
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

    byte[] getReport() {
    return gamepadData;
  }

    /** Interface to send the Mouse data with. */
    public interface GamepadDataSender {
        /**
         * Send the Gamepad data to the connected HID Host device.
         *
         * @param state The current state of the gamepad.
         */
        void sendGamepad(GamepadState state);
    }
}
