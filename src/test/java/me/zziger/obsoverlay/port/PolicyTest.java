package me.zziger.obsoverlay.port;
public final class PolicyTest {
    public static void main(String[] args) {
        expect(ModTarget.METEOR, ModTarget.ofClassName("meteordevelopment.meteorclient.renderer.Renderer2D"));
        expect(ModTarget.RUSHERHACK, ModTarget.ofClassName("org.rusherhack.client.api.ui.hud.HudHandlerBase"));
        expect(ModTarget.XAERO, ModTarget.ofClassName("xaero.common.minimap.render.MinimapRenderer"));
        expect(ModTarget.XAERO, ModTarget.ofClassName("xaero.map.gui.GuiMap"));
        expect(ModTarget.MALILIB, ModTarget.ofClassName("fi.dy.masa.malilib.gui.GuiBase"));
        expect(null, ModTarget.ofClassName("fi.dy.masa.malilibfake.GuiBase"));
        expect(null, ModTarget.ofClassName("net.minecraft.client.gui.screen.Screen"));
        expect(ModTarget.RUSHERHACK, ModTarget.ofSource("file:/mods/rusherhack-1.21.11-release.jar"));
        expect(null, ModTarget.ofSource("file:/rusherhack/minecraft.jar"));
        expect(null, ModTarget.ofSource("file:/mods/rusherhack/"));
        expect(null, ModTarget.ofScreen(PolicyTest.class));
        System.out.println("POLICY_TEST_PASS: package boundaries, both Xaero maps, RusherHack jar fallback");
    }
    private static void expect(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
