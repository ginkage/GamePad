package com.ginkage.gamepad.bluetooth;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.BinderThread;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import android.util.Log;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
public class HidDeviceApp
        implements GamepadReport.GamepadDataSender,
                BatteryReport.BatteryDataSender {

    private static final String TAG = "HidDeviceApp";

    /** Used to call back when a device connection state has changed. */
    public interface DeviceStateListener {
        /**
         * Callback that receives the new device connection state.
         *
         * @param device Device that was connected or disconnected.
         * @param state New connection state, see {@link BluetoothProfile#EXTRA_STATE}.
         */
        @MainThread
        void onConnectionStateChanged(BluetoothDevice device, int state);

        /** Callback that receives the app unregister event. */
        @MainThread
        void onAppStatusChanged(boolean registered);
    }

    private final GamepadReport gamepadReport = new GamepadReport();
    private final BatteryReport batteryReport = new BatteryReport();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Nullable private BluetoothDevice device;
    @Nullable private DeviceStateListener deviceStateListener;

    /** Callback to receive the HID Device's SDP record state. */
    private final BluetoothHidDevice.Callback callback =
            new BluetoothHidDevice.Callback() {
                @Override
                @BinderThread
                public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                    super.onAppStatusChanged(pluggedDevice, registered);
                    HidDeviceApp.this.registered = registered;
                    HidDeviceApp.this.onAppStatusChanged(registered);
                }

                @Override
                @BinderThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    super.onConnectionStateChanged(device, state);
                    HidDeviceApp.this.onConnectionStateChanged(device, state);
                }

                @Override
                @BinderThread
                public void onGetReport(
                        BluetoothDevice device, byte type, byte id, int bufferSize) {
                    super.onGetReport(device, type, id, bufferSize);
                    if (proxy != null) {
                        if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                            proxy.reportError(
                                    device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                        } else if (!replyReport(device, type, id)) {
                            proxy.reportError(
                                    device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
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

    @Nullable private BluetoothHidDevice proxy;
    private boolean registered;

    /**
     * Register the HID Device's SDP record.
     *
     * @param proxy Interface for managing the paired HID Host devices and sending the data.
     */
    @MainThread
    void registerApp(BluetoothHidDevice proxy) {
        this.proxy = checkNotNull(proxy);
        this.proxy.registerApp(
                Constants.SDP_SETTINGS,
                null,
                Constants.QOS_SETTINGS,
                Runnable::run,
                callback);
    }

    /** Unregister the HID Device's SDP record. */
    @MainThread
    void unregisterApp() {
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
    }

    /** Stop listening for device connection state changes. */
    @MainThread
    void unregisterDeviceListener() {
        deviceStateListener = null;
    }

    /**
     * Notify that we have a new HID Host to send the data to.
     *
     * @param device New device or {@code null} if we should stop sending any data.
     */
    @MainThread
    public void setDevice(@Nullable BluetoothDevice device) {
        this.device = device;
    }

    @Override
    @MainThread
    public void sendGamepad(GamepadState state) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = gamepadReport.setValue(state);
        if (proxy != null && device != null) {
            proxy.sendReport(device, Constants.ID_GAMEPAD, report);
        }
    }

    @Override
    @MainThread
    public void sendBatteryLevel(float level) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = batteryReport.setValue(level);
        if (proxy != null && device != null) {
            proxy.sendReport(device, Constants.ID_BATTERY, report);
        }
    }

    @BinderThread
    private void onConnectionStateChanged(BluetoothDevice device, int state) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onConnectionStateChanged(device, state);
            }
        });
    }

    @BinderThread
    private void onAppStatusChanged(boolean registered) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onAppStatusChanged(registered);
            }
        });
    }

    @BinderThread
    private boolean replyReport(BluetoothDevice device, byte type, byte id) {
        @Nullable byte[] report = getReport(id);
        if (report == null) {
            return false;
        }

        if (proxy != null) {
            proxy.replyReport(device, type, id, report);
        }
        return true;
    }

    @BinderThread
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