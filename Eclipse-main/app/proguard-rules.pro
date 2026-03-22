# Eclipse Browser — ProGuard Rules
# Section 33.3: R8 obfuscation enabled in release builds

# ── KEEP ESSENTIAL ANDROID CLASSES ──
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# ── ECLIPSE CONFIG — Keep so backend URLs compile correctly ──
-keep class com.eclipse.browser.EclipseConfig { *; }

# ── VIEWMODEL — Required for Android lifecycle ──
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ── COMPOSE SERIALIZATION ──
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── DATASTORE ──
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class androidx.datastore.** { *; }

# ── OKHTTP & NETWORKING ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── JSON ──
-keep class org.json.** { *; }

# ── COIL IMAGE LOADING ──
-keep class coil.** { *; }
-dontwarn coil.**

# ── WEBKIT ──
-keep class android.webkit.** { *; }
-keepclassmembers class * extends android.webkit.WebViewClient {
    <methods>;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    <methods>;
}

# ── SECURITY CRYPTO ──
-keep class androidx.security.crypto.** { *; }

# ── REMOVE LOGGING IN RELEASE ──
# Section 31.16: Never log backend URLs or responses in production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ── REMOVE DEBUG MARKERS ──
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
}

# ── DATA CLASSES — Keep field names for JSON serialization ──
-keepclassmembers class com.eclipse.browser.ui.viewmodel.SearchResult { *; }
-keepclassmembers class com.eclipse.browser.ui.viewmodel.SearchResponse { *; }
-keepclassmembers class com.eclipse.browser.ui.viewmodel.TabInfo { *; }
-keepclassmembers class com.eclipse.browser.ui.viewmodel.WeatherData { *; }
-keepclassmembers class com.eclipse.browser.ui.screens.AiMessage { *; }
-keepclassmembers class com.eclipse.browser.ui.screens.Extension { *; }
-keepclassmembers class com.eclipse.browser.ui.screens.DiscoverScript { *; }
