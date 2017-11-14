package com.ginkage.gamepad.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import com.ginkage.gamepad.R;

/** Show a spinner animation while the Bluetooth is turning on or off. */
public class BluetoothStateActivity extends Activity {

    private final BroadcastReceiver bluetoothStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();

                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        checkState(
                                intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_state);
        registerReceiver(
                bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        checkState(BluetoothAdapter.getDefaultAdapter().getState());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bluetoothStateReceiver);
        super.onDestroy();
    }

    private void checkState(int state) {
        if (state != BluetoothAdapter.STATE_TURNING_ON
                && state != BluetoothAdapter.STATE_TURNING_OFF) {
            finish();
        } else {
            ((TextView) findViewById(R.id.title))
                    .setText(
                            getString(
                                    state == BluetoothAdapter.STATE_TURNING_ON
                                            ? R.string.pref_bluetooth_turningOn
                                            : R.string.pref_bluetooth_turningOff));
        }
    }
}
