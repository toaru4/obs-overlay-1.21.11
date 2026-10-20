# Validation — 3.0.0-beta.3+1.21.11

Date: 2026-10-21 (Asia/Shanghai). Environment: Windows x64, Java 21, Gradle 9.2.0, Loom 1.14.10, Yarn 1.21.11+build.4, Fabric Loader 0.18.4, Fabric API 0.141.3+1.21.11.

## Passed

- `build`: production JAR, sources JAR, package boundary/ownership tests.
- `runSmoke -PglSmoke -x downloadAssets`: Fabric loaded and transformed MinecraftClient, GameRenderer, GuiRenderer, GuiRenderState, GlCommandEncoder, Screen, VideoOptionsScreen and InGameHud.
- Real hidden GLFW/OpenGL window: MinHook swap callback executed; overlay pixels reached the local backbuffer; separate simulated capture texture was unchanged; framebuffer bindings, viewport and tested GL capabilities were restored. This test does not start OBS.
- GUI command routing with four synthetic producer packages: all eight HUD/GUI switches remained independent; enabled HUD commands entered the overlay queue; disabled HUD commands stayed in the captured queue; MaLiLib superclass detection and in-game screen policy passed.
- Same smoke run with Meteor Client 1.21.11-86, MaLiLib 0.27.20, Xaero's Minimap 26.6.0 (XaeroLib 1.7.3), and Xaero's World Map 1.47.0 installed in the isolated test directory.
- Inspected production JAR: Mixin targets remapped to intermediary Minecraft names; native DLL and both licenses present; synthetic producers and smoke entrypoints absent.

Final test markers:

```text
POLICY_TEST_PASS
MIXIN_SMOKE_PASS
GL_SMOKE_PASS
QUEUE_SMOKE_PASS: eight independent HUD/GUI policies, ON/OFF routing, MaLiLib superclass, extraction boundary
BUILD SUCCESSFUL
```

## Limits

The smoke process exits before the real Minecraft client/world starts. Loading third-party mixins successfully is not an end-to-end test of their live HUDs or screens. The queue policy tests use synthetic producers, not the actual HUD implementation of each mod. RusherHack was not installed or executed. Actual OBS capture ordering, private/native mod rendering, shader/renderer combinations, gameplay, resolution changes and fullscreen transitions still require manual acceptance testing.

This is a HUD/GUI-focused Fabric beta. It does not implement the upstream world-object filters, auto-hide feature, legacy extension API, or NeoForge build. These limits are stated in the README and release notes.

## Beta 2: Xaero minimap target regression

User report: the ordinary corner minimap became enlarged and appeared at screen center when Xaero HUD exclusion was enabled; disabling the toggle restored it.

Source inspection confirmed that Xaero's `ImprovedFramebuffer.bindAsMainTarget` temporarily replaces `MinecraftClient.framebuffer` while rendering the map's scaling and rotation textures. Beta 1 incorrectly used that mutable field to identify the screen, and globally overrode the framebuffer getter during overlay drawing. Both could send intermediate map rendering to the full-size overlay.

Beta 2 snapshots the screen color/depth attachments at frame start, redirects only passes targeting that captured color attachment, and leaves the game's framebuffer getter unchanged. GUI command routing is restricted to GameRenderer's screen queue, preserving private texture queues.

Regression coverage: `TARGET_SMOKE_PASS` checks stable screen identity across scaling/rotation/PiP target replacements; `QUEUE_SMOKE_PASS` also checks offscreen GUI queue isolation for all four producers. These are automated regression models; the user's actual Xaero gameplay/OBS result still needs confirmation.

## Beta 3: XaeroPlus transparent world map

The screenshot showed map colors washed out while the world remained visible through the map background. XaeroLib's `xaerolib:pipeline/map_frame` uses RGB copy with destination-alpha preservation (ONE/ZERO, ZERO/ONE), also used by XaeroPlus transparency. The overlay starts with zero alpha, so this leaves nonzero map RGB with zero coverage. Premultiplied composition then incorrectly adds the world color to the map.

The encoder now recognizes passes actually targeting the local overlay, and replaces that exact map-frame blend with premultiplied source-over. Normal screen passes, intermediate textures, map-generation pipelines and unrelated blend modes keep their original behavior. Pass transitions invalidate the cached pipeline state.

Added tests: `BLEND_SMOKE_PASS` verifies the narrow blend policy; `TRANSPARENCY_GL_PASS` first reproduces the additive brightening with the old factors, then checks opaque map pixels, half-alpha edges and untouched world pixels with the correction. The hidden-window GPU test does not run the full game/OBS scene. Smoke loading additionally includes XaeroPlus 2.37.0.
