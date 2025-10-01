-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# ---- BEGIN: ChatGPT hardening append ----
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
# Ajusta el paquete de modelos si procede
-keep class **.model.** { *; }

-keep class androidx.room.** { *; }
-keep @androidx.room.* class * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep interface * implements androidx.room.RoomDatabase

-keepclassmembers class ** implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-keep class androidx.work.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
# ---- END: ChatGPT hardening append ----
