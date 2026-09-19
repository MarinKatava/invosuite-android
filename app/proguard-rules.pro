# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Gson + our model records (reflective field access)
-keep class com.invoisuite.app.model.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
