package com.ginkage.gamepad.bluetooth;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.ArraySet;
import android.util.Log;
import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import com.google.android.clockwork.utils.BroadcastBus;
import com.google.android.clockwork.utils.DefaultBroadcastBus;
import com.ginkage.gamepad.bluetooth.classic.ClassicHidDeviceApp;
import com.ginkage.gamepad.bluetooth.classic.ClassicHidHostProfile;
import com.ginkage.gamepad.bluetooth.classic.ClassicServiceStateBus;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Central point for enabling the HID SDP record and sending all data. */
public class HidDataSender implements GamepadDataSender {

  private static final String TAG = "HidDataSender";

  /** Compound interface that listens to both device and service state changes. */
  public interface ProfileListener extends DeviceStateListener, ServiceStateListener {}

  @VisibleForTesting
  public static final LazyContextSupplier<HidDataSender> INSTANCE =
      new LazyContextSupplier<>(HidDataSender::createClassicInstance, "HidDataSender");

  private static HidDataSender createClassicInstance(Context appContext) {
    ClassicHidDeviceApp hidDeviceApp = new ClassicHidDeviceApp();
    return new HidDataSender(
        new DefaultBroadcastBus(appContext),
        hidDeviceApp,
        new ClassicHidHostProfile(
            new ClassicServiceStateBus(appContext, BluetoothAdapter.getDefaultAdapter())),
        hidDeviceApp);
  }

  private final BroadcastBus.BroadcastListener batteryListener = this::onBatteryChanged;

  private final BroadcastBus broadcastBus;
  private final HidDeviceApp hidDeviceApp;
  private final HidHostProfile hidHostProfile;
  private final DeviceStateBus deviceStateBus;

  private final Object lock = new Object();

  @GuardedBy("lock")
  private final Set<ProfileListener> listeners = new ArraySet<>();

  @GuardedBy("lock")
  @Nullable
  private BluetoothDevice connectedDevice;

  @GuardedBy("lock")
  @Nullable
  private BluetoothDevice waitingForDevice;

  /**
   * @param hidDeviceApp HID Device App interface.
   * @param hidHostProfile Interface to manage paired HID Host devices.
   * @param deviceStateBus Interface to register for device connection state changes.
   */
  @VisibleForTesting
  HidDataSender(
      BroadcastBus broadcastBus,
      HidDeviceApp hidDeviceApp,
      HidHostProfile hidHostProfile,
      DeviceStateBus deviceStateBus) {
    this.broadcastBus = checkNotNull(broadcastBus);
    this.hidDeviceApp = checkNotNull(hidDeviceApp);
    this.hidHostProfile = checkNotNull(hidHostProfile);
    this.deviceStateBus = checkNotNull(deviceStateBus);
  }

  /**
   * Retrieve the singleton instance of the class.
   *
   * @param context Ensures that only Activities or Services access the singleton.
   * @return Singleton instance.
   */
  public static HidDataSender getInstance(Context context) {
    return INSTANCE.get(context);
  }

  /**
   * Ensure that the HID Device SDP record is registered and start listening for the profile proxy
   * and HID Host connection state changes.
   *
   * @param listener Callback that will receive the profile events.
   * @return Interface for managing the paired HID Host devices.
   */
  @MainThread
  public HidHostProfile register(ProfileListener listener) {
    synchronized (lock) {
      if (!listeners.add(listener)) {
        // This user is already registered
        return hidHostProfile;
      }
      if (listeners.size() > 1) {
        // There are already some users
        return hidHostProfile;
      }

      hidHostProfile.register(this::onServiceStateChanged);
      deviceStateBus.register(this::onDeviceStateChanged);
      broadcastBus.register(batteryListener, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    return hidHostProfile;
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

      broadcastBus.unregister(batteryListener);
      deviceStateBus.unregister();

      for (BluetoothDevice device : hidHostProfile.getConnectedDevices()) {
        hidHostProfile.disconnect(device);
      }

      hidDeviceApp.setDevice(null);
      hidDeviceApp.unregisterApp();

      hidHostProfile.unregister();

      connectedDevice = null;
      waitingForDevice = null;
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
      waitingForDevice = device;
      connectedDevice = null;

      updateDeviceList();

      if (device != null && device.equals(connectedDevice)) {
        for (ProfileListener listener : listeners) {
          listener.onDeviceStateChanged(device, BluetoothProfile.STATE_CONNECTED);
        }
      }
    }
  }

  @Override
  @MainThread
  public void sendGamepad(GamepadState state) {
    synchronized (lock) {
      if (connectedDevice != null) {
        hidDeviceApp.sendGamepad(state);
      }
    }
  }

  @MainThread
  private void onServiceStateChanged(BluetoothProfile proxy) {
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

  @MainThread
  private void onDeviceStateChanged(BluetoothDevice device, int state) {
    synchronized (lock) {
      if (state == BluetoothProfile.STATE_CONNECTED) {
        // A new connection was established. If we weren't expecting that, it must be an
        // incoming one. In that case, we shouldn't try to disconnect from it.
        waitingForDevice = device;
      }
      updateDeviceList();
      for (ProfileListener listener : listeners) {
        listener.onDeviceStateChanged(device, state);
      }
    }
  }

  @MainThread
  private void updateDeviceList() {
    synchronized (lock) {
      BluetoothDevice connected = null;

      // If we are connected to some device, but want to connect to another (or disconnect
      // completely), then we should disconnect all other devices first.
      for (BluetoothDevice device : hidHostProfile.getConnectedDevices()) {
        if (device.equals(waitingForDevice) || device.equals(connectedDevice)) {
          connected = device;
        } else {
          hidHostProfile.disconnect(device);
        }
      }

      // If there is nothing going on, and we want to connect, then do it.
      if (hidHostProfile
              .getDevicesMatchingConnectionStates(
                  new int[] {
                    BluetoothProfile.STATE_CONNECTED,
                    BluetoothProfile.STATE_CONNECTING,
                    BluetoothProfile.STATE_DISCONNECTING
                  })
              .isEmpty()
          && waitingForDevice != null) {
        hidHostProfile.connect(waitingForDevice);
      }

      if (connectedDevice == null && connected != null) {
        connectedDevice = connected;
        waitingForDevice = null;
      } else if (connectedDevice != null && connected == null) {
        connectedDevice = null;
      }
      hidDeviceApp.setDevice(connectedDevice);
    }
  }

  @MainThread
  private void onBatteryChanged(Intent intent) {
    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    if (level >= 0 && scale > 0) {
      float batteryLevel = (float) level / (float) scale;
      hidDeviceApp.sendBatteryLevel(batteryLevel);
    } else {
      Log.e(TAG, "Bad battery level data received: level=" + level + ", scale=" + scale);
    }
  }
}
