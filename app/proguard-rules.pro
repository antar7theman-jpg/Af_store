# ==============================================================================
# R8 / ProGuard Optimization & Safety Rules for AF Store
# Preserves Reflection, Room, Firebase, ML Kit, CameraX, ZXing, and Compose
# while aggressively tree-shaking unused code and icons.
# ==============================================================================

-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'

# Preserve annotations, generics signatures, and line numbers for stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 1. App Data Models, Backup Payloads & Firebase Reflection Models
# ------------------------------------------------------------------------------
-keep class com.example.data.models.** { *; }
-keep class com.example.data.firebase.** { *; }
-keep class com.example.data.backup.** { *; }
-keep class com.example.data.datastore.** { *; }
-keep class com.example.utils.** { *; }

# Keep all Kotlin data classes used in serialization / Firestore mapping
-keepclassmembers class com.example.** {
    <init>(...);
    public <fields>;
    public <methods>;
}

# ------------------------------------------------------------------------------
# 2. Room Database, Entities & DAOs
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.database.** { *; }
-keep class com.example.data.dao.** { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 3. Firebase Firestore, Auth & Google Play Services / Credentials
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepnames class com.google.firebase.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.errorprone.annotations.**

# ------------------------------------------------------------------------------
# 4. Google ML Kit (Barcode Scanning & Text Recognition) + CameraX + ZXing
# ------------------------------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class androidx.camera.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**
-dontwarn com.google.zxing.**

# ------------------------------------------------------------------------------
# 5. Kotlin Coroutines, Serialization & Compose Runtime
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.debug.**
-dontwarn sun.misc.Unsafe

# Strip verbose/debug Android Log calls in release builds to save DEX size
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
