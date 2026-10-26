# ProGuard and R8 Security & Optimization Rules for Google Play Store (Globoup Production v11)

# 1. Preservar clases, modelos y lógica central de GloboUp (Paquetes Kotlin y ApplicationId)
-keep class com.example.** { *; }
-keep interface com.example.** { *; }
-keep enum com.example.** { *; }
-keep class com.aistudio.globoup.arcade.** { *; }
-keep interface com.aistudio.globoup.arcade.** { *; }
-keep enum com.aistudio.globoup.arcade.** { *; }
-keep class com.aiastudio.globoup.arcade.** { *; }
-keep interface com.aiastudio.globoup.arcade.** { *; }
-keep enum com.aiastudio.globoup.arcade.** { *; }

# ViewModels y Lifecycle
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.AndroidViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# 2. Puntos de entrada nativos de Android y Lifecycle
-keep public class com.example.MainActivity extends androidx.activity.ComponentActivity { *; }
-keep public class com.aistudio.globoup.arcade.MainActivity extends android.app.Activity { *; }
-keep public class com.aiastudio.globoup.arcade.MainActivity extends android.app.Activity { *; }
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends android.app.Service { *; }

# 3. Google Mobile Ads (AdMob) SDK & Native Ads
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.nativead.** { *; }
-keep interface com.google.android.gms.ads.nativead.** { *; }
-dontwarn com.google.android.gms.ads.**

# 4. Google Play Services y AdServices
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.adservices.** { *; }
-keep class android.adservices.** { *; }
-dontwarn com.google.android.adservices.**
-dontwarn android.adservices.**

# 5. Jetpack Compose, Corrutinas y Reflexión
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn androidx.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.coroutines.**

# 6. Almacenamiento local (SharedPreferences)
-keepclassmembers class * implements android.content.SharedPreferences$OnSharedPreferenceChangeListener { *; }

# 7. Desofuscación y mapeo de errores para Google Play Console
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
