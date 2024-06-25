[![pk3f4HO.png](https://s21.ax1x.com/2024/05/30/pk3f4HO.png)](https://imgse.com/i/pk3f4HO)

### 一个让开发者快速完成上传功能的框架

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
    implementation 'com.github.XJ-Up:quickupload:1.1.0'
}
```

# 如何使用

#### 第一步 Application进行初始化配置

```kotlin
//defaultNotificationChannel用于后台服务需自行createNotificationChannel详情见demo
//debug 用于是否打印日志
UploadConfiguration.initialize(
    context = this,
    defaultNotificationChannel = notificationChannelID,
    debug = BuildConfig.DEBUG
)
```

#### 第二步 准备上传

```kotlin

//第一步：构建数据类并继承 UploadObserverBase() ，其中fileName、filePath均为自定义内容
data class FileItem(
    val fileName: String,
    val filePath: String,
) : UploadObserverBase()


//第二步：创建数据类对象 赋值uploadId 添加至观察者
fileItem = FileItem(name(Uri.parse(path)), path)
fileItem?.uploadId = path //根据你的具体情况选择合适的值作为 uploadId
//添加至观察者
UploadService.observers.add(fileItem)


//第三步：创建 QuickUploadRequest 并赋值给数据类
 --单文件上传
val request = QuickUploadRequest(this, serverUrl = "你的上传地址")
    .setMethod("POST")
    .addFileToUpload(
        filePath = fileItem!!.filePath,
        parameterName = "files"
    )
    .setResumedFileStart(0)//如果需要断点续传调用此方法，默认情况下不需要调用
fileItem.quickUploadRequest = request

 --多个单文件上传
fileList.forEachIndexed { index, s ->
    val request =
        QuickUploadRequest(this, serverUrl = "你的上传地址")
            .setMethod("POST")
            .addFileToUpload(
                filePath = s.filePath,
                parameterName = "files"
            )
    s.quickUploadRequest = request
}

 --多文件同时上传
val  request=QuickUploadRequest(this, serverUrl = "你的上传地址")
    .setMethod("POST")
    .apply {
        it.filePath .forEachIndexed { index, s ->
            addFileToUpload(
                filePath = s,
                parameterName = "files"
            )
        }
    }
filesItem.quickUploadRequest=request


//第四步：开始或停止上传
fileItem.startUpload()
fileItem.stopUpload()
```

#### 第三步 获取上传详情

```kotlin
  fileItem.refresh { uploadStatus, uploadInfo, throwable, serverResponse ->
    when (uploadStatus) {
        UploadStatus.DEFAULT -> {
            //默认初始化状态
        }
        UploadStatus.Wait -> {
            //加入上传队列，但不一定开始上传时
        }
        UploadStatus.InProgress -> {
            //上传中进度
        }
        UploadStatus.Success -> {
            //上传成功时
        }
        UploadStatus.Error -> {
            //出现异常时
        }
        UploadStatus.Completed -> {
            //上传完成时调用 ，注意：成功或错误都会触发
        }
        else -> {}
    }

}
```
#### 如果你没有服务器上传接口，你可以下载服务器demo[点我](https://github.com/XJ-Up/TestServer)搭建自己的测试服务器，来体验quickupload
#### 具体使用可参考demo

