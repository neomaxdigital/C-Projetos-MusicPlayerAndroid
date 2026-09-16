# Media3 ships consumer rules for its reflective components. Keep the manifest entry point explicit.
-keep class com.musicplayer.offline.playback.PlaybackService { *; }
-dontwarn java.awt.**
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
