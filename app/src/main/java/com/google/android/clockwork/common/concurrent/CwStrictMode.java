
package com.google.android.clockwork.common.concurrent;

import static android.os.StrictMode.getThreadPolicy;
import static android.os.StrictMode.setThreadPolicy;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Handler;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.google.android.clockwork.utils.BuildUtils;

/**
 * Utils for catching startup-time regressions, and tracking operations on the UI thread to avoid
 * jank.
 */
@SuppressLint("StrictModeUsage")
public class CwStrictMode {

    private static final String MAIN_DEATH_TAG = "CwStrictMode.MainDeath";

    /** Code for marshmallow. This module is compiled with sdk 19, so we can't access it directly */
    private static final int VERSION_CODE_MNC = 23;

    /** {@code true} if {@link CwStrictMode} should be enabled. */
    public static final boolean ENABLED = !BuildUtils.IS_USER_BUILD;

    /**
     * {@code true} if {@link CwStrictMode} should penalize main thread violations with a crash (i.e.
     * death penalty).
     *
     * <p>On the main thread, the death penalty is only enabled on {@code Build.VERSION_CODES.M} and
     * beyond, as some platform methods like {@code Context.startActivity(Intent)} and {@code
     * PackageManager.resolveActivity(Intent, int)} violate strict mode on older platform builds.
     */
    public static final boolean MAIN_THREAD_ALLOWS_DEATH_PENALTY = false; /* b/27150264 ENABLED
            && (Build.VERSION.SDK_INT >= VERSION_CODE_MNC); */

    /** {@link Policy} that allows everything except the network calls and custom slow calls. */
    public static final Policy USER_POLICY =
            new Policy(
                    new ThreadPolicy.Builder()
                            .permitAll()
                            .detectNetwork()
                            .detectCustomSlowCalls()
                            .penaltyLog()
                            .build());

    /** {@link Policy} that allows everything including the network calls. */
    public static final Policy LAX_POLICY = new Policy(ThreadPolicy.LAX);

    /**
     * {@link CwStrictMode.Policy} that kills the app on network access, storage access, or slow
     * calls.
     */
    public static final Policy STRICT_POLICY =
            new Policy(new ThreadPolicy.Builder().detectAll().penaltyLog().build());

    private CwStrictMode() {}

    /** Call this early to enable strict mode in the application. */
    @MainThread
    public static void init() {
        if (ENABLED) {
            ThreadUtils.checkOnMainThread();
            setStrictModeForMainThread();
        }
    }

    /** Call this at the end of Application.onCreate to ensure bugs didn't disable the policy. */
    @MainThread
    public static void ensureStrictModeEnabled() {
        if (ENABLED) {
            ThreadUtils.checkOnMainThread();
            // See https://code.google.com/p/android/issues/detail?id=35298 for why we need to
            // enable strict mode after Application.onCreate has returned.
            new Handler()
                    .postAtFrontOfQueue(
                            new Runnable() {
                                @Override
                                public void run() {
                                    setStrictModeForMainThread();
                                }
                            });
        }
    }

    public static void setStrictModeForMainThread() {
        if (ENABLED) {
            setThreadPolicy(getMainThreadPolicy());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
    }

    public static void noteSlowCall(String name) {
        if (ENABLED) {
            StrictMode.noteSlowCall(name);
        }
    }

    /**
     * Used to temporarily change the strict mode thread policy to allow disk reads. The returned
     * {@link ThreadPolicy} should be re-instantiated using {@link #restoreStrictMode(ThreadPolicy)} .
     *
     * @return the current {@link ThreadPolicy} to be instantiated later if strict mode is enabled,
     *     {@code null} otherwise.
     */
    @Nullable
    public static ThreadPolicy allowDiskReads() {
        if (ENABLED) {
            return StrictMode.allowThreadDiskReads();
        } else {
            return null;
        }
    }

    /**
     * Used to temporarily change the strict mode thread policy to allow disk reads and writes. The
     * {@link ThreadPolicy} should be re-instantiated using {@link #restoreStrictMode(ThreadPolicy)} .
     *
     * @return the current {@link ThreadPolicy} to be instantiated later if strict mode is enabled,
     *     {@code null} otherwise.
     */
    @Nullable
    public static ThreadPolicy allowDiskWrites() {
        if (ENABLED) {
            return StrictMode.allowThreadDiskWrites();
        } else {
            return null;
        }
    }

    /**
     * Used to temporarily change the strict mode thread policy to allow slow calls. The {@link
     * ThreadPolicy} should be re-instantiated using {@link #restoreStrictMode(ThreadPolicy)} .
     *
     * @return the current {@link ThreadPolicy} to be instantiated later if strict mode is enabled,
     *     {@code null} otherwise.
     */
    @Nullable
    public static ThreadPolicy allowSlowCalls() {
        if (ENABLED) {
            ThreadPolicy oldPolicy = getThreadPolicy();
            ThreadPolicy newPolicy = new ThreadPolicy.Builder(oldPolicy).permitCustomSlowCalls().build();
            setThreadPolicy(newPolicy);
            return oldPolicy;
        } else {
            return null;
        }
    }

    public static void restoreStrictMode(@Nullable ThreadPolicy policy) {
        if (ENABLED && policy != null) {
            setThreadPolicy(policy);
        }
    }

    private static ThreadPolicy getMainThreadPolicy() {
        if (ENABLED) {
            ThreadPolicy.Builder threadPolicyBuilder =
                    new ThreadPolicy.Builder().detectAll().penaltyLog();

            boolean deathAllowedByLogTag = Log.isLoggable(MAIN_DEATH_TAG, Log.VERBOSE);
            if ((MAIN_THREAD_ALLOWS_DEATH_PENALTY || deathAllowedByLogTag)
                    && !ActivityManager.isRunningInTestHarness()) {
                threadPolicyBuilder.penaltyDeath();
            } else {
                threadPolicyBuilder.penaltyFlashScreen();
            }
            return threadPolicyBuilder.build();
        } else {
            return getThreadPolicy();
        }
    }

    /** Allows setting the strict mode policy for the current non-main thread. */
    public static class Policy {

        @Nullable
        private final ThreadPolicy mPolicy;

        /** @param policy The {@link ThreadPolicy} to set. */
        public Policy(@Nullable ThreadPolicy policy) {
            if (ENABLED) {
                mPolicy =
                        (policy == null)
                                ? new ThreadPolicy.Builder().build()
                                : new ThreadPolicy.Builder(policy).build();
            } else {
                mPolicy = null;
            }
        }

        /** Sets the strict mode policy for the current thread. */
        @WorkerThread
        public void enforce() {
            ThreadUtils.checkNotMainThread();
            if (ENABLED) {
                if (mPolicy != null) {
                    setThreadPolicy(mPolicy);
                }
            }
        }
    }
}
