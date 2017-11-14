package com.google.android.clockwork.common.suppliers;

import static com.google.common.base.Preconditions.checkState;

import android.os.Trace;
import android.support.annotation.VisibleForTesting;
import com.google.common.base.Supplier;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Supplies the instance returned by the given supplier on its first {@link Supplier#get get} call,
 * unless a {@linkplain #setTestInstance test instance is set}. The value returned by {@link
 * Supplier#get} must not be null.
 *
 * @param <V> Type of instance(s) supplied by this object.
 */
@ThreadSafe
public class LazySingletonSupplier<V> {

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private V mValue;

    @GuardedBy("mLock")
    private V mTestValue;

    /** Sets a test instance that should be returned instead of the real instance. */
    @VisibleForTesting
    public void setTestInstance(V testInstance) {
        synchronized (mLock) {
            mTestValue = testInstance;
        }
    }

    /** Clears any test instance that has been set via {@link #setTestInstance(Object)}. */
    @VisibleForTesting
    public void clearTestInstance() {
        synchronized (mLock) {
            mTestValue = null;
        }
    }

    /**
     * Returns the value corresponding to the supplier this reference was initialized with, or the
     * test instance if applicable.
     *
     * @param supplier Supplier to retrieve the singleton from, if no existing instance exists
     * @param traceSection Section name to use for performance tracing when a singleton is created
     * @throws IllegalStateException if supplier returned a null value and no test instance was set
     */
    protected V get(Supplier<V> supplier, String traceSection) {
        synchronized (mLock) {
            if (mTestValue != null) {
                return mTestValue;
            } else if (mValue != null) {
                return mValue;
            } else {
                Trace.beginSection(traceSection);
                mValue = supplier.get();
                Trace.endSection();
                checkState(
                        mValue != null,
                        "Supplier returned a null value. [supplier class=%s]",
                        supplier.getClass());
                return mValue;
            }
        }
    }
}
