# Minimal Proguard / R8 rules for Lumora
-keepattributes SourceFile,LineNumberTable
-repackageclasses ''
-allowaccessmodification

# Coil rules
-dontwarn io.coilkt.**
