# Project-specific ProGuard rules for Lectures app

# =============================================================================
# Room Database
# =============================================================================
-keep class com.mustime.core.database.** { *; }
-keep class com.mustime.features.timetable.domain.** { *; }
-keep class com.mustime.features.rooms.** { *; }
-keep class com.mustime.core.util.** { *; }

# Room-specific
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.Dao

# Room entity annotations
-keep class * {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.ForeignKey *;
    @androidx.room.Index *;
    @androidx.room.TypeConverters *;
}

# =============================================================================
# KSP / Kotlin Symbol Processing
# =============================================================================
-keep class com.google.devtools.ksp.** { *; }
-dontwarn com.google.devtools.ksp.**

# =============================================================================
# Kotlin Coroutines
# =============================================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# =============================================================================
# Kotlinx Serialization
# =============================================================================
-keep class kotlinx.serialization.** { *; }
-keep class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
    @kotlinx.serialization.Transient *;
    @kotlinx.serialization.Contextual *;
    @kotlinx.serialization.Polymorphic *;
    @kotlinx.serialization.Subclass *;
}
-keep class kotlinx.serialization.internal.** { *; }

# =============================================================================
# Kotlinx DateTime
# =============================================================================
-keep class kotlinx.datetime.** { *; }

# =============================================================================
# AndroidX Lifecycle / ViewModel / LiveData / DataStore
# =============================================================================
-keep class androidx.lifecycle.** { *; }
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# =============================================================================
# Material3 / Compose
# =============================================================================
-keep class androidx.compose.** { *; }
-keep class androidx.activity.** { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.activity.**

# =============================================================================
# Google Play Services / Firebase
# =============================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# =============================================================================
# Kotlinx Coroutines / Coroutines Android
# =============================================================================
-keep class kotlinx.coroutines.android.** { *; }

# =============================================================================
# Custom classes that must not be obfuscated (reflection/serialization)
# =============================================================================
# Data classes used for JSON serialization
-keep class com.mustime.features.timetable.domain.** { *; }
-keep class com.mustime.features.calendar.domain.** { *; }
-keep class com.mustime.features.tasks.domain.** { *; }
-keep class com.mustime.features.rooms.domain.** { *; }
-keep class com.mustime.features.notes.domain.** { *; }
-keep class com.mustime.core.alarm.** { *; }
-keep class com.mustime.core.storage.** { *; }
-keep class com.mustime.core.sync.** { *; }
-keep class com.mustime.core.notification.** { *; }
-keep class com.mustime.core.util.** { *; }

# Preserve @Serializable classes
-keep @kotlinx.serialization.Serializable class *

# =============================================================================
# Keep Parcelable implementations
# =============================================================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# =============================================================================
# Keep enum values
# =============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================================================
# Keep class members annotated with @JvmStatic or @JvmField
# =============================================================================
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic *;
    @kotlin.jvm.JvmField *;
}

# =============================================================================
# Preserve annotations
# =============================================================================
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# =============================================================================
# Optimization: Don't warn about missing classes we don't use
# =============================================================================
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**
-dontwarn com.google.**
-dontwarn org.jetbrains.**

# =============================================================================
# Keep names for debugging
# =============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =============================================================================
# Room Database - prevent obfuscation of table/column names
# =============================================================================
-keep class * {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.ForeignKey *;
    @androidx.room.Index *;
}