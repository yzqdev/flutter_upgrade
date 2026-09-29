# 内置XUpdate更新库混淆保护
-keep class com.xuexiang.xupdate.** { *; }
-dontwarn com.xuexiang.xupdate.**

# 更新提示弹窗使用的实体类
-keep class com.xuexiang.xupdate.entity.** { *; }

# ViewBinding生成的类
-keep class com.xuexiang.flutter_xupdate.databinding.** { *; }
