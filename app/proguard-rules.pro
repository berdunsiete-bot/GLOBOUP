# ProGuard and R8 Security & Optimization Rules for Google Play Store

# Preserve Kotlin reflection and Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep data models
-keep class com.example.GameStatus { *; }
-keep class com.example.StormCloud { *; }
-keep class com.example.StarCollectible { *; }
-keep class com.example.SparkleParticle { *; }
-keep class com.example.FloatingText { *; }
-keep class com.example.BackgroundCloud { *; }

# Prevent obfuscation of native Android entry points
-keep public class com.example.MainActivity extends androidx.activity.ComponentActivity

# Suppress warnings for standard AndroidX dependencies
-dontwarn androidx.**
-dontwarn kotlinx.coroutines.**

# Hide debugging line numbers and source file names in production release APK/AAB
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
