# Add project specific ProGuard rules here.

# Keep Room entities and data models
-keep class com.example.data.models.** { *; }

# Keep kotlinx.serialization annotated classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room database and DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Moshi / Retrofit / Gson if used
-keepattributes Signature
-keepattributes *Annotation*

# Preserve line numbers for debugging if needed
-keepattributes SourceFile,LineNumberTable
