package com.google.android.clockwork.utils;

import android.os.Build;

/** Utilities for working with Build versioning */
public final class BuildUtils {
    public static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);

    private BuildUtils() {}
}
