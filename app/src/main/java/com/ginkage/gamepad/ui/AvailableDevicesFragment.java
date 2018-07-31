package com.ginkage.gamepad.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.HidDataSender;
import com.ginkage.gamepad.bluetooth.HidDataSender.ProfileListener;
import com.ginkage.gamepad.bluetooth.HidDeviceProfile;
import com.google.common.base.Preconditions;

/**
 * Bluetooth device discovery scan for available devices.
 *
 * <p>When this fragment is created a scan will automatically be initiated. There is also a
 * preference to initiate a BT re-scan from this fragment once the initial scan has terminated due
 * to limited discovery time.
 *
 * <p>Once a scan has initiated, each BT device discovered is bound to a preference indexed by the
 * BT mac address. These preferences are then shown in an available preference category presented to
 * the user.
 *
 * <p>Users click on a preference in a the available preference category to initiate a bonding
 * sequence with the device. Should the bond be successful, the device will disappear from the
 * available preference category and appear in the previous fragment bonded preference list. If the
 * bond fails the device will remain in the available preference category.
 */
public class AvailableDevicesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "BluetoothScan";

    private static final String KEY_PREF_BLUETOOTH_SCAN = "pref_bluetoothScan";
    private static final String KEY_PREF_BLUETOOTH_AVAILABLE = "pref_bluetoothAvailable";

    private static final int PERMISSION_REQUEST = 1;

    private BluetoothAdapter bluetoothAdapter;
    private HidDeviceProfile hidDeviceProfile;
    private HidDataSender hidDataSender;

    private BluetoothStateReceiver stateReceiver;
    private BluetoothScanReceiver scanReceiver;

    private Preference initiateScanDevices;
    private PreferenceGroup availableDevices;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_available_devices, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hidDataSender = HidDataSender.getInstance(context);
        hidDeviceProfile = hidDataSender.register(profileListener);

        initiateScanDevices = findPreference(KEY_PREF_BLUETOOTH_SCAN);
        availableDevices = (PreferenceGroup) findPreference(KEY_PREF_BLUETOOTH_AVAILABLE);
        availableDevices.setLayoutResource(R.layout.preference_group_no_title);

        initScanDevices(initiateScanDevices);
        initAvailableDevices();
        registerStateReceiver();

        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            }
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBluetoothState();
    }

    @Override
    public void onPause() {
        stopDiscovery();
        unregisterScanReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterStateReceiver();
        hidDataSender.unregister(profileListener);
        super.onDestroy();
    }

    protected void initScanDevices(Preference pref) {
        if (bluetoothAdapter.isDiscovering()) {
            pref.setEnabled(false);
        }

        pref.setOnPreferenceClickListener(
                (p) -> {
                    clearAvailableDevices();
                    startDiscovery();
                    return true;
                });
    }

    protected void initAvailableDevices() {
        clearAvailableDevices();
    }

    protected BluetoothDevicePreference addAvailableDevice(BluetoothDevice device) {
        final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            availableDevices.addPreference(pref);
            pref.setEnabled(true);
        }
        return pref;
    }

    /** Re-examine the device and update if necessary. */
    protected void updateAvailableDevice(BluetoothDevice device) {
        final BluetoothDevicePreference pref = findDevicePreference(device);
        if (pref != null) {
            pref.updateBondState();
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDED:
                    pref.setEnabled(false);
                    availableDevices.removePreference(pref);
                    break;
                case BluetoothDevice.BOND_BONDING:
                    pref.setEnabled(false);
                    break;
                case BluetoothDevice.BOND_NONE:
                    pref.setEnabled(true);
                    addAvailableDevice(device);
                    break;
                default: // fall out
            }
        }
    }

    protected void clearAvailableDevices() {
        availableDevices.removeAll();
    }

    /** Handles changes in the bluetooth adapter state. */
    protected void checkBluetoothState() {
        switch (bluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                initiateScanDevices.setTitle(R.string.generic_disabled);
                initiateScanDevices.setEnabled(false);
                clearAvailableDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_TURNING_OFF:
                initiateScanDevices.setEnabled(false);
                clearAvailableDevices();
                startActivity(new Intent(getActivity(), BluetoothStateActivity.class));
                break;
            case BluetoothAdapter.STATE_ON:
                initiateScanDevices.setTitle(R.string.pref_bluetoothScan);
                initiateScanDevices.setEnabled(true);
                registerScanReceiver();
                startDiscovery();
                break;
            default: // fall out
        }
    }

    private void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        initiateScanDevices.setEnabled(false);
    }

    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void registerScanReceiver() {
        if (scanReceiver != null) {
            return;
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(scanReceiver = new BluetoothScanReceiver(), intentFilter);

        BluetoothUtils.setScanMode(
                bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
    }

    private void unregisterScanReceiver() {
        if (scanReceiver != null) {
            getContext().unregisterReceiver(scanReceiver);
            scanReceiver = null;
            BluetoothUtils.setScanMode(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 0);
        }
    }

    private void registerStateReceiver() {
        Preconditions.checkArgument(stateReceiver == null);
        final IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(stateReceiver = new BluetoothStateReceiver(), intentFilter);
    }

    private void unregisterStateReceiver() {
        if (stateReceiver != null) {
            getContext().unregisterReceiver(stateReceiver);
            stateReceiver = null;
        }
    }

    /** Handles bluetooth scan responses and other indicators. */
    protected class BluetoothScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getContext() == null) {
                Log.w(TAG, "BluetoothScanReceiver context disappeared");
                return;
            }

            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (action == null ? "" : action) {
                case BluetoothDevice.ACTION_FOUND:
                    if (hidDeviceProfile.isProfileSupported(device)) {
                        addAvailableDevice(device);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    initiateScanDevices.setEnabled(false);
                    initiateScanDevices.setTitle(R.string.pref_bluetoothScan_scanning);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    initiateScanDevices.setEnabled(true);
                    initiateScanDevices.setTitle(R.string.pref_bluetoothScan);
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    updateAvailableDevice(device);
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
    }

    /** Receiver to listen for changes in the bluetooth adapter state. */
    protected class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                checkBluetoothState();
            }
        }
    }

    private final ProfileListener profileListener =
            new ProfileListener() {
                @Override
                public void onServiceStateChanged(BluetoothHidDevice proxy) {}

                @Override
                public void onDeviceStateChanged(BluetoothDevice device, int state) {
                    final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
                    pref.updateProfileConnectionState();
                }

                @Override
                public void onAppUnregistered() {
                    getActivity().finish();
                }
            };

    /**
     * Looks for a preference in the preference group.
     *
     * <p>Returns null if no preference available.
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
