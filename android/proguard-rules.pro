# LibGDX ProGuard rules
-verbose

-dontwarn android.support.**
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreeType

-keep class com.badlogic.gdx.controllers.android.AndroidControllers

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class fr.mesabloo.heavymachdefense.**$$serializer { *; }
-keepclassmembers class fr.mesabloo.heavymachdefense.** {
    *** Companion;
}
-keepclasseswithmembers class fr.mesabloo.heavymachdefense.** {
    kotlinx.serialization.KSerializer serializer(...);
}
