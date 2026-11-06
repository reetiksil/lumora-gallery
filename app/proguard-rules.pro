# Minimal Proguard / R8 rules for Lumora
-keepattributes SourceFile,LineNumberTable
-repackageclasses ''
-allowaccessmodification

# Coil rules
-dontwarn io.coilkt.**

# Media3 ExoPlayer keep rules for native audio/video decoders & rendering
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.**

# ExifInterface
-keep class androidx.exifinterface.media.ExifInterface { *; }

# DataStore Preferences serialization
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    <fields>;
    <methods>;
}

# Lumora Data Models
-keep class com.lumora.gallery.data.model.** { *; }
