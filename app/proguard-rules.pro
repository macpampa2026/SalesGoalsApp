# Proguard rules
-keep class com.salesgoals.app.data.entities.** { *; }
-keep class com.salesgoals.app.data.models.** { *; }
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
