# Keep the device-admin receiver and provisioning entry points that the
# system instantiates by name.
-keep class com.darkib.appduper.admin.** { *; }
-keep class com.darkib.appduper.bridge.** { *; }
-keep class com.darkib.appduper.install.** { *; }

# APK cloning libraries — keep intact so R8 doesn't strip reflectively-used code.
-keep class com.reandroid.** { *; }
-dontwarn com.reandroid.**
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-dontwarn javax.annotation.**
-dontwarn java.awt.**
