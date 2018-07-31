package com.ginkage.gamepad.bluetooth;

import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;

/** Handy constants that are shared between Classic and BLE modes. */
class Constants {
    static final byte ID_GAMEPAD = 1;
    static final byte ID_BATTERY = 2;

    private static final byte[] HIDD_REPORT_DESC = {
        (byte) 0x05,
        (byte) 0x01, // Usage Page (Generic Desktop Ctrls)
        (byte) 0x09,
        (byte) 0x05, // Usage (Game Pad)
        (byte) 0xA1,
        (byte) 0x01, // Collection (Application)
        (byte) 0x85,
        ID_GAMEPAD, //   Report ID

        // 11 buttons: A, B, X, Y, L1, R1, L3, R3, Start, Back, Home (1 bit per button)

        (byte) 0x05,
        (byte) 0x09, //   Usage Page (Button)
        (byte) 0x09,
        (byte) 0x01, //   Usage (0x01 - A)
        (byte) 0x09,
        (byte) 0x02, //   Usage (0x02 - B)
        (byte) 0x09,
        (byte) 0x04, //   Usage (0x04 - X)
        (byte) 0x09,
        (byte) 0x05, //   Usage (0x05 - Y)
        (byte) 0x09,
        (byte) 0x07, //   Usage (0x07 - L1)
        (byte) 0x09,
        (byte) 0x08, //   Usage (0x08 - R1)
        (byte) 0x09,
        (byte) 0x0E, //   Usage (0x0E - L3)
        (byte) 0x09,
        (byte) 0x0F, //   Usage (0x0F - R3)
        (byte) 0x09,
        (byte) 0x0C, //   Usage (0x0D - Start)
        (byte) 0x05,
        (byte) 0x0C, //   Usage Page (Consumer)
        (byte) 0x0A,
        (byte) 0x24,
        (byte) 0x02, //   Usage (AC Back)
        (byte) 0x0A,
        (byte) 0x23,
        (byte) 0x02, //   Usage (AC Home)
        (byte) 0x15,
        (byte) 0x00, //   Logical Minimum (0)
        (byte) 0x25,
        (byte) 0x01, //   Logical Maximum (1)
        (byte) 0x75,
        (byte) 0x01, //   Report Size (1)
        (byte) 0x95,
        (byte) 0x0B, //   Report Count (11)
        (byte) 0x81,
        (byte) 0x02, //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)

        // 1 bit padding

        (byte) 0x75,
        (byte) 0x01, //   Report Size (1)
        (byte) 0x95,
        (byte) 0x01, //   Report Count (1)
        (byte) 0x81,
        (byte) 0x03, //   Input (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)

        // 4 bits for D-pad rotation values 0-7 -> 0-315 (360 - 45)
        // 0=up, 2=right, 4=down, 6=left, 8=release

        (byte) 0x05,
        (byte) 0x01, //   Usage Page (Generic Desktop Ctrls)
        (byte) 0x75,
        (byte) 0x04, //   Report Size (4)
        (byte) 0x95,
        (byte) 0x01, //   Report Count (1)
        (byte) 0x25,
        (byte) 0x07, //   Logical Maximum (7)
        (byte) 0x46,
        (byte) 0x3B,
        (byte) 0x01, //   Physical Maximum (315)
        (byte) 0x66,
        (byte) 0x14,
        (byte) 0x00, //   Unit (System: English Rotation, Length: Centimeter)
        (byte) 0x09,
        (byte) 0x39, //   Usage (Hat switch)
        (byte) 0x81,
        (byte) 0x42, //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,Null State)

        // 6x 8-bit values for LX/LY, RX/RY, L2/R2
        // Sticks: Up=0, Down=FF, Left=0, Right=FF, Center=80
        // Triggers: Released=0, Pressed=FF

        (byte) 0x66,
        (byte) 0x00,
        (byte) 0x00, //   Unit (None)
        (byte) 0xA1,
        (byte) 0x00, //   Collection (Physical)
        (byte) 0x09,
        (byte) 0x30, //     Usage (X)
        (byte) 0x09,
        (byte) 0x31, //     Usage (Y)
        (byte) 0x09,
        (byte) 0x32, //     Usage (Z)
        (byte) 0x09,
        (byte) 0x35, //     Usage (Rz)
        (byte) 0x05,
        (byte) 0x02, //     Usage Page (Sim Ctrls)
        (byte) 0x09,
        (byte) 0xC5, //     Usage (Brake)
        (byte) 0x09,
        (byte) 0xC4, //     Usage (Accelerator)
        (byte) 0x15,
        (byte) 0x00, //     Logical Minimum (0)
        (byte) 0x26,
        (byte) 0xFF,
        (byte) 0x00, //     Logical Maximum (255)
        (byte) 0x35,
        (byte) 0x00, //     Physical Minimum (0)
        (byte) 0x46,
        (byte) 0xFF,
        (byte) 0x00, //     Physical Maximum (255)
        (byte) 0x75,
        (byte) 0x08, //     Report Size (8)
        (byte) 0x95,
        (byte) 0x06, //     Report Count (6)
        (byte) 0x81,
        (byte) 0x02, //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        (byte) 0xC0, //   End Collection
        (byte) 0xC0, // End Collection

        // Battery level, 1 byte, 0-FF

        (byte) 0x05,
        (byte) 0x01, // Usage Page (Generic Desktop Ctrls)
        (byte) 0x09,
        (byte) 0x05, // Usage (Game Pad)
        (byte) 0xA1,
        (byte) 0x01, // Collection (Application)
        (byte) 0x85,
        ID_BATTERY, //   Report ID
        (byte) 0x05,
        (byte) 0x06, //   Usage Page (Generic Dev Ctrls)
        (byte) 0x09,
        (byte) 0x20, //   Usage (Battery Strength)
        (byte) 0x15,
        (byte) 0x00, //   Logical Minimum (0)
        (byte) 0x26,
        (byte) 0xFF,
        (byte) 0x00, //   Logical Maximum (255)
        (byte) 0x75,
        (byte) 0x08, //   Report Size (8)
        (byte) 0x95,
        (byte) 0x01, //   Report Count (1)
        (byte) 0x81,
        (byte) 0x02, //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        (byte) 0xC0, // End Collection
    };

    private static final String SDP_NAME = "Android Gamepad";
    private static final String SDP_DESCRIPTION = "Android HID Device";
    private static final String SDP_PROVIDER = "Google Inc.";
    private static final int QOS_TOKEN_RATE = 800; // 9 bytes * 1000000 us / 11250 us
    private static final int QOS_TOKEN_BUCKET_SIZE = 9;
    private static final int QOS_PEAK_BANDWIDTH = 0;
    private static final int QOS_LATENCY = 11250;

    static final BluetoothHidDeviceAppSdpSettings SDP_SETTINGS =
            new BluetoothHidDeviceAppSdpSettings(
                    SDP_NAME,
                    SDP_DESCRIPTION,
                    SDP_PROVIDER,
                    BluetoothHidDevice.SUBCLASS2_GAMEPAD,
                    HIDD_REPORT_DESC);

    static final BluetoothHidDeviceAppQosSettings QOS_SETTINGS =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    QOS_TOKEN_RATE,
                    QOS_TOKEN_BUCKET_SIZE,
                    QOS_PEAK_BANDWIDTH,
                    QOS_LATENCY,
                    BluetoothHidDeviceAppQosSettings.MAX);
}
