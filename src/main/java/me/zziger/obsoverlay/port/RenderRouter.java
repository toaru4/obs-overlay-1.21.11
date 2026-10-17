package me.zziger.obsoverlay.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Classify commands during extraction, before deferred GUI drawing loses the producer stack. */
public final class RenderRouter {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static boolean extracting;
    public static boolean screen;
    public static boolean renderingOverlay;
    public static int vanillaScope;
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
        if (screen) return hiddenScreen(MinecraftClient.getInstance().currentScreen);
        if (vanillaScope > 0) return true;
        OverlayConfig config = OBSOverlay.config;
        if (!config.anyHud()) return false;
        return WALKER.walk(frames -> frames.anyMatch(frame -> {
            ModTarget target = ModTarget.ofClass(frame.getDeclaringClass());
            return target != null && config.enabled(target, false);
        }));
    }
}
