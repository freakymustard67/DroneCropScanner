# Keep Gson serialization models
-keepclassmembers class com.example.cropdrone.data.remote.dto.** { *; }
-keepclassmembers class com.example.cropdrone.data.local.entity.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Keep OkHttp/Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep Google Maps
-keep class com.google.android.gms.maps.** { *; }
