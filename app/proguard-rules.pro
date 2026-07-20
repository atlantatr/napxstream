# Retrofit/Gson model sınıflarını koru (Xtream API cevapları serialize/deserialize edilirken kırılmasın)
-keep class com.napxstream.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Chromecast / Play Services Cast
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.mediarouter.** { *; }
-keep class com.napxstream.cast.** { *; }
-dontwarn com.google.android.gms.cast.**

# security-crypto (Tink)
-keep class com.google.crypto.tink.** { *; }
-keep,allowobfuscation,allowshrinking class com.google.crypto.tink.**
-dontwarn com.google.crypto.tink.**

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit (genel tip imzaları korunmalı)
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Room / coroutines
-dontwarn kotlinx.coroutines.**
-keep class * extends androidx.room.RoomDatabase
