# Validation — 4.0.0-alpha.2+26.3

Environment: Windows x64, Java 25, Gradle 9.8.0, Loom 1.19.3, Fabric Loader 0.19.5, Fabric API 0.165.0+26.3. Official unobfuscated Minecraft 26.3 client.

## alpha.2 regression verification

- Fixed the reported startup exception: the local renderer no longer re-registers Minecraft-owned panorama textures. The removed call replaced/closed the existing shared texture with an unloaded CubeMapTexture.
- `TextureOwnershipTest`, included in Gradle check, rejects registration/replacement/release of Minecraft-owned textures from production overlay bytecode; its negative control detects the offending alpha.1 call.
- `PANORAMA_OWNERSHIP_PASS` on real RenderPearl Vulkan: existing texture and view survive two local GuiRenderer creations and closures. Repeating the removed registration reproduces the exact uninitialized texture-view exception. This exercises real TextureManager, GuiRenderer and GPU resources, not a complete game/menu launch.
- `build runSmoke -PvkSmoke -PdesktopSmoke -x downloadAssets --offline` passed, including the Vulkan readback, Mixin and queue tests.

## Passed

- Production `build`, including package ownership/policy tests.
- Fabric transformations for Minecraft, GameRenderer, Gui, Hud, GuiRenderer, GuiRenderState, FrontendCommandEncoder, Screen and VideoSettingsScreen.
- Real transformed GUI queues: hidden producer removed from capture queue; full local queue retains HUD-before-screen ordering; private map queue untouched; filter OFF; CPU-only extraction.
- Real Meteor 26.3-15 JAR loaded in isolated `run26_3/mods`: WidgetScreen injection and private MeteorMcGuiRenderer contract checks. This is not an in-world ClickGUI recording test.
- Real SDL3/OpenGL context: native swap hook fired; local backbuffer green; simulated capture texture red and unchanged; tested graphics state restored.
- Real RenderPearl Vulkan device on Intel Iris Xe Graphics: separate capture/local textures, copy and clear commands, native asynchronous screenshot readback; capture red/local green verified. The Vulkan frame-delayed callback queue was advanced through real submissions.
- Win32 software presenter: real hidden HWND, capture affinity 0x11, no-activate/click-through styles, BGRA DIB upload, stale request cancellation and shutdown.
- RenderPearl render-pass replay and attachment-routing contracts were separately tested against the official 26.3 interfaces (including mutable command argument snapshots and unrelated attachment preservation).

Reproduce:

```text
./gradlew build runSmoke -x downloadAssets
./gradlew runSmoke -PglSmoke -x downloadAssets
./gradlew runSmoke -PvkSmoke -PdesktopSmoke -x downloadAssets
```

GPU/native tests require Windows x64 and supported drivers. Default CI runs only the first command. Optional Meteor checks require its matching JAR in `run26_3/mods`; it is not redistributed.

## Still required before a stable release

- Full game launch and OBS game-capture acceptance on both backends, including normal HUD, ClickGUI, vanilla menus, resize, focus changes and mode switching.
- Visible Vulkan presentation, cursor/click alignment, mixed DPI monitors and capture behavior. The hidden-window test does not establish the appearance or exclusion of a visible window.
- Xaero minimap normal/expanded layouts and XaeroPlus transparent world map under actual 26.3 mod versions.
- MaLiLib-derived screens, optional Mod Menu, and RusherHack (no paid binary available for verification).
- Profiling and review of Vulkan full-frame 30 FPS CPU readback latency and performance. Exclusive fullscreen is unsupported; capture exclusion failure disables filtering.

No live OBS acceptance has been completed. This is an experimental development artifact, not a verified stable dual-backend release. The 1.21.11 stable release is unaffected.
