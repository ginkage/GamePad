package com.ginkage.gamepad.bluetooth.classic;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.MainThread;
import com.google.android.clockwork.bluetooth.BluetoothUtils;
import com.google.android.clockwork.bluetooth.InputHost;
import com.ginkage.gamepad.bluetooth.HidHostProfile;
import com.ginkage.gamepad.bluetooth.ServiceStateBus;
import com.ginkage.gamepad.bluetooth.ServiceStateListener;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Wrapper for BluetoothInputHost profile that manages paired HID Host devices. */
public class ClassicHidHostProfile implements HidHostProfile {

  private static final String TAG = "HidHostProfile";

  private final ServiceStateBus serviceStateBus;
  @Nullable private ServiceStateListener serviceStateListener;
  @Nullable private InputHost service;

  /** @param serviceStateBus Interface to listen for the profile proxy state. */
  public ClassicHidHostProfile(ServiceStateBus serviceStateBus) {
    this.serviceStateBus = checkNotNull(serviceStateBus);
  }

  @Override
  public boolean isProfileSupported(BluetoothDevice device) {
    // If a device reports itself as a HID Device, then it isn't a HID Host.
    return !BluetoothUtils.isInputDevice(device);
  }

  @Override
  public int getConnectionState(BluetoothDevice device) {
    if (service == null) {
      return BluetoothProfile.STATE_DISCONNECTED;
    }
    return service.getConnectionState(device);
  }

  @Override
  @MainThread
  public void register(ServiceStateListener listener) {
    serviceStateListener = checkNotNull(listener);
    serviceStateBus.register(this::onServiceStateChanged);
  }

  @Override
  @MainThread
  public void unregister() {
    serviceStateListener = null;
    serviceStateBus.unregister();
  }

  @Override
  @MainThread
  public void connect(BluetoothDevice device) {
    if (service != null && isProfileSupported(device)) {
      service.connect(device);
    }
  }

  @Override
  @MainThread
  public void disconnect(BluetoothDevice device) {
    if (service != null && isProfileSupported(device)) {
      service.disconnect(device);
    }
  }

  @Override
  @MainThread
  public List<BluetoothDevice> getConnectedDevices() {
    if (service == null) {
      return new ArrayList<>();
    }
    return service.getConnectedDevices();
  }

  @Override
  @MainThread
  public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
    if (service == null) {
      return new ArrayList<>();
    }
    return service.getDevicesMatchingConnectionStates(states);
  }

  @MainThread
  private void onServiceStateChanged(BluetoothProfile proxy) {
    service = new InputHost(proxy);
    if (serviceStateListener != null) {
      serviceStateListener.onServiceStateChanged(proxy);
    }
  }
}
