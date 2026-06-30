# ════════════════════════════════════════════
#  NgheNhac — ProGuard / R8 Rules
#  Phiên bản hoàn chỉnh cho release build
# ════════════════════════════════════════════

# ── Debug info ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── AndroidX / Material ──
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ── ExoPlayer / Media3 ──
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# ── Room Database ──
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ── Retrofit ──
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions, InnerClasses
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── Gson ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ═══ Data model classes (Gson serialization) ═══
-keep class com.example.nghenhac.data.model.** { *; }
-keep class com.example.nghenhac.data.remote.** { *; }
-keepclassmembers class com.example.nghenhac.data.model.** { *; }

# ── Glide ──
-dontwarn com.bumptech.glide.**
-keep class com.bumptech.glide.** { *; }
-keep class * extends com.bumptech.glide.module.AppGlideModule
-keep class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class * extends com.bumptech.glide.request.target.CustomTarget

# ── Firebase ──
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Navigation ──
-dontwarn androidx.navigation.**
-keep class androidx.navigation.** { *; }
-keepclassmembers class * {
    @androidx.navigation.NavDeepLink <fields>;
}

# ── Lifecycle ──
-dontwarn androidx.lifecycle.**
-keep class androidx.lifecycle.** { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent <methods>;
}

# ── WorkManager ──
-dontwarn androidx.work.**
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Security (EncryptedSharedPreferences) ──
-dontwarn androidx.security.**
-keep class androidx.security.** { *; }

# ── Room entities (không được obfuscate tên trường) ──
-keepclassmembers class com.example.nghenhac.data.local.entity.** {
    *;
}

# ── Keep our app classes ──
-keep class com.example.nghenhac.** { *; }
-keepclassmembers class com.example.nghenhac.** {
    public <methods>;
}

# ── Keep Service, BroadcastReceiver, ContentProvider ──
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keepclassmembers class * extends android.app.Service {
    public <methods>;
}

# ── Enum ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelable ──
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ── Serializable ──
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
