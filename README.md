# OBS Overlay — Minecraft 26.3（实验性适配）

基于 [zziger/obs-overlay](https://github.com/zziger/obs-overlay)（MIT），面向无规则服务器使用场景的 Fabric / Windows x64 移植。当前分支为 `port/minecraft-26.3`，版本 `4.0.0-alpha.2+26.3`。

选中的 HUD、GUI 仅在本地显示，目标是在 OBS 游戏采集中排除。**目前已通过构建、Mixin、真实 GPU 隔离测试，尚未完成真实游戏与 OBS 的录制验收，请勿用于未经预览检查的正式直播。**

## alpha.2 热修复

修复启动主菜单时 `Texture view does not exist` 崩溃：本地 GUI 渲染器复用原版全景背景纹理，不再重复注册并替换资源重载管理的纹理。替换 alpha.1 JAR 即可，配置文件保留。已用真实 Vulkan GPU 复现旧异常并验证纹理生命周期；尚未宣称完成整个整合包的启动或 OBS 验收。

## 两种渲染后端

| 后端 | 本地显示方式 | 当前限制 |
| --- | --- | --- |
| OpenGL | SDL3 的原生 `wglSwapBuffers` 回调中合成本地画面 | OBS 游戏采集钩子顺序仍需实际验收 |
| Vulkan | GPU 异步读回完整本地画面，由 Windows 捕获排除窗口显示 | 实验性；窗口或无边框模式；本地画面最多 30 FPS；增加 CPU/GPU 开销和显示延迟；不支持独占全屏 |

Vulkan 窗口采用 GDI 软件绘制，没有额外的图形交换链；启用 `WDA_EXCLUDEFROMCAPTURE`、鼠标穿透和不激活窗口。失焦、最小化、尺寸不匹配或帧过期时隐藏。Windows 捕获排除失败时停用过滤；切换独占全屏期间也停用过滤。该路径需要 Windows 10 2004 或更新系统及正常的桌面合成环境。

## 安装与设置

1. Minecraft **26.3**、Java **25**、Fabric Loader **0.19.5 或更新版本**，以及对应版本 Fabric API。
2. 放入 `obs-overlay-fabric-4.0.0-alpha.2+26.3.jar`。不要与 1.21.11 版同时安装。
3. 打开 **选项 → 视频设置 → OBS Overlay 设置**；Mod Menu 可选。
4. 分别设置 Meteor、RusherHack、Xaero、MaLiLib 的 HUD / GUI 开关。开启表示仅本地显示；关闭表示正常录制。模组开关默认关闭。
5. OBS 使用游戏采集，关闭采集源的「采集第三方叠加层」。打开「录制测试标记」，确认本地有 `OBS LOCAL ONLY`，OBS 预览没有。

保留原版 F3、聊天、TAB、字幕、计分板、动作栏、标题、药水效果、主 HUD 和所有游戏内界面的过滤选项。原版 F3 默认仅本地显示。

配置保存为 `config/obs_overlay-26.3.json`。首次使用可读取旧的 `obs_overlay-1.21.11.json`，后续修改只写入新版配置。

## 兼容范围与验证边界

- 已迁移 26.3 的 GUI 提取 / 提交分离、RenderPearl、SDL3，以及 Java 25 工具链。
- 捕获队列排除隐藏内容；独立本地队列保留完整 HUD / 界面顺序，在对应 GUI 绘制阶段合成，避免隐藏 HUD 最后覆盖背包。
- Meteor 26.3-15 的 WidgetScreen 和独立 GUI 渲染器已通过实际 JAR 的 Mixin / 接口检查；分别处理主 GUI 提交前后的 Meteor 绘制，保留 HUD / GUI 开关独立性。**尚未完成真实游戏内 ClickGUI 录制验收。**
- Xaero 的私有队列和中间纹理不按屏幕纹理处理，避免误改其地图坐标空间。Xaero、XaeroPlus 透明世界地图、MaLiLib、RusherHack 的 26.3 组合尚未完成实际画面验收。
- 不能保证排除任意模组绕过标准渲染接口绘制的内容；显示器采集与窗口采集不属于当前验收范围。

详细结果见 [VALIDATION.md](VALIDATION.md)。1.21.11 稳定版仍在 [v3.0.0-mc1.21.11](https://github.com/toaru4/obs-overlay-1.21.11/releases/tag/v3.0.0-mc1.21.11)。

## 构建与测试

```text
./gradlew build runSmoke -x downloadAssets
./gradlew runSmoke -PglSmoke -x downloadAssets
./gradlew runSmoke -PvkSmoke -PdesktopSmoke -x downloadAssets
```

后两项需要 Windows x64 与相应显卡支持。默认 smoke 不启动完整游戏，不应将它解释为 OBS 录制验收。第三方测试模组可放到独立的 `run26_3/mods`；不打包进发布 JAR。

代码遵循上游 MIT 许可证；保留原作者署名和 MinHook 许可证。
