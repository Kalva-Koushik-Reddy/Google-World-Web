# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

@proguard
-keep class org.apache.poi.** { *; }
-keep interface org.apache.poi.** { *; }

# Specific rules that might be needed depending on POI features used
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; } # If using XMLBeans features of POI
-keep interface org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# For handling underlying XML processing if not already covered
-keep class com.sun.xml.internal.stream.** { *; }
-dontwarn com.sun.xml.internal.stream.**
-keep class com.fasterxml.aalto.** { *; } # If Aalto XML parser is pulled in
-dontwarn com.fasterxml.aalto.**