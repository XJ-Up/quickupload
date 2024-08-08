[![pk3f4HO.png](https://s21.ax1x.com/2024/05/30/pk3f4HO.png)](https://imgse.com/i/pk3f4HO)

### 一个让开发者快速完成上传功能的框架 支持java、kotlin,使用说明：[QuickUpDoc](https://xj-up.github.io/quickupdoc/)

# 截图

| 单文件上传模式                                                                                      | 单文件上传模式多个文件上传                                                                                      | 多个文件同时上传模式                                                                                        |
|----------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| ![单文件上传模式](https://github.com/XJ-Up/quickupload/blob/main/pictureresources/one.gif?raw=true) | ![单文件上传模式多个文件上传](https://github.com/XJ-Up/quickupload/blob/main/pictureresources/two.gif?raw=true) | ![多个文件同时上传模式](https://github.com/XJ-Up/quickupload/blob/main/pictureresources/three.gif?raw=true) |

# 如何集成

- 添加仓库

```groovy
// build.gradle(Project:)
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

- 添加依赖

```groovy
// build.gradle(Module:)
dependencies {
    implementation 'com.github.XJ-Up:quickupload:1.2.1'
}
```
## 具体使用可参考[demo](https://github.com/XJ-Up/quickupload/tree/main/app/src/main/java/com/dh/updemo)
#### 如果你没有服务器上传接口，你可以下载服务器[demo](https://github.com/XJ-Up/TestServer)搭建自己的测试服务器，来体验quickupload


