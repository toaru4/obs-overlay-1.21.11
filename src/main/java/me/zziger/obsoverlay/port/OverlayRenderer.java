package me.zziger.obsoverlay.port;

import com.mojang.renderpearl.api.GpuFormat;
import org.joml.Vector4f;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import me.zziger.obsoverlay.port.mixin.GuiRendererAccessor;

public final class OverlayRenderer implements AutoCloseable {
    public final GuiRenderState state = new GuiRenderState();
    private RenderTarget framebuffer;
    private RenderTarget localScene;
    private boolean sceneActive;
    private final LocalCompositor compositor;
    private boolean dirty;
    private boolean active;
    private boolean testIconDrawn;
    public OverlayRenderer() {
        compositor = new LocalCompositor();
        try { NativeSwapHook.install(this::present); }
        catch (RuntimeException | LinkageError e) { compositor.close(); throw e; }
        OBSOverlay.LOGGER.info("OBS Overlay 1.21.11 native overlay initialized");
    }
    public boolean ready() { return active && framebuffer != null; }
    public RenderTarget framebuffer() { return framebuffer; }
    public RenderTarget localScene() { return sceneActive ? localScene : null; }
    public boolean mirrorScreen(RenderTarget target) {
        return ready() && sceneActive && !RenderRouter.renderingOverlay
                && target != null && RenderRouter.isScreenTarget(target.getColorTexture());
    }
    public void markDirty() { dirty = true; }
    public void beginFrame() {
        sceneActive = false;
        RenderRouter.extracting = false;
        RenderRouter.screen = false;
        RenderRouter.screenOverlay = false;
        RenderRouter.renderingOverlay = false;
        RenderRouter.vanillaScope = 0;
        var window = Minecraft.getInstance().getWindow();
        int width = window.getWidth(), height = window.getHeight();
        active = width > 0 && height > 0;
        if (!active) return;
        if (framebuffer == null) framebuffer = new TextureTarget("OBS local-only overlay", width, height, RenderRouter.mainColorTexture.getFormat(), GpuFormat.D32_FLOAT);
        else if (framebuffer.width != width || framebuffer.height != height) framebuffer.resize(width, height);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorTexture(), new Vector4f(), framebuffer.getDepthTexture(), 1.0);
        state.reset();
        dirty = false;
        testIconDrawn = false;
    }
    /** Called after HUD submission, before extracting the first screen. Never writes the captured color. */
    public void beginScreenScene() {
        if (!ready() || sceneActive) return;
        int width = framebuffer.width, height = framebuffer.height;
        if (localScene == null) localScene = new TextureTarget("OBS complete local scene", width, height, RenderRouter.mainColorTexture.getFormat(), GpuFormat.D32_FLOAT);
        else if (localScene.width != width || localScene.height != height) localScene.resize(width, height);
        localScene.copyColorFrom(Minecraft.getInstance().gameRenderer.mainRenderTarget());
        sceneActive = true;
        mergeOverlay();
        // The screen starts a new submission, so its depth must not collide with the flushed HUD.
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(RenderRouter.mainDepthTexture, 1.0);
        encoder.clearDepthTexture(localScene.getDepthTexture(), 1.0);
    }
    private void mergeOverlay() {
        if (!sceneActive || !dirty) return;
        framebuffer.blitAndBlendToTexture(localScene.getColorTextureView(), localScene.getDepthTextureView());
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorTexture(), new Vector4f(), framebuffer.getDepthTexture(), 1.0);
        dirty = false;
    }
    public void renderGui(GuiRenderer renderer) {
        if (ready() && OBSOverlay.config.showTestIcon && !testIconDrawn) {
            var client = Minecraft.getInstance();
            var context = new net.minecraft.client.gui.GuiGraphicsExtractor(client, state, 0, 0);
            context.fill(4, 4, 112, 20, 0xE0008800);
            context.text(client.font, "OBS LOCAL ONLY", 8, 8, 0xFFFFFFFF);
            dirty = true;
            testIconDrawn = true;
        }
        if (!ready() || !dirty) return;
        GuiRendererAccessor access = (GuiRendererAccessor) renderer;
        GuiRenderState original = access.obs$getState();
        try {
            RenderRouter.renderingOverlay = true;
            access.obs$setState(state);
            renderer.render();
        } finally {
            access.obs$setState(original);
            RenderRouter.renderingOverlay = false;
        }
        mergeOverlay();
    }
    private void present() {
        if (!ready()) return;
        if (sceneActive) {
            mergeOverlay();
            compositor.draw(((GlTexture)localScene.getColorTexture()).glId(), localScene.width, localScene.height, true);
        } else if (dirty && framebuffer.getColorTexture() instanceof GlTexture texture) compositor.draw(texture.glId(), framebuffer.width, framebuffer.height);
    }
    @Override public void close() {
        active = false;
        NativeSwapHook.uninstall();
        if (framebuffer != null) framebuffer.destroyBuffers();
        if (localScene != null) localScene.destroyBuffers();
        compositor.close();
    }
}
