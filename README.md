[![](https://jitpack.io/v/azheng95/AndroidUtils.svg)](https://jitpack.io/#azheng95/AndroidUtils)

在 `settings.gradle` 文件中加入

```groovy
dependencyResolutionManagement {
    repositories {
        // JitPack 远程仓库：https://jitpack.io
        maven { url 'https://jitpack.io' }
    }
}
```
或者在 `settings.gradle.kts` 文件中加入

```groovy
dependencyResolutionManagement {
    repositories {
        // JitPack 远程仓库：https://jitpack.io
        maven { url = uri("https://jitpack.io") }
    }
}
```


在项目 app 模块下的 `build.gradle` 文件中加入远程依赖

```groovy
dependencies {
	        implementation 'com.github.azheng95:AndroidUtils:0.0.8'
}
```

在 Application 初始化 Utils

```
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化Utils
        Utils.init(this)
    }
}
```
***********添加以下依赖**********

 ```
    //https://github.com/Tencent/MMKV
    implementation 'com.tencent:mmkv:1.3.12'
    // Gson 解析容错：https://github.com/getActivity/GsonFactory
    implementation 'com.github.getActivity:GsonFactory:9.6'
    // Json 解析框架：https://github.com/google/gson
    implementation 'com.google.code.gson:gson:2.12.1'
    // Kotlin 反射库：用于反射 Kotlin data class 类对象，2.0.0 请修改成当前项目 Kotlin 的版本号
    implementation 'org.jetbrains.kotlin:kotlin-reflect:2.0.0'
```
