package com.ginkage.gamepad.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.text.TextUtils;

import com.google.android.clockwork.bluetooth.BluetoothUtils;
import com.ginkage.gamepad.R;
import com.ginkage.gamepad.bluetooth.HidDataSender;
import com.ginkage.gamepad.bluetooth.HidHostProfile;

import javax.annotation.Nonnull;

/**
 * Preference class representing a single bluetooth device.
 *
 * <p>This class is largely based on the one from
 * https://cs.corp.google.com/android/vendor/google_clockwork/packages/Settings/src/com/google/android/clockwork/settings/connectivity/bluetooth/BluetoothDevicePreference.java
 * with a few modifications.
 */
class BluetoothDevicePreference extends DialogPreference {
    private static final String TAG = "BluetoothDevicePref";

    @Nonnull private final BluetoothDevice device;
    @Nonnull private final HidHostProfile hidHostProfile;
    @Nonnull private final HidDataSender hidDataSender;

    private int whichButtonClicked;
    private int connectionState;

    BluetoothDevicePreference(
            Context context,
            @Nonnull final BluetoothDevice device,
            @Nonnull final HidHostProfile hidHostProfile) {
        super(context);
        this.device = device;
        this.hidHostProfile = hidHostProfile;
        hidDataSender = HidDataSender.getInstance(context);

        setKey(this.device.getAddress());
        setIcon(R.drawable.ic_cc_settings_bluetooth);
        setNegativeButtonText(R.string.cancel);

        updateName();
        updateBondState();
        updateClass();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // No need to try to connect to devices when we don't support any profiles
        // on the target device.
        if (!hidHostProfile.isProfileSupported(device)) {
            builder.setPositiveButton(R.string.pref_bluetooth_unavailable, this);
        } else {
            connectionState = hidHostProfile.getConnectionState(device);
            builder.setPositiveButton(
                    connectionState == BluetoothProfile.STATE_CONNECTED
                                    || connectionState == BluetoothProfile.STATE_CONNECTING
                            ? R.string.pref_bluetooth_disconnect
                            : R.string.pref_bluetooth_connect,
                    this);
        }

        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
            builder.setNeutralButton(R.string.pref_bluetooth_select, this);
        } else if (device.getBluetoothClass() != null
                && device.getBluetoothClass().getMajorDeviceClass()
                        != BluetoothClass.Device.Major.PHONE) {
            builder.setNeutralButton(R.string.pref_bluetooth_forget, this);
        }
    }

    /** Present when the device is available */
    @Override
    protected void onClick() {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            super.onClick();
        } else {
            // Discovery may be in progress so cancel discovery before attempting to bond.
            stopDiscovery();
            device.createBond();
        }
    }

    /** Present when the device requires a dialog */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        whichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (whichButtonClicked) {
            case DialogInterface.BUTTON_POSITIVE:
                if (connectionState == BluetoothProfile.STATE_CONNECTED
                        || connectionState == BluetoothProfile.STATE_CONNECTING) {
                    hidDataSender.requestConnect(null);
                } else {
                    hidDataSender.requestConnect(device);
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    hidDataSender.requestConnect(device);
                } else {
                    requestUnpair();
                }
                break;
                // do nothing, dismiss
            case DialogInterface.BUTTON_NEGATIVE:
            default:
                break;
        }
    }

    /** Request to unpair and remove the bond */
    private void requestUnpair() {
        final int state = device.getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            BluetoothUtils.cancelBondProcess(device);
            return;
        }
/*
        if (state != BluetoothDevice.BOND_NONE) {
            AcceptDenyDialog diag = new AcceptDenyDialog(getContext());
            diag.setTitle(R.string.pref_bluetooth_unpair);
            diag.setMessage(device.getName());
            diag.setPositiveButton(
                    (dialog, which) -> {
                        if (!BluetoothUtils.removeBond(device)) {
                            Log.w(TAG, "Unpair request rejected straight away.");
                        }
                    });
            diag.setNegativeButton((dialog, which) -> {});
            diag.show();
        }*/
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
        connectionState = hidHostProfile.getConnectionState(device);
        if (!hidHostProfile.isProfileSupported(device)) {
            // If the device is a phone it is likely the primary paired
            // phone so skip indicating it is unavailable.
            if (device.getBluetoothClass() != null
                    && device.getBluetoothClass().getMajorDeviceClass()
                            != BluetoothClass.Device.Major.PHONE) {
                setSummary(R.string.pref_bluetooth_unavailable);
            } else {
                setSummary(null);
            }
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
