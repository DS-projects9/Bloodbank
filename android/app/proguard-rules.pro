# ---- Hilt ----
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewFragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# ---- Retrofit ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ---- Gson ----
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.medkeen.**$$serializer { *; }
-keepclassmembers class com.medkeen.** {
    *** Companion;
}
-keepclasseswithmembers class com.medkeen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Data models (Gson reflection) ----
-keep class com.medkeen.data.model.** { *; }
-keep class com.medkeen.data.remote.** { *; }

# ---- Keep API interface methods ----
-keepclassmembers interface com.medkeen.data.remote.** {
    <methods>;
}

# ---- General ----
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Google Tink (used by security-crypto) ----
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
