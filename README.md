[![pk3f4HO.png](https://s21.ax1x.com/2024/05/30/pk3f4HO.png)](https://imgse.com/i/pk3f4HO)
### 一个让开发者快速完成上传功能的框架

# 截图

| 单文件上传模式                                | 单文件上传模式多个文件上传                                                     | 多个文件同时上传模式                                           |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
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
    implementation 'com.github.XJ-Up:quickupload:1.0.0'
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
//单文件
     QuickUploadRequest(this, serverUrl = "你的上传地址")
                    .setMethod("POST")
                    .addFileToUpload(
                        filePath = mPath, //文件地址（注意：content://也行哦）
                        parameterName = "files" //后台服务接收的参数名
                    )
                    .setResumedFileStart(0)//如果需要断点续传调用此方法，默认情况下不需要调用
                    .setUploadID("1")
                    .startUpload()
					
//同时多文件（不支持断点续传）
 QuickUploadRequest(this, serverUrl = "你的上传地址")
                    .setMethod("POST")
                    .apply {
                        filePath.forEachIndexed { index, s ->
                            addFileToUpload(
                                filePath = s,
                                parameterName = "files"
                            )
                        }
                    }
                    .setUploadID("2")
                    .startUpload()
```
#### 第三步 获取上传详情
```kotlin
   RequestLiveData(this,object :RequestObserverDelegate{
            override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                //上传完成时调用 ，注意：成功或错误都会触发
            }

            override fun onCompletedWhileNotObserving() {
                //仅在监听单个上传ID并注册请求观察者时调用
            }

            override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
                //出现异常时
            }

            override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                //上传中进度
            }

            override fun onSuccess(
                context: Context,
                uploadInfo: UploadInfo,
                serverResponse: ServerResponse
            ) {
                //上传成功时
            }

            override fun onWait(context: Context, uploadInfo: UploadInfo) {
                //加入上传队列，但不一定开始上传时
            }

        })
```
具体使用可参考demo

