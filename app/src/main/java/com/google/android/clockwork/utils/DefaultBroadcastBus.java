package com.google.android.clockwork.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.MainThread;
import android.util.Log;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link BroadcastBus}.
 *
 * <p>This implementation supports multiple registrations, with the caveat that each listener may
 * only be registered for a single intent filter.
 */
public final class DefaultBroadcastBus implements BroadcastBus {
    private static final String TAG = "BroadcastBus";
    private final Context mContext;
    private final Map<BroadcastListener, BroadcastReceiver> mRegistrations = new HashMap<>();

    public DefaultBroadcastBus(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    @MainThread
    public void register(final BroadcastListener listener, IntentFilter intentFilter) {
        checkNotNull(listener);
        BroadcastReceiver receiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        BroadcastReceiver registeredReceiver = mRegistrations.get(listener);
                        // These are defensive checks to double-check that unregistered listeners
                        // never get called back.
                        if (registeredReceiver == null) {
                            Log.e(
                                    TAG,
                                    "Received callback for unregistered listener "
                                            + listener
                                            + " with intent "
                                            + intent);
                        } else if (registeredReceiver != this) {
                            Log.e(
                                    TAG,
                                    "Received callback on wrong receiver for listener "
                                            + listener
                                            + " with intent "
                                            + intent);
                        } else {
                            listener.onReceive(intent);
                        }
                    }
                };
        if (mRegistrations.containsKey(listener)) {
            throw new IllegalStateException("Listener already registered");
        }
        mRegistrations.put(listener, receiver);
        Intent intent = mContext.registerReceiver(receiver, intentFilter);
        if (intent != null) {
            listener.onReceive(intent);
        }
    }

    @Override
    @MainThread
    public void unregister(BroadcastListener listener) {
        BroadcastReceiver broadcastReceiver = mRegistrations.get(checkNotNull(listener));
        Preconditions.checkArgument(
                broadcastReceiver != null, "Trying to unregister a listener that was not registered.");
        mContext.unregisterReceiver(broadcastReceiver);
        mRegistrations.remove(listener);
    }
}
