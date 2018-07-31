package com.ginkage.gamepad.bluetooth;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.MainThread;
import android.util.ArraySet;
import com.ginkage.gamepad.bluetooth.HidDeviceApp.DeviceStateListener;
import com.ginkage.gamepad.bluetooth.HidDeviceProfile.ServiceStateListener;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Central point for enabling the HID SDP record and sending all data. */
public class HidDataSender {
  private static final String TAG = "HidDataSender";
  private static final Object LOCK = new Object();
  @Nullable private static HidDataSender instance;

  /** Compound interface that listens to both device and service state changes. */
  public interface ProfileListener extends DeviceStateListener, ServiceStateListener {}

  private final ProfileListener profileListener =
      new ProfileListener() {
        @Override
        @MainThread
        public void onServiceStateChanged(BluetoothHidDevice proxy) {
          synchronized (lock) {
            if (proxy != null) {
              hidDeviceApp.registerApp(proxy);
            }
            updateDeviceList();
            for (ProfileListener listener : listeners) {
              listener.onServiceStateChanged(proxy);
            }
          }
        }

        @Override
        @MainThread
        public void onDeviceStateChanged(BluetoothDevice device, int state) {
          synchronized (lock) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
              // A new connection was established. If we weren't expecting that, it must be an
              // incoming one. In that case, we shouldn't try to disconnect from it.
              requestedDevice = device;
            }
            updateDeviceList();
            for (ProfileListener listener : listeners) {
              listener.onDeviceStateChanged(device, state);
            }
          }
        }

        @Override
        @MainThread
        public void onAppUnregistered() {
          synchronized (lock) {
            for (ProfileListener listener : listeners) {
              listener.onAppUnregistered();
            }
          }
        }
      };

  private final HidDeviceApp hidDeviceApp;
  private final HidDeviceProfile hidDeviceProfile;
  private final Object lock = new Object();

  @GuardedBy("lock")
  private final Set<ProfileListener> listeners = new ArraySet<>();

  @GuardedBy("lock")
  @Nullable
  private BluetoothDevice connectedDevice;

  @GuardedBy("lock")
  @Nullable
  private BluetoothDevice requestedDevice;

  private HidDataSender(Context appContext) {
    hidDeviceApp = new HidDeviceApp(appContext);
    hidDeviceProfile = new HidDeviceProfile(appContext);
  }

  /**
   * Retrieve the singleton instance of the class.
   *
   * @param context Ensures that only Activities or Services access the singleton.
   * @return Singleton instance.
   */
  public static HidDataSender getInstance(Context context) {
    Context appContext = checkNotNull(context).getApplicationContext();
    synchronized (LOCK) {
      if (instance == null) {
        instance = new HidDataSender(appContext);
      }
      return instance;
    }
  }

  /**
   * Ensure that the HID Device SDP record is registered and start listening for the profile proxy
   * and HID Host connection state changes.
   *
   * @param listener Callback that will receive the profile events.
   * @return Interface for managing the paired HID Host devices.
   */
  @MainThread
  public HidDeviceProfile register(ProfileListener listener) {
    synchronized (lock) {
      if (!listeners.add(listener)) {
        // This user is already registered
        return hidDeviceProfile;
      }
      if (listeners.size() > 1) {
        // There are already some users
        return hidDeviceProfile;
      }

      hidDeviceProfile.registerServiceListener(profileListener);
      hidDeviceApp.registerDeviceListener(profileListener);
    }
    return hidDeviceProfile;
  }

  /**
   * Stop listening for the profile events. When the last listener is unregistered, the SD record
   * for HID Device will also be unregistered.
   *
   * @param listener Callback to unregister.
   */
  @MainThread
  public void unregister(ProfileListener listener) {
    synchronized (lock) {
      if (!listeners.remove(listener)) {
        // This user was removed before
        return;
      }
      if (!listeners.isEmpty()) {
        // Some users are still left
        return;
      }

      hidDeviceApp.unregisterDeviceListener();

      for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
        hidDeviceProfile.disconnect(device);
      }

      hidDeviceApp.unregisterApp();
      hidDeviceProfile.unregisterServiceListener();

      connectedDevice = null;
      requestedDevice = null;
    }
  }

  /**
   * Initiate connection sequence for the specified HID Host. If another device is already
   * connected, it will be disconnected first. If the parameter is {@code null}, then the service
   * will only disconnect from the current device.
   *
   * @param device New HID Host to connect to.
   */
  @MainThread
  public void requestConnect(BluetoothDevice device) {
    synchronized (lock) {
      requestedDevice = device;
      connectedDevice = null;

      updateDeviceList();

      if (device != null && device.equals(connectedDevice)) {
        for (ProfileListener listener : listeners) {
          listener.onDeviceStateChanged(device, BluetoothProfile.STATE_CONNECTED);
        }
      }
    }
  }

  /** Send the Gamepad data to the connected HID Host device. */
  @MainThread
  public void sendGamepad(GamepadState state) {
    synchronized (lock) {
      if (connectedDevice != null) {
        hidDeviceApp.sendGamepad(state);
      }
    }
  }

  @MainThread
  private void updateDeviceList() {
    synchronized (lock) {
      BluetoothDevice connected = null;

      // If we are connected to some device, but want to connect to another (or disconnect
      // completely), then we should disconnect all other devices first.
      for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
        if (device.equals(requestedDevice) || device.equals(connectedDevice)) {
          connected = device;
        } else {
          hidDeviceProfile.disconnect(device);
        }
      }

      // If there is nothing going on, and we want to connect, then do it.
      if (hidDeviceProfile
              .getDevicesMatchingConnectionStates(
                  new int[] {
                    BluetoothProfile.STATE_CONNECTED,
                    BluetoothProfile.STATE_CONNECTING,
                    BluetoothProfile.STATE_DISCONNECTING
                  })
              .isEmpty()
          && requestedDevice != null) {
        hidDeviceProfile.connect(requestedDevice);
      }

      if (connectedDevice == null && connected != null) {
        connectedDevice = connected;
        requestedDevice = null;
      } else if (connectedDevice != null && connected == null) {
        connectedDevice = null;
      }
      hidDeviceApp.setHostDevice(connectedDevice);
    }
  }
}
