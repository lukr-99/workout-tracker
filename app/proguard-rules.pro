# Room generates direct adapters, but its database/entities must retain their constructors and
# annotations when an optimized release is assembled.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class com.lukr99.workout.data.** { *; }

# The export/import contract is deliberately stable across app versions.
-keep @kotlinx.serialization.Serializable class com.lukr99.workout.domain.** { *; }
-keep @kotlinx.serialization.Serializable class com.lukr99.workout.data.export.** { *; }
-keep @kotlinx.serialization.Serializable class com.lukr99.workout.data.sync.** { *; }
-keep class com.lukr99.workout.domain.**$$serializer { *; }
-keep class com.lukr99.workout.data.**$$serializer { *; }

# Health Connect crosses a provider/Binder boundary and may instantiate SDK record metadata.
-keep class androidx.health.connect.client.** { *; }
-keep class com.lukr99.workout.data.health.** { *; }

# OkHttp is used by the wger sync. Keep its public runtime surface and suppress warnings for its
# optional TLS providers, which are not bundled by this app.
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
