package me.zziger.obsoverlay.port;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OBSOverlay implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("obs_overlay");
    public static OverlayConfig config;
    public static OverlayRenderer renderer;
    private static boolean failed;
    @Override public void onInitializeClient() { config = OverlayConfig.load(); }
    public static void beginFrame() {
        if (failed) return;
        if (renderer == null) {
            try { renderer = new OverlayRenderer(); }
            catch (RuntimeException | LinkageError e) { failed = true; LOGGER.error("OBS Overlay initialization failed", e); return; }
        }
        renderer.beginFrame();
    }
}
