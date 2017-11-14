package com.google.android.clockwork.utils;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.MainThread;

/** Helper interface that hides implementation of broadcast receiver registration. */
public interface BroadcastBus {
    interface BroadcastListener {
        @MainThread
        void onReceive(Intent intent);
    }

    /**
     * Registers a broadcast listener. Broadcast of Intents matching the {@code intentFilter} will
     * trigger an {@code onReceive} callback on the {@code listener}.
     */
    @MainThread
    void register(BroadcastListener listener, IntentFilter intentFilter);

    /** Unregisters a broadcast {@code listener}. */
    @MainThread
    void unregister(BroadcastListener listener);
}
