# OBS Overlay — Minecraft 1.21.11

基于 [zziger/obs-overlay](https://github.com/zziger/obs-overlay)（MIT）的 **Fabric / Windows x64 测试版移植**。

让选中的 HUD、GUI 在自己屏幕上显示，同时从 OBS **游戏采集**中排除。

## 安装和设置

1. 安装 Minecraft **1.21.11**、Java 21、Fabric Loader **0.18.4 或更新版本**。
2. 将本仓库 Releases 中的 `obs-overlay-fabric-*.jar` 与适用于 1.21.11 的 **Fabric API** 放入 `mods`。
3. 打开 **选项 → 视频设置 → OBS Overlay 设置**，或者通过 Mod Menu 打开。
4. 在「模组 HUD / 界面」页分别开启需要排除的项目。

| 模组 | HUD | GUI |
| --- | --- | --- |
| Meteor Client | 独立开关 | 独立开关 |
| RusherHack | 独立开关 | 独立开关 |
| Xaero 小地图 / 世界地图 | 共用 Xaero HUD 开关 | 共用 Xaero GUI 开关 |
| MaLiLib | 独立开关 | 独立开关，识别 MaLiLib 界面基类的子类 |

开启表示「仅本地显示」；关闭表示正常录制。各模组开关默认关闭。MaLiLib GUI 包括继承其界面基类的工具模组设置页；HUD 开关处理通过 MaLiLib 绘制的内容，不代表这些模组的所有自定义渲染。

另有原版 F3、聊天记录、TAB、字幕、计分板、动作栏、标题、药水效果、主 HUD 和所有游戏内界面的开关。原版 F3 默认仅本地显示。

配置即时保存于 `config/obs_overlay-1.21.11.json`，不会覆盖旧版配置。不需要 Architectury 或 Cloth Config。Mod Menu 可选。

## 录制检查

- 使用 OBS 的「游戏采集」，关闭采集源中的「采集第三方叠加层」。显示器采集、窗口采集不适用。
- 先启动 OBS，再启动游戏；打开「录制测试标记」。游戏中应显示 `OBS LOCAL ONLY`，OBS 画面中不应显示。
- 分别打开模组 HUD 和设置页确认结果，切换全屏或采集方式后重新检查。录制效果取决于 OBS 与 OpenGL 钩子的安装顺序，不能仅凭设置开关断言成功。

## 移植范围与兼容性

1.21.11 的 GUI 使用延后绘制。本版本将选中的 GUI 命令送往独立队列，在独立纹理上绘制，再通过上游同类的 Windows `wglSwapBuffers` 钩子合成本地画面。直接使用现代 Minecraft RenderPass 绘制到主目标的 HUD 也会重定向。

- **本分支专注 HUD / GUI，不是上游所有功能的等价移植。** 上游世界物体过滤（告示牌、箱子、旗帜、信标、地图、实体名字）、自动隐藏与旧版扩展 API 尚未移植；不提供 NeoForge 构建。上游原始快照保留于 `upstream-1.21.4` 分支。
- 没有为绕开 Minecraft RenderPass / GUI 队列的私有原生 OpenGL 绘制提供兼容保证。
- RusherHack 提供包名与来源 JAR 识别，其闭源付费客户端未进行实际录制验证；修改过的加载器、混淆路径或自定义渲染器仍可能需要适配。
- 尚未完成真实游戏场景下四个模组同时运行的 OBS 端到端录制验收，因此按 **beta** 发布。

## 构建与验证

```sh
./gradlew build
./gradlew runSmoke -x downloadAssets
# Windows x64 上额外测试真实隐藏 OpenGL 窗口、原生钩子、像素与绘制队列：
./gradlew runSmoke -PglSmoke -x downloadAssets
```

`build` 执行包名边界与识别规则测试。`runSmoke` 在真正的 Fabric/Mixin 加载环境中加载所有拦截目标，并在游戏启动前退出。`-PglSmoke` 进一步验证原生合成、采集纹理不变、图形状态恢复，以及四种模拟来源的 HUD 开关隔离。测试模组和第三方模组不会打包进交付 JAR。

详见 [验证记录](VALIDATION.md)。

## 授权与来源

原作者 zziger，原项目 [obs-overlay](https://github.com/zziger/obs-overlay)，MIT 协议见 [LICENSE](LICENSE)。保留上游 MinHook x64 资源。此仓库为独立移植，不代表原作者或所列模组作者提供支持。
