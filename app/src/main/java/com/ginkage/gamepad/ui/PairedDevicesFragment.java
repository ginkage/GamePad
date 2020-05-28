package com.ginkage.gamepad.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.util.Log;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.HidDataSender;
import com.ginkage.gamepad.bluetooth.HidDataSender.ProfileListener;
import com.ginkage.gamepad.bluetooth.HidDeviceProfile;
import java.util.ArrayList;
import java.util.List;

/** Paired Bluetooth devices list. */
public class PairedDevicesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "BluetoothSettings";

    private static final int PREFERENCE_ORDER_NORMAL = 100;

    private BluetoothAdapter bluetoothAdapter;
    private HidDeviceProfile hidDeviceProfile;
    private HidDataSender hidDataSender;

    private final List<Preference> bondedDevices = new ArrayList<>();

    private boolean scanReceiverRegistered;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_paired_devices, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hidDataSender = HidDataSender.getInstance();
        hidDeviceProfile = hidDataSender.register(getContext(), profileListener);
        registerStateReceiver();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment fragment = PairedDeviceDialog.newInstance(preference);
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager(), PairedDeviceDialog.TAG);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBluetoothSwitchAndDevices();
    }

    @Override
    public void onPause() {
        unregisterScanReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterStateReceiver();
        hidDataSender.unregister(getContext(), profileListener);
        getContext().stopService(new Intent(getContext(), NotificationService.class));
        super.onDestroy();
    }

    protected void updateBluetoothSwitchAndDevices() {
        switch (bluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                unregisterScanReceiver();
                clearBondedDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_TURNING_OFF:
                clearBondedDevices();
                startActivity(new Intent(getActivity(), BluetoothStateActivity.class));
                break;
            case BluetoothAdapter.STATE_ON:
                registerScanReceiver();
                updateBondedDevices();
                break;
            default: // fall out
        }
    }

    protected void updateBondedDevices() {
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            updatePreferenceBondState(device);
        }
    }

    /** Examine the bond state of the device and update preference if necessary. */
    @VisibleForTesting
    void updatePreferenceBondState(final BluetoothDevice device) {
        final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
        pref.updateBondState();
        pref.updateProfileConnectionState();
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                pref.setEnabled(true);
                pref.setOrder(PREFERENCE_ORDER_NORMAL);
                bondedDevices.add(pref);
                getPreferenceScreen().addPreference(pref);
                break;
            case BluetoothDevice.BOND_NONE:
                pref.setEnabled(false);
                bondedDevices.remove(pref);
                getPreferenceScreen().removePreference(pref);
                break;
            case BluetoothDevice.BOND_BONDING:
                pref.setEnabled(false);
                break;
            default: // fall out
        }
    }

    protected void clearBondedDevices() {
        for (Preference p : bondedDevices) {
            getPreferenceScreen().removePreference(p);
        }
        bondedDevices.clear();
    }

    private void registerScanReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(bluetoothScanReceiver, intentFilter);
        scanReceiverRegistered = true;

        BluetoothUtils.setScanMode(
                bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
    }

    private void unregisterScanReceiver() {
        if (scanReceiverRegistered) {
            getContext().unregisterReceiver(bluetoothScanReceiver);
            scanReceiverRegistered = false;
            BluetoothUtils.setScanMode(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 0);
        }
    }

    private void registerStateReceiver() {
        getContext()
                .registerReceiver(
                        bluetoothStateReceiver,
                        new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private void unregisterStateReceiver() {
        getContext().unregisterReceiver(bluetoothStateReceiver);
    }

    private final ProfileListener profileListener =
            new ProfileListener() {
                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothHidDevice proxy) {
                    if (proxy != null) {
                        for (final BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                            final BluetoothDevicePreference pref = findDevicePreference(device);
                            if (pref != null) {
                                pref.updateProfileConnectionState();
                            }
                        }
                    }
                }

                @Override
                @MainThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    updatePreferenceBondState(device);

                    Context context = getContext();
                    if (state != BluetoothProfile.STATE_DISCONNECTED) {
                        Intent intent = NotificationService.buildIntent(device.getName(), state);
                        intent.setClass(context, NotificationService.class);
                        context.startService(intent);
                    }

                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        getActivity()
                                .startActivity(new Intent(getActivity(), GamepadActivity.class));
                    }
                }

                @Override
                @MainThread
                public void onAppStatusChanged(boolean registered) {
                    if (!registered) {
                        getActivity().finish();
                    }
                }
            };

    /** Handles bluetooth scan responses and other indicators. */
    private final BroadcastReceiver bluetoothScanReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getContext() == null) {
                        Log.w(TAG, "BluetoothScanReceiver got intent with no context");
                        return;
                    }
                    final BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    final String action = intent.getAction();
                    switch (action == null ? "" : action) {
                        case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                            updatePreferenceBondState(device);
                            break;
                        case BluetoothDevice.ACTION_NAME_CHANGED:
                            BluetoothDevicePreference pref = findDevicePreference(device);
                            if (pref != null) {
                                pref.updateName();
                            }
                            break;
                        default: // fall out
                    }
                }
            };

    /** Receiver to listen for changes in the bluetooth adapter state. */
    private final BroadcastReceiver bluetoothStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getContext() == null) {
                        Log.w(TAG, "BluetoothStateReceiver got intent with no context");
                        return;
                    }
                    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                        updateBluetoothSwitchAndDevices();
                    }
                }
            };

    /**
     * Looks for a preference in the preference group.
     *
     * <p>Returns null if no preference found.
     */
    private BluetoothDevicePreference findDevicePreference(final BluetoothDevice device) {
        return (BluetoothDevicePreference) findPreference(device.getAddress());
    }

    /**
     * Looks for a preference in the preference group.
     *
     * <p>Allocates a new preference if none found.
     */
    private BluetoothDevicePreference findOrAllocateDevicePreference(final BluetoothDevice device) {
        BluetoothDevicePreference pref = findDevicePreference(device);
        if (pref == null) {
            pref = new BluetoothDevicePreference(getContext(), device, hidDeviceProfile);
        }
        return pref;
    }
}
