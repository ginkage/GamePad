-dontwarn android.bluetooth.**
-dontwarn sun.misc.Unsafe
-dontwarn javax.lang.model.element.Modifier
-dontwarn java.lang.ClassValue

-keep class com.ginkage.gamepad.ui.BluetoothScanFragment {}
-keep class com.ginkage.gamepad.ui.InputSettingsFragment {}

-keepclasseswithmembers class * {
    native <methods>;
}

-keepclassmembers class com.google.android.clockwork.bluetooth.HidDeviceCallback* { public *; }