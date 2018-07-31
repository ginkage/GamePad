package com.ginkage.gamepad.bluetooth;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.BinderThread;
import android.support.annotation.MainThread;
import android.util.Log;
import javax.annotation.Nullable;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
class HidDeviceApp {
    private static final String TAG = "HidDeviceApp";

    /** Used to call back when a device connection state has changed. */
    public interface DeviceStateListener {
        /**
         * Callback that receives the new device connection state.
         *
         * @param device Device that was connected or disconnected.
         * @param state New connection state, see {@link BluetoothProfile.EXTRA_STATE}.
         */
        void onDeviceStateChanged(BluetoothDevice device, int state);

        /** Callback that receives the app unregister event. */
        void onAppUnregistered();
    }

    private final Context appContext;
    private final GamepadReport gamepadReport = new GamepadReport();
    private final BatteryReport batteryReport = new BatteryReport();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /** Callback to receive the HID Device's SDP record state. */
    private final BluetoothHidDevice.Callback callback =
            new BluetoothHidDevice.Callback() {
                @Override
                @BinderThread
                public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                    super.onAppStatusChanged(pluggedDevice, registered);
                    HidDeviceApp.this.registered = registered;
                    if (!registered) {
                        mainThreadHandler.post(HidDeviceApp.this::onAppUnregistered);
                    }
                }

                @Override
                @BinderThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    super.onConnectionStateChanged(device, state);
                    mainThreadHandler.post(() -> onDeviceStateChanged(device, state));
                }

                @Override
                @BinderThread
                public void onGetReport(
                        BluetoothDevice device, byte type, byte id, int bufferSize) {
                    super.onGetReport(device, type, id, bufferSize);
                    if (proxy != null) {
                        if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                            proxy.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                        } else {
                            @Nullable byte[] report = getReport(id);
                            if (report == null) {
                                proxy.reportError(
                                        device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                            } else {
                                proxy.replyReport(device, type, id, report);
                            }
                        }
                    }
                }

                @Override
                @BinderThread
                public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
                    super.onSetReport(device, type, id, data);
                    if (proxy != null) {
                        proxy.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
                    }
                }
            };

    private final BroadcastReceiver batteryReceiver =
            new BroadcastReceiver() {
                @Override
                @MainThread
                public void onReceive(Context context, Intent intent) {
                    onBatteryChanged(intent);
                }
            };

    @Nullable private BluetoothDevice hostDevice;
    @Nullable private DeviceStateListener deviceStateListener;
    @Nullable private BluetoothHidDevice proxy;
    private boolean registered;

    HidDeviceApp(Context appContext) {
        this.appContext = appContext;
    }

    /**
     * Register the HID Device's SDP record.
     *
     * @param proxy Interface for managing the paired HID Host devices and sending the data.
     */
    @MainThread
    void registerApp(BluetoothHidDevice proxy) {
        this.proxy = checkNotNull(proxy);
        this.proxy.registerApp(
                Constants.SDP_SETTINGS, null, Constants.QOS_SETTINGS, Runnable::run, callback);
    }

    /** Unregister the HID Device's SDP record. */
    @MainThread
    void unregisterApp() {
        hostDevice = null;
        if (proxy != null && registered) {
            proxy.unregisterApp();
        }
        proxy = null;
    }

    /**
     * Start listening for device connection state changes.
     *
     * @param listener Callback that will receive the new device connection state.
     */
    @MainThread
    void registerDeviceListener(DeviceStateListener listener) {
        deviceStateListener = checkNotNull(listener);
        appContext.registerReceiver(
                batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    /** Stop listening for device connection state changes. */
    @MainThread
    void unregisterDeviceListener() {
        appContext.unregisterReceiver(batteryReceiver);
        deviceStateListener = null;
    }

    /**
     * Notify that we have a new HID Host to send the data to.
     *
     * @param device New device or {@code null} if we should stop sending any data.
     */
    @MainThread
    void setHostDevice(@Nullable BluetoothDevice device) {
        hostDevice = device;
    }

    /** Send the Gamepad data to the connected HID Host device. */
    @MainThread
    void sendGamepad(GamepadState state) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        gamepadReport.setValue(state);
        sendReport(Constants.ID_GAMEPAD);
    }

    @MainThread
    private void onDeviceStateChanged(BluetoothDevice device, int state) {
        if (deviceStateListener != null) {
            deviceStateListener.onDeviceStateChanged(device, state);
      }
    }

    @MainThread
    private void onAppUnregistered() {
        if (deviceStateListener != null) {
            deviceStateListener.onAppUnregistered();
        }
    }

    @MainThread
    private void onBatteryChanged(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            batteryReport.setValue((float) level / (float) scale);
            sendReport(Constants.ID_BATTERY);
        } else {
            Log.e(TAG, "Bad battery level data received: level=" + level + ", scale=" + scale);
        }
    }

    @MainThread
    private void sendReport(byte id) {
        if (proxy != null && hostDevice != null) {
            byte[] report = getReport(id);
            if (report != null) {
                proxy.sendReport(hostDevice, id, report);
            }
        }
    }

    @Nullable
    private byte[] getReport(byte id) {
        switch (id) {
            case Constants.ID_GAMEPAD:
                return gamepadReport.getReport();

            case Constants.ID_BATTERY:
                return batteryReport.getReport();

            default: // fall out
        }

        Log.e(TAG, "Invalid report ID requested: " + id);
        return null;
    }
}
