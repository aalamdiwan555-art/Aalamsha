# Keep Android components that are instantiated from the manifest.
-keep class com.autopilot.driver.AalamApp { *; }
-keep class com.autopilot.driver.automation.AalamAccessibilityService { *; }
-keep class com.autopilot.driver.service.AalamScreenService { *; }
-keep class com.autopilot.driver.service.FloatingPanelService { *; }
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn org.jetbrains.kotlinx.coroutines.debug.**
