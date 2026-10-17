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
    private final LocalCompositor compositor;
    private boolean dirty;
    private boolean active;
    public OverlayRenderer() {
        compositor = new LocalCompositor();
        try { NativeSwapHook.install(this::present); }
        catch (RuntimeException | LinkageError e) { compositor.close(); throw e; }
        OBSOverlay.LOGGER.info("OBS Overlay 1.21.11 native overlay initialized");
    }
    public boolean ready() { return active && framebuffer != null; }
    public Framebuffer framebuffer() { return framebuffer; }
    public void markDirty() { dirty = true; }
    public void beginFrame() {
        RenderRouter.extracting = false;
        RenderRouter.screen = false;
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
    }
    public void renderGui(GuiRenderer renderer, GpuBufferSlice fog) {
        if (ready() && OBSOverlay.config.showTestIcon) {
            var client = MinecraftClient.getInstance();
            var context = new net.minecraft.client.gui.DrawContext(client, state, 0, 0);
            context.fill(4, 4, 112, 20, 0xE0008800);
            context.drawTextWithShadow(client.textRenderer, "OBS LOCAL ONLY", 8, 8, 0xFFFFFFFF);
            dirty = true;
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
    }
    private void present() {
        if (ready() && dirty && framebuffer.getColorAttachment() instanceof GlTexture texture) compositor.draw(texture.getGlId(), framebuffer.textureWidth, framebuffer.textureHeight);
    }
    @Override public void close() {
        active = false;
        NativeSwapHook.uninstall();
        if (framebuffer != null) framebuffer.delete();
        compositor.close();
    }
}
