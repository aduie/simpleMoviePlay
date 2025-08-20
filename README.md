# 简易播放器（GrandmaVideoPlayer）

一个**极简离线视频播放器** Android 应用：
- 打开即全屏播放本地视频
- 上/下滑切换视频（类似抖音）
- 点击屏幕暂停/继续
- 自动适配 Android 6.0+，请求必要读视频权限

## 1) 如何把项目打包成 APK（不需要本地 Android Studio）
你可以用 **GitHub Actions** 在线编译：

1. 把本项目上传到你的 GitHub 仓库（新建仓库 -> 上传 ZIP 解压后的文件）。
2. 仓库中的 `.github/workflows/android.yml` 已配置好工作流，提交后会自动运行。
3. 进入 GitHub 仓库的 **Actions** 页面，找到运行记录，下载构建产物（`app-debug.apk`）。
4. 把 APK 传到手机安装即可。

> 注：这是 **Debug** 构建，方便安装测试；若需要正式签名，可另行配置 keystore。

## 2) 把视频放到手机哪里？
应用会自动扫描手机里的**所有本地视频**（如 `Movies/` 文件夹）。
把视频拷贝到手机存储即可，无需特定目录。

## 3) 权限说明
- Android 13+：`READ_MEDIA_VIDEO`
- Android 12 及以下：`READ_EXTERNAL_STORAGE`

首次打开会请求一次权限。

## 4) 常见问题
- **没有找到视频**：请确认已把视频文件拷贝到手机存储，并给予读取权限。
- **无法上下滑**：确保视频列表多于 1 个；单个视频时也能播放，但无切换效果。

## 5) 本地构建（可选）
如果以后安装了 Android Studio：
```bash
# 终端执行
./gradlew assembleDebug
# 生成的 APK 在 app/build/outputs/apk/debug/app-debug.apk
```
