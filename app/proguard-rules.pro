# Gson uses reflection over the DTO classes — keep their field names.
-keep class lk.synergypharma.employee.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation class retrofit2.Response
