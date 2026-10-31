# ProGuard / R8 Rules for Lectures (MUST Timetable & Student Companion)

# --- General Android & Line Numbers ---
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Kotlin & Coroutines ---
-keepattributes EnclosingMethod,InnerClasses
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- AndroidX & Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# --- Room Persistence Library ---
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
    @androidx.room.TypeConverter *;
}

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepnames class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    public static <fields>;
}
-dontwarn kotlinx.serialization.**

# --- Application Models & Data Layer ---
-keep class com.mustime.features.timetable.model.** { *; }
-keep class com.mustime.features.timetable.domain.** { *; }
-keep class com.mustime.features.timetable.data.** { *; }
-keep class com.mustime.features.notes.domain.** { *; }
-keep class com.mustime.features.rooms.model.** { *; }
-keep class com.mustime.features.rooms.UniversityDirectory { *; }
-keep class com.mustime.features.rooms.RoomDirectoryKt { *; }
-keep class com.mustime.features.tasks.Assignment { *; }
-keep class com.mustime.features.tasks.TaskItem { *; }
-keep class com.mustime.features.tasks.TaskPriority { *; }
-keep class com.mustime.features.tasks.TaskCategory { *; }

# --- Alarms, Receivers, and System Services ---
-keep class com.mustime.core.alarm.** { *; }
-keep class com.mustime.core.notification.** { *; }
-keep class com.mustime.core.auth.** { *; }
-keep class com.mustime.core.sync.** { *; }
-keep class com.mustime.features.timetable.receiver.** { *; }

# --- Firebase & Google Play Services ---
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn androidx.credentials.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

