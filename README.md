[![](https://jitpack.io/v/RookieExaminer/AndroidUtils.svg)](https://jitpack.io/#RookieExaminer/AndroidUtils)

在 `settings.gradle` 文件中加入

```groovy
dependencyResolutionManagement {
    repositories {
        // JitPack 远程仓库：https://jitpack.io
        maven { url 'https://jitpack.io' }
    }
}
```

在项目 app 模块下的 `build.gradle` 文件中加入远程依赖

```groovy
dependencies {
	        implementation 'com.github.RookieExaminer:AndroidUtils:0.0.3'
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
