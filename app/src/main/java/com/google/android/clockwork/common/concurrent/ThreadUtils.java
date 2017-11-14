package com.google.android.clockwork.common.concurrent;

import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import com.google.android.clockwork.utils.BuildUtils;

public final class ThreadUtils {

    /** Setting volatile allows consistent reads after modifications. No read-modify-write calls. */
    private static volatile boolean sThreadChecksEnabled = true;

    // Cannot instantiate.
    private ThreadUtils() {}

    /** Returns true if this method is invoked on the main thread. */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Ensure that this method is not called from the main thread, otherwise an exception will be
     * thrown.
     */
    @WorkerThread
    public static void checkNotMainThread() {
        if (sThreadChecksEnabled
                && !BuildUtils.IS_USER_BUILD
                && (Looper.myLooper() == Looper.getMainLooper())) {
            throw new IllegalStateException("This method cannot be called from the UI thread.");
        }
    }

    /**
     * Ensure that this method is called from the main thread, otherwise an exception will be thrown.
     */
    @MainThread
    public static void checkOnMainThread() {
        if (sThreadChecksEnabled
                && !BuildUtils.IS_USER_BUILD
                && (Looper.myLooper() != Looper.getMainLooper())) {
            throw new IllegalStateException("This method should be called from the UI thread.");
        }
    }

    /**
     * Like {@link #checkOnMainThread()}, except that it will always throw an exception if called off
     * the main thread, regardless of build type or other flags.
     */
    @MainThread
    public static void enforceOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("This method must be called from the UI thread.");
        }
    }

    /**
     * Prefer {@code DisabledThreadChecksRule} to ensure that your tests restore thread checking when
     * they're done.
     */
    @VisibleForTesting
    public static void setThreadChecksEnabled(boolean enabled) {
        sThreadChecksEnabled = enabled;
    }

    @VisibleForTesting
    static String getName(Thread t, CwNamed n) {
        return t.getName() + "-" + n.getName();
    }
}
