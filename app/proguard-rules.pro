# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable

# Room Database and Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.database.** { *; }

# Preserve Entity fields and annotations
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Ignore *;
}

# Coroutines and Flow Main Dispatcher
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# VPN & Firewall Services and Broadcast Receivers
-keep class com.example.firewall.FirewallVpnService { *; }
-keep class com.example.firewall.BootReceiver { *; }
-keep class com.example.firewall.PackageChangeReceiver { *; }
-keep class com.example.quicksettings.** { *; }

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }

