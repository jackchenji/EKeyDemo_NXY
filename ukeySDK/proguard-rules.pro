# 指定代码的压缩级别 0 - 7(指定代码进行迭代优化的次数，在Android里面默认是5，这条指令也只有在可以优化时起作用。)
-optimizationpasses 5
# 混淆时不会产生形形色色的类名(混淆时不使用大小写混合类名)
-dontusemixedcaseclassnames
# 指定不去忽略非公共的库类(不跳过library中的非public的类)
-dontskipnonpubliclibraryclasses
# 指定不去忽略包可见的库类的成员
-dontskipnonpubliclibraryclassmembers
 # 不进行预校验,Android不需要,可加快混淆速度。
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#不进行优化，建议使用此选项
-dontoptimize
#忽略警告
-ignorewarnings
#保证是独立的jar,没有任何项目引用,如果不写就会认为我们所有的代码是无用的,从而把所有的代码压缩掉,导出一个空的jar
-dontshrink
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn java.io.**
-dontwarn java.lang.**
-dontwarn java.util.**
-dontwarn java.security.**
-dontwarn java.net.**
-dontwarn java.math.**
-dontwarn javax.crypto.**
-dontwarn android.util.**
-dontwarn android.annotation.**
-dontwarn android.app.**
-dontwarn android.content.**
-dontwarn android.net.**
-dontwarn android.os.**
-dontwarn android.provider.**
-dontwarn android.view.**
-dontwarn android.support.**

-keep class javax.naming.** { *;}

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

-keep class java.lang.reflect.** { *; }
-dontwarn java.lang.reflect.**
-dontnote java.lang.reflect.**
-keep class android.telephony.** { *; }
-dontwarn android.telephony.**
-dontnote android.telephony.**
-dontnote com.yulong.android.telephony.CPSmsManager

-keep class com.froad.ukey.manager.** { *; }
-dontwarn com.froad.ukey.manager.**
-keep class com.froad.ukey.simchannel.** { *; }
-dontwarn com.froad.ukey.simchannel.**
-keep class com.froad.ukey.utils.np.** { *; }
-dontwarn com.froad.ukey.utils.np.**
-keep class com.froad.ukey.constant.** { *; }
-dontwarn com.froad.ukey.constant.**
-keep class com.froad.ukey.interf.** { *; }
-dontwarn com.froad.ukey.interf.**
-keep class com.froad.ukey.jni.** { *; }
-keep class org.bc.** { *; }
-dontwarn com.froad.ukey.jni.**


