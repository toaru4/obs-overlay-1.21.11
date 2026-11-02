# Validation — 4.0.0-alpha.3+26.3

Environment: Windows x64, Java 25, Gradle 9.8.0, Loom 1.19.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3. Official unobfuscated Minecraft 26.3 client.

## alpha.3 validation

- Local native presentation now uses retained-frame WM_PAINT, BeginPaint/EndPaint, GdiFlush around shared bitmap access, and geometry updates only on actual changes. Native callback failures hide the window and disable filtering.
- Visible synthetic-window test passed: protected window DC contains green pixels; desktop capture sees the blue owner while exclusion is enabled; with exclusion temporarily disabled for synthetic pixels, green/red desktop pixels, invalidation repaint, physical resize and hide were verified. No user game pixels were exposed by this test.
- Real DefaultFluidRenderer class transformation and JVM verification passed with official Sodium 0.9.2+mc26.3 and Meteor 26.3-15. Sodium 0.9.3-beta.1 is excluded from supported combinations because of the upstream Meteor integration failure; no Meteor binary patch or automatic disabling of its Xray was used.
- Full client menu and temporary integrated world tested with Meteor 26.3-15, Sodium 0.9.2, Singularity 23, ImmediatelyFast 1.17.1, Fabric API 0.165.0 and Loader 0.20.1 on Intel Iris Xe / Vulkan. The test placed water/lava and saved capture/local screenshots. The marker is disabled for the world check. An OpenGL menu/world baseline with Meteor + Sodium 0.9.2 also passed before the Windows presentation changes.
- Repeated the full Vulkan menu/world test without Sodium (Meteor + Singularity + ImmediatelyFast); it also passed with nonblack local and capture frames.
- Full build, policy, resource ownership, Mixin, native window and real Vulkan readback tests passed.
- A repeatable reproduction of the user's intermittent physical black screen was not established. Tests establish the repaired repaint/synchronization behavior and healthy rendered frames, not universal display or live OBS acceptance.

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

- Live OBS game-capture acceptance on both backends, including ClickGUI, focus/mode changes and the original intermittent black-screen scenario. Full menu/local-world rendering is now tested separately.
- Actual game-window physical presentation and cursor/click alignment under recording, mixed DPI monitors and mode changes. Synthetic visible-window repaint/resize tests are separate from live gameplay acceptance.
- Xaero minimap normal/expanded layouts and XaeroPlus transparent world map under actual 26.3 mod versions.
- MaLiLib-derived screens, optional Mod Menu, and RusherHack (no paid binary available for verification).
- Profiling and review of Vulkan full-frame 30 FPS CPU readback latency and performance. Exclusive fullscreen is unsupported; capture exclusion failure disables filtering.

No live OBS acceptance has been completed. This is an experimental development artifact, not a verified stable dual-backend release. The 1.21.11 stable release is unaffected.
