# Meteor 26.3-15 — Sodium 0.9.3-beta.1 compatibility patch

This directory is a GPL-3.0-only derivative of Meteor Client (see LICENSE). It is separate from the MIT-licensed OBS Overlay code.

Base: Meteor commit `e3a2e77967629d216e127b5ceecf71fb15f9d2e5` / official build `26.3-15`. The patch changes only `SodiumDefaultFluidRendererMixin.class` and provenance metadata in a copy of the original Meteor JAR. Other bytecode, nested dependencies, resources and upstream license are retained. The changed Java source is embedded in the patched JAR. The displayed mod name identifies the compatibility patch; the base version stays unchanged for addons.

## Fix

1. Remove `@Local(otherState)` from the full-block fluid-side return handler. Read the same neighbor from stable method arguments after the original/Xray/vertical guards. This avoids invalid return-site local frames and the reported VerifyError.
2. Add the Sodium 0.9.3 fluid-side exposure descriptor and handler. Its world/neighbor position arguments replace the old neighbor-state argument. Retain the 0.9.2 handler as an optional branch; a Mixin injection group requires at least one branch to actually inject.
3. Preserve Xray visibility decisions, fluid alpha, quad colors and translucent material selection. No Xray disablement, blanket Mixin suppression, or Sodium downgrade is used.

## Build

Use Java 25 and the repository Gradle wrapper:

```text
./gradlew patchMeteorSodium -PmeteorCompatJar=/path/to/original/meteor-client-26.3-15.jar -PsodiumCompatJar=/path/to/sodium-fabric-0.9.3-beta.1+mc26.3.jar -PcompatPython=python3
```

Output: `build/compat/meteor-client-26.3-15-sodium093-compat1.jar`.

The tool requires the original build-15 mixin hash and refuses unknown/custom versions or signed archives. It never overwrites its input. After building, it verifies that only the intended class and metadata were changed.

## Install

Replace the original Meteor JAR with the compatibility JAR; keep exactly one active Meteor JAR. Use Sodium **0.9.3-beta.1+mc26.3**. OBS Overlay alpha.3 stays unchanged. Keep your existing Meteor configuration. An update that replaces this patched Meteor JAR also removes the patch.

## Validation

The unpatched original reproduces the missing Sodium method injection error. Patched Meteor passes real DefaultFluidRenderer transformation and JVM verification with beta.1, as well as the legacy 0.9.2 selector branch.

Vulkan and OpenGL full-client tests on Intel Iris Xe passed. Each isolated test enters an integrated world containing water/lava, enables Xray with fluid alpha 64, renders and captures local/capture frames, then cleanly exits. Reproduce with the matching two JARs in `run26_3/mods`, downloaded Minecraft assets, and:

```text
./gradlew runSmoke -PgameSmoke -PsodiumCompatSmoke -x downloadAssets
```

The test asserts the actual loaded Sodium beta.1 version and Meteor patch metadata before starting. This validates the reported fluid-renderer conflict; it is not a claim of compatibility with every addon, shaders, future Sodium/Meteor release, or live OBS capture setup.
