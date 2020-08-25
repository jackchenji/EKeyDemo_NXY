-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-dontskipnonpubliclibraryclassmembers
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn android.util.**

-keep class javax.naming.** { *;}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepattributes InnerClasses

-keep class org.simalliance.openmobileapi.service.** { *; }
-dontwarn org.simalliance.openmobileapi.service.**
-keep class org.simalliance.openmobileapi.Channel { *; }
-dontwarn org.simalliance.openmobileapi.Channel
-keep class org.simalliance.openmobileapi.Reader { *; }
-dontwarn org.simalliance.openmobileapi.Reader
-keep class org.simalliance.openmobileapi.SEService { *; }
-dontwarn org.simalliance.openmobileapi.SEService
-keep class org.simalliance.openmobileapi.Session { *; }
-dontwarn org.simalliance.openmobileapi.Session
-keep class org.simalliance.openmobileapi.SEService$CallBack { *; }
-dontwarn org.simalliance.openmobileapi.SEService$CallBack
-keep class junit.textui.TestRunner { *; }
-dontwarn junit.textui.TestRunner
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.util.io.pem.AllTests { *; }
-dontwarn org.bouncycastle.util.io.pem.AllTests

-keep class com.froad.ukey.** { *; }
-dontwarn com.froad.ukey.**

