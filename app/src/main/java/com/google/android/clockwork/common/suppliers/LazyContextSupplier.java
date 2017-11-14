package com.google.android.clockwork.common.suppliers;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.support.annotation.Nullable;
import com.google.common.base.Supplier;
import javax.annotation.concurrent.GuardedBy;

/**
 * A {@link LazySingletonSupplier} that supplies a singleton instance for a given {@link Context}.
 */
public final class LazyContextSupplier<V> extends LazySingletonSupplier<V> {

    /**
     * Interface used by {@link LazyContextSupplier} to get a new instance given an application
     * context.
     */
    public interface InstanceCreator<V> {
        V createNewInstance(Context applicationContext);
    }

    private final Object mLock = new Object();
    private final InstanceCreator<? extends V> mInstanceCreator;
    private final String mName;

    @GuardedBy("mLock")
    @Nullable
    private LazySupplier mLazySupplier;

    /**
     * @param instanceCreator Creator to use when a new instance is required.
     * @param name The name of the singleton to use for performance tracing and debugging purposes.
     */
    public LazyContextSupplier(InstanceCreator<? extends V> instanceCreator, String name) {
        mInstanceCreator = checkNotNull(instanceCreator);
        mName = checkNotNull(name);
    }

    public V get(Context context) {
        checkNotNull(context);

        // Only initialize mLazySupplier once.
        synchronized (mLock) {
            if (mLazySupplier == null) {
                // To prevent leaking activity context, always use application context.
                mLazySupplier = new LazySupplier(context.getApplicationContext());
            }
            return super.get(mLazySupplier, mName);
        }
    }

    private final class LazySupplier implements Supplier<V> {
        private final Context mApplicationContext;

        private LazySupplier(Context applicationContext) {
            mApplicationContext = checkNotNull(applicationContext);
        }

        @Override
        public V get() {
            return mInstanceCreator.createNewInstance(mApplicationContext);
        }
    }
}
