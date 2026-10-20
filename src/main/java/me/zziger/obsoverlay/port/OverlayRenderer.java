package me.zziger.obsoverlay.port;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.GlTexture;
import me.zziger.obsoverlay.port.mixin.GuiRendererAccessor;

public final class OverlayRenderer implements AutoCloseable {
    public final GuiRenderState state = new GuiRenderState();
    private Framebuffer framebuffer;
    private Framebuffer localScene;
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
    public Framebuffer framebuffer() { return framebuffer; }
    public Framebuffer localScene() { return sceneActive ? localScene : null; }
    public boolean mirrorScreen(Framebuffer target) {
        return ready() && sceneActive && !RenderRouter.renderingOverlay
                && target != null && RenderRouter.isScreenTarget(target.getColorAttachment());
    }
    public void markDirty() { dirty = true; }
    public void beginFrame() {
        sceneActive = false;
        RenderRouter.extracting = false;
        RenderRouter.screen = false;
        RenderRouter.screenOverlay = false;
        RenderRouter.renderingOverlay = false;
        RenderRouter.vanillaScope = 0;
        var window = MinecraftClient.getInstance().getWindow();
        int width = window.getFramebufferWidth(), height = window.getFramebufferHeight();
        active = width > 0 && height > 0;
        if (!active) return;
        if (framebuffer == null) framebuffer = new SimpleFramebuffer("OBS local-only overlay", width, height, true);
        else if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) framebuffer.resize(width, height);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorAttachment(), 0, framebuffer.getDepthAttachment(), 1.0);
        state.clear();
        dirty = false;
        testIconDrawn = false;
    }
    /** Called after HUD submission, before extracting the first screen. Never writes the captured color. */
    public void beginScreenScene() {
        if (!ready() || sceneActive) return;
        int width = framebuffer.textureWidth, height = framebuffer.textureHeight;
        if (localScene == null) localScene = new SimpleFramebuffer("OBS complete local scene", width, height, true);
        else if (localScene.textureWidth != width || localScene.textureHeight != height) localScene.resize(width, height);
        compositor.drawToTexture(((GlTexture)RenderRouter.mainColorTexture).getGlId(),
                ((GlTexture)localScene.getColorAttachment()).getGlId(), width, height, true);
        sceneActive = true;
        mergeOverlay();
        // The screen starts a new submission, so its depth must not collide with the flushed HUD.
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(RenderRouter.mainDepthTexture, 1.0);
        encoder.clearDepthTexture(localScene.getDepthAttachment(), 1.0);
    }
    private void mergeOverlay() {
        if (!sceneActive || !dirty) return;
        compositor.drawToTexture(((GlTexture)framebuffer.getColorAttachment()).getGlId(),
                ((GlTexture)localScene.getColorAttachment()).getGlId(), framebuffer.textureWidth, framebuffer.textureHeight, false);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorAttachment(), 0, framebuffer.getDepthAttachment(), 1.0);
        dirty = false;
    }
    public void renderGui(GuiRenderer renderer, GpuBufferSlice fog) {
        if (ready() && OBSOverlay.config.showTestIcon && !testIconDrawn) {
            var client = MinecraftClient.getInstance();
            var context = new net.minecraft.client.gui.DrawContext(client, state, 0, 0);
            context.fill(4, 4, 112, 20, 0xE0008800);
            context.drawTextWithShadow(client.textRenderer, "OBS LOCAL ONLY", 8, 8, 0xFFFFFFFF);
            dirty = true;
            testIconDrawn = true;
        }
        if (!ready() || !dirty) return;
        GuiRendererAccessor access = (GuiRendererAccessor) renderer;
        GuiRenderState original = access.obs$getState();
        var oldColor = RenderSystem.outputColorTextureOverride;
        var oldDepth = RenderSystem.outputDepthTextureOverride;
        try {
            RenderRouter.renderingOverlay = true;
            access.obs$setState(state);
            renderer.render(fog);
        } finally {
            RenderSystem.outputColorTextureOverride = oldColor;
            RenderSystem.outputDepthTextureOverride = oldDepth;
            access.obs$setState(original);
            RenderRouter.renderingOverlay = false;
        }
        mergeOverlay();
    }
    private void present() {
        if (!ready()) return;
        if (sceneActive) {
            mergeOverlay();
            compositor.draw(((GlTexture)localScene.getColorAttachment()).getGlId(), localScene.textureWidth, localScene.textureHeight, true);
        } else if (dirty && framebuffer.getColorAttachment() instanceof GlTexture texture) compositor.draw(texture.getGlId(), framebuffer.textureWidth, framebuffer.textureHeight);
    }
    @Override public void close() {
        active = false;
        NativeSwapHook.uninstall();
        if (framebuffer != null) framebuffer.delete();
        if (localScene != null) localScene.delete();
        compositor.close();
    }
}
