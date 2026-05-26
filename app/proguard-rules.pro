# Mithaq App — ProGuard/R8 Rules
# These rules prevent obfuscation from stripping classes that are needed at runtime.

# ---- Kotlin ----
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---- Firebase Firestore: keep data model classes for serialization ----
-keep class com.mithaq.app.model.** { *; }

# ---- Room Database ----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ---- Jetpack Compose ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---- Google ML Kit (Face Detection) ----
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ---- Google Generative AI (Gemini) ----
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ---- Coil (Image Loading) ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- OkHttp (used internally) ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Prevent stripping of Coroutines internals ----
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ---- Keep enum names (used in Firestore string mappings) ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ---- Keep crash stack traces readable ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
