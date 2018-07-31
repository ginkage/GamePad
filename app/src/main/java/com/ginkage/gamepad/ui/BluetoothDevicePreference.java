package com.ginkage.gamepad.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.text.TextUtils;
import android.util.Log;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.HidDataSender;
import com.ginkage.gamepad.bluetooth.HidDeviceProfile;

/** Preference class representing a single bluetooth device. */
class BluetoothDevicePreference extends DialogPreference {
    private static final String TAG = "BluetoothDevicePref";

    private final BluetoothDevice device;
    private final HidDeviceProfile hidDeviceProfile;
    private final HidDataSender hidDataSender;

    private int connectionState;

    BluetoothDevicePreference(
            Context context,
            final BluetoothDevice device,
            final HidDeviceProfile hidDeviceProfile) {
        super(context);
        this.device = checkNotNull(device);
        this.hidDeviceProfile = checkNotNull(hidDeviceProfile);
        this.hidDataSender = HidDataSender.getInstance(context);

        setKey(this.device.getAddress());
        setIcon(R.drawable.ic_cc_settings_bluetooth);
        setNegativeButtonText(R.string.cancel);

        updateName();
        updateBondState();
        updateClass();
    }

    /** Present when the device is available */
    @Override
    protected void onClick() {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            // No need to try to connect to devices when we don't support any profiles
            // on the target device.
            if (!hidDeviceProfile.isProfileSupported(device)) {
                return;
            }

            connectionState = hidDeviceProfile.getConnectionState(device);
            setPositiveButtonText(
                    connectionState == BluetoothProfile.STATE_CONNECTED
                                    || connectionState == BluetoothProfile.STATE_CONNECTING
                            ? R.string.pref_bluetooth_disconnect
                            : R.string.pref_bluetooth_connect);

            setNegativeButtonText(
                    connectionState == BluetoothProfile.STATE_CONNECTED
                            ? R.string.pref_bluetooth_select
                            : R.string.pref_bluetooth_forget);

            super.onClick();
      } else {
            // Discovery may be in progress so cancel discovery before attempting to bond.
            stopDiscovery();
            device.createBond();
      }
    }

    void onDialogClosed(boolean positiveResult) {
        connectionState = hidDeviceProfile.getConnectionState(device);
        if (positiveResult) {
            if (connectionState == BluetoothProfile.STATE_CONNECTED
                    || connectionState == BluetoothProfile.STATE_CONNECTING) {
                hidDataSender.requestConnect(null);
            } else {
                hidDataSender.requestConnect(device);
            }
        } else {
            if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                hidDataSender.requestConnect(device);
            } else {
                requestUnpair();
            }
        }
    }

    /** Request to unpair and remove the bond */
    private void requestUnpair() {
        final int state = device.getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            BluetoothUtils.cancelBondProcess(device);
            return;
        }

        if (state != BluetoothDevice.BOND_NONE) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.pref_bluetooth_unpair)
                    .setMessage(device.getName())
                    .setPositiveButton(
                            R.string.generic_yes,
                            (dialog, which) -> {
                                if (!BluetoothUtils.removeBond(device)) {
                                    Log.w(TAG, "Unpair request rejected straight away.");
                                }
                            })
                    .setNegativeButton(R.string.generic_cancel, (dialog, which) -> {})
                    .show();
        }
    }

    private void stopDiscovery() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
      }
    }

    void updateName() {
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = device.getAddress();
        }

        setTitle(name);
        setDialogTitle(name);
        notifyChanged();
    }

    /** Re-examine the device and update the bond state. */
    void updateBondState() {
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                setSummary(null);
                break;
            case BluetoothDevice.BOND_BONDING:
                setSummary(R.string.pref_bluetooth_pairing);
                break;
            case BluetoothDevice.BOND_NONE:
                setSummary(R.string.pref_bluetooth_available);
                break;
            default: // fall out
        }
        notifyChanged();
    }

    private void updateClass() {
        if (device.getBluetoothClass() == null) {
            return;
        }

        switch (device.getBluetoothClass().getDeviceClass()) {
            case BluetoothClass.Device.PHONE_CELLULAR:
            case BluetoothClass.Device.PHONE_SMART:
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                setIcon(R.drawable.ic_phone);
                break;

            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                setIcon(R.drawable.ic_settings_headset);
                break;

            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                setIcon(R.drawable.ic_settings_device_only);
                break;

            case BluetoothClass.Device.WEARABLE_GLASSES:
                setIcon(R.drawable.ic_glass_device_white);
                break;
        default: // fall out
      }
        notifyChanged();
    }

    /**
     * Update the preference summary with the profile connection state
     *
     * <p>However, if no profiles are supported from the target device we indicate that this target
     * device is unavailable.
     */
    void updateProfileConnectionState() {
        connectionState = hidDeviceProfile.getConnectionState(device);
        if (!hidDeviceProfile.isProfileSupported(device)) {
            setEnabled(false);
            setSummary(R.string.pref_bluetooth_unavailable);
        } else {
            switch (connectionState) {
                case BluetoothProfile.STATE_CONNECTED:
                    setEnabled(true);
                    setSummary(R.string.pref_bluetooth_connected);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    setEnabled(false);
                    setSummary(R.string.pref_bluetooth_connecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    setEnabled(false);
                    setSummary(R.string.pref_bluetooth_disconnecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    setEnabled(true);
                    setSummary(R.string.pref_bluetooth_disconnected);
                    break;
                default: // fall out
            }
        }
        notifyChanged();
    }
}
