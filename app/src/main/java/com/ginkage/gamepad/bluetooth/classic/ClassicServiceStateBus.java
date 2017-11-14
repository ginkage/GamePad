package com.ginkage.gamepad.bluetooth.classic;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.MainThread;
import android.util.Log;
import com.google.android.clockwork.bluetooth.InputHost;
import com.ginkage.gamepad.bluetooth.ServiceStateBus;
import com.ginkage.gamepad.bluetooth.ServiceStateListener;
import javax.annotation.Nullable;

/** Helper class to register for the profile proxy state changes. */
public class ClassicServiceStateBus implements ServiceStateBus {
  private static final String TAG = "ServiceStateBus";
  private static final int PROFILE_ID = InputHost.INPUT_HOST;

  private final Context context;
  private final BluetoothAdapter bluetoothAdapter;

  @Nullable private ServiceStateListener serviceStateListener;
  @Nullable private BluetoothProfile service;

  /**
   * @param context Context that is required to establish the service connection.
   * @param bluetoothAdapter Bluetooth adapter to connect with.
   */
  public ClassicServiceStateBus(Context context, BluetoothAdapter bluetoothAdapter) {
    this.context = context.getApplicationContext();
    this.bluetoothAdapter = checkNotNull(bluetoothAdapter);
  }

  @Override
  @MainThread
  public void register(ServiceStateListener listener) {
    serviceStateListener = listener;
    bluetoothAdapter.getProfileProxy(context, new ServiceListener(), PROFILE_ID);
  }

  @Override
  @MainThread
  public void unregister() {
    if (service != null) {
      try {
        bluetoothAdapter.closeProfileProxy(PROFILE_ID, service);
        service = null;
      } catch (Throwable t) {
        Log.w(TAG, "Error cleaning up proxy", t);
      }
    }
    serviceStateListener = null;
  }

  private final class ServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    @MainThread
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      if (serviceStateListener != null) {
        service = proxy;
        serviceStateListener.onServiceStateChanged(service);
      } else {
        bluetoothAdapter.closeProfileProxy(PROFILE_ID, proxy);
      }
    }

    @Override
    @MainThread
    public void onServiceDisconnected(int profile) {
      service = null;
      if (serviceStateListener != null) {
        serviceStateListener.onServiceStateChanged(null);
      }
    }
  }
}
