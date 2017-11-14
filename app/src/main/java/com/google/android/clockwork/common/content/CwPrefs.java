package com.google.android.clockwork.common.content;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceManager;
import com.google.android.clockwork.common.concurrent.CwStrictMode;
import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Set;

/** Get versions of shared prefs that ignore strict mode. */
public final class CwPrefs {

    /**
     * A global, shared instance of app preferences.
     *
     * <p>We use this singleton to avoid a change made in N in which the object returned by {@link
     * PreferenceManager#getDefaultSharedPreferences} might change, depending on the context that is
     * passed into the call.
     */
    @SuppressLint("PreferenceManagerUsage")
    public static final LazyContextSupplier<SharedPreferences> DEFAULT =
            new LazyContextSupplier<>(
                    new LazyContextSupplier.InstanceCreator<SharedPreferences>() {
                        @Override
                        @SuppressLint("PreferenceManagerUsage")
                        public SharedPreferences createNewInstance(Context applicationContext) {
                            ThreadPolicy policy = CwStrictMode.allowDiskReads();
                            try {
                                return new WrappedPrefs(
                                        PreferenceManager.getDefaultSharedPreferences(applicationContext));
                            } finally {
                                CwStrictMode.restoreStrictMode(policy);
                            }
                        }
                    },
                    CwPrefs.class.getSimpleName());

    private CwPrefs() {}

    /**
     * Returns {@link SharedPreferences} wrapped to ignore disk read and write StrictMode violations.
     */
    @SuppressLint("PreferenceManagerUsage")
    public static SharedPreferences wrapDefault(Context context) {
        ThreadPolicy policy = CwStrictMode.allowDiskReads();
        try {
            return new WrappedPrefs(PreferenceManager.getDefaultSharedPreferences(context));
        } finally {
            CwStrictMode.restoreStrictMode(policy);
        }
    }

    /**
     * Returns {@link SharedPreferences} wrapped to ignore disk read and write StrictMode violations.
     */
    public static SharedPreferences wrap(Context context, String name) {
        ThreadPolicy policy = CwStrictMode.allowDiskReads();
        try {
            return new WrappedPrefs(context.getSharedPreferences(name, Context.MODE_PRIVATE));
        } finally {
            CwStrictMode.restoreStrictMode(policy);
        }
    }

    private static class WrappedPrefs implements SharedPreferences {

        private final SharedPreferences mPreferences;

        private WrappedPrefs(SharedPreferences prefs) {
            mPreferences = Preconditions.checkNotNull(prefs);
        }

        @Override
        public Map<String, ?> getAll() {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getAll();
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public String getString(String key, String defValue) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getString(key, defValue);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getStringSet(key, defValues);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public int getInt(String key, int defValue) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getInt(key, defValue);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public long getLong(String key, long defValue) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getLong(key, defValue);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public float getFloat(String key, float defValue) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getFloat(key, defValue);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.getBoolean(key, defValue);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public boolean contains(String key) {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.contains(key);
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        @SuppressLint("CommitPrefEdits") // Interface compatibility.
        public Editor edit() {
            ThreadPolicy policy = CwStrictMode.allowDiskReads();
            try {
                return mPreferences.edit();
            } finally {
                CwStrictMode.restoreStrictMode(policy);
            }
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            mPreferences.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
