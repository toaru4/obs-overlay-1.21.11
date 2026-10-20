package me.zziger.obsoverlay.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.render.state.GuiRenderState;
import com.mojang.blaze3d.textures.GpuTexture;

/** Classify commands during extraction, before deferred GUI drawing loses the producer stack. */
public final class RenderRouter {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static boolean extracting;
    public static boolean screen;
    public static boolean screenOverlay;
    public static boolean renderingOverlay;
    public static int vanillaScope;
    public static ScreenScope screenScope(boolean hidden) { return new ScreenScope(hidden); }
    /** Screen renderers may run after vanilla GUI submission (e.g. Meteor renderCustom). */
    public static final class ScreenScope implements AutoCloseable {
        private final boolean previousExtracting = extracting;
        private final boolean previousScreen = screen;
        private final boolean previousOverlay = screenOverlay;
        private ScreenScope(boolean hidden) { extracting = true; screen = true; screenOverlay = hidden; }
        @Override public void close() { extracting = previousExtracting; screen = previousScreen; screenOverlay = previousOverlay; }
    }
    // Only commands destined for the game's screen may be moved to the local overlay.
    // Mods also use GuiRenderState for intermediate textures with their own coordinates.
    public static GuiRenderState mainGuiState;
    public static boolean isScreenQueue(GuiRenderState state) { return state == mainGuiState; }
    // Xaero temporarily replaces MinecraftClient.framebuffer while generating its map.
    // Snapshot the actual screen attachments before any mod starts drawing this frame.
    public static GpuTexture mainColorTexture;
    public static GpuTexture mainDepthTexture;
    public static boolean isScreenTarget(GpuTexture texture) { return texture != null && texture == mainColorTexture; }
    public static boolean hiddenScreen(Screen value) {
        return value != null && hiddenScreenType(value.getClass(), MinecraftClient.getInstance().world != null);
    }
    public static boolean hiddenScreenType(Class<?> type, boolean inWorld) {
        if (SettingsScreen.class.isAssignableFrom(type)) return false;
        if (OBSOverlay.config.hideAllScreens && inWorld) return true;
        ModTarget target = ModTarget.ofScreen(type);
        return target != null && OBSOverlay.config.enabled(target, true);
    }
    public static boolean hiddenProducer() {
        if (!extracting || renderingOverlay || OBSOverlay.renderer == null || !OBSOverlay.renderer.ready()) return false;
        if (screen) return screenOverlay;
        if (vanillaScope > 0) return true;
        OverlayConfig config = OBSOverlay.config;
        if (!config.anyHud()) return false;
        return WALKER.walk(frames -> frames.anyMatch(frame -> {
            ModTarget target = ModTarget.ofClass(frame.getDeclaringClass());
            return target != null && config.enabled(target, false);
        }));
    }
}
