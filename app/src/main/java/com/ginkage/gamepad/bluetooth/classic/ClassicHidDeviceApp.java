package com.ginkage.gamepad.bluetooth.classic;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.BinderThread;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.ginkage.gamepad.bluetooth.BatteryReport;
import com.ginkage.gamepad.bluetooth.DeviceStateBus;
import com.ginkage.gamepad.bluetooth.DeviceStateListener;
import com.ginkage.gamepad.bluetooth.GamepadReport;
import com.ginkage.gamepad.bluetooth.GamepadState;
import com.ginkage.gamepad.bluetooth.HidDeviceApp;
import com.google.android.clockwork.bluetooth.InputHost;
import javax.annotation.Nullable;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
public class ClassicHidDeviceApp implements HidDeviceApp, DeviceStateBus {

  private static final String TAG = "HidDeviceApp";

  private final GamepadReport gamepadReport = new GamepadReport();
  private final BatteryReport batteryReport = new BatteryReport();

  /** Callback to receive the HID Device's SDP record state. */
  private final CallbackWrapper callback =
      new CallbackWrapper() {
        @Override
        @BinderThread
        public void onAppStatusChanged(
            BluetoothDevice pluggedDevice, AppConfigurationWrapper config, boolean registered) {
          ClassicHidDeviceApp.this.config = registered ? config : null;
        }

        @Override
        @BinderThread
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
          new Handler(Looper.getMainLooper())
              .post(
                  () -> {
                    if (deviceStateListener != null) {
                      deviceStateListener.onDeviceStateChanged(device, state);
                    }
                  });
        }

        @Override
        @BinderThread
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
          if (inputHost != null) {
            if (type == InputHost.REPORT_TYPE_INPUT) {
              switch (id) {
                case Constants.ID_GAMEPAD:
                  inputHost.replyReport(device, type, id, gamepadReport.getReport());
                  return;

                case Constants.ID_BATTERY:
                  inputHost.replyReport(device, type, id, batteryReport.getReport());
                  return;

                default:
                  Log.e(TAG, "Invalid report ID requested: " + id);
                  inputHost.reportError(device, InputHost.ERROR_RSP_INVALID_RPT_ID);
                  return;
              }
            }

            inputHost.reportError(device, InputHost.ERROR_RSP_UNSUPPORTED_REQ);
          }
        }

        @Override
        @BinderThread
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
          if (inputHost != null) {
            inputHost.reportError(device, InputHost.ERROR_RSP_SUCCESS);
          }
        }
      };

  @Nullable private AppConfigurationWrapper config;
  @Nullable private InputHostWrapper inputHost;
  @Nullable private BluetoothDevice device;
  @Nullable private DeviceStateListener deviceStateListener;

  @Override
  @MainThread
  public void registerApp(BluetoothProfile inputHost) {
    registerApp(new InputHostWrapper(inputHost));
  }

  @MainThread
  @VisibleForTesting
  void registerApp(InputHostWrapper inputHost) {
    this.inputHost = checkNotNull(inputHost);
    this.inputHost.registerApp(callback);
  }

  @Override
  @MainThread
  public void unregisterApp() {
    if (inputHost != null && config != null) {
      inputHost.unregisterApp(config);
    }
    inputHost = null;
  }

  @Override
  @MainThread
  public void register(DeviceStateListener listener) {
    deviceStateListener = checkNotNull(listener);
  }

  @Override
  @MainThread
  public void unregister() {
    deviceStateListener = null;
  }

  @Override
  @MainThread
  public void setDevice(@Nullable BluetoothDevice device) {
    this.device = device;
  }

  @Override
  @MainThread
  public void sendGamepad(GamepadState state) {
    // Store the current values in case the host will try to read them with a GET_REPORT call.
    byte[] report = gamepadReport.setValue(state);
    if (inputHost != null && device != null) {
      inputHost.sendReport(device, Constants.ID_GAMEPAD, report);
    }
  }

  @Override
  @MainThread
  public void sendBatteryLevel(float level) {
    // Store the current values in case the host will try to read them with a GET_REPORT call.
    byte[] report = batteryReport.setValue(level);
    if (inputHost != null && device != null) {
      inputHost.sendReport(device, Constants.ID_BATTERY, report);
    }
  }
}
