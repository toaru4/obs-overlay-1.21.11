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

public final class OverlayRenderer implements AutoCloseable {
    public final GuiRenderState state = new GuiRenderState();
    private RenderTarget framebuffer;
    private RenderTarget localScene;
    private boolean sceneActive;
    private LocalCompositor compositor;
    private VulkanLocalPresentation vulkan;
    private boolean dirty;
    private boolean active;
    private boolean testIconDrawn;
    private boolean closed;
    private GuiRenderer localGuiRenderer;
    public OverlayRenderer() {}
    public boolean ready() { return !closed && active && framebuffer != null && (vulkan==null || vulkan.ready()); }
    public RenderTarget framebuffer() { return RenderRouter.renderingOverlay && sceneActive ? localScene : framebuffer; }
    public RenderTarget localScene() { return sceneActive ? localScene : null; }
    public boolean mirrorScreen(RenderTarget target) {
        return ready() && sceneActive && !RenderRouter.renderingOverlay
                && target != null && RenderRouter.isScreenTarget(target.getColorTexture());
    }
    public void markDirty() { dirty = true; }
    public void beginExtraction() { state.reset(); dirty=false; }
    public void beginFrame() {
        if(closed) return;
        sceneActive = false;
        RenderRouter.extracting = false;
        RenderRouter.screen = false;
        RenderRouter.screenOverlay = false;
        RenderRouter.renderingOverlay = false;
        RenderRouter.vanillaScope = 0;
        RenderRouter.submittingGui=false;
        RenderRouter.mainGuiDraw=false;
        RenderRouter.meteorScreenQueue=null;
        var window = Minecraft.getInstance().getWindow();
        int width = window.getWidth(), height = window.getHeight();
        active = width > 0 && height > 0;
        if (!active) { hideLocalPresentation(); return; }
        if (RenderRouter.mainColorTexture instanceof GlTexture && compositor == null) {
            compositor = new LocalCompositor();
            try { NativeSwapHook.install(this::present); }
            catch (RuntimeException | LinkageError e) { compositor.close(); compositor=null; throw e; }
        } else if (!(RenderRouter.mainColorTexture instanceof GlTexture) && vulkan == null) {
            if (!RenderSystem.getDevice().getDeviceInfo().backendName().toLowerCase(java.util.Locale.ROOT).contains("vulkan"))
                throw new IllegalStateException("Unsupported GPU backend: "+RenderSystem.getDevice().getDeviceInfo().backendName());
            vulkan = new VulkanLocalPresentation();
            OBSOverlay.LOGGER.warn("Experimental Vulkan local-window composition enabled: windowed/borderless only, 30 FPS readback, OBS acceptance test required");
        }
        if (framebuffer == null) framebuffer = new TextureTarget("OBS local-only overlay", width, height, RenderRouter.mainColorTexture.getFormat(), GpuFormat.D32_FLOAT);
        else if (framebuffer.width != width || framebuffer.height != height) framebuffer.resize(width, height);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorTexture(), new Vector4f(), framebuffer.getDepthTexture(), 0.0);
        if(localGuiRenderer==null) localGuiRenderer=createLocalGuiRenderer();
        testIconDrawn = false;
    }
    /** Snapshot the current world before captured GUI submission, then draw the complete local GUI in original order. */
    public void beginScreenScene() {
        if (!ready() || sceneActive) return;
        int width = framebuffer.width, height = framebuffer.height;
        if (localScene == null) localScene = new TextureTarget("OBS complete local scene", width, height, RenderRouter.mainColorTexture.getFormat(), GpuFormat.D32_FLOAT);
        else if (localScene.width != width || localScene.height != height) localScene.resize(width, height);
        localScene.copyColorFrom(Minecraft.getInstance().gameRenderer.mainRenderTarget());
        sceneActive = true;
        // Direct local-only producers may have drawn before the GUI submission.
        if (dirty) framebuffer.blitAndBlendToTexture(localScene.getColorTextureView(), localScene.getDepthTextureView());
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearDepthTexture(localScene.getDepthTexture(), 0.0);
    }
    public boolean needsLocalScene() { return ready() && (dirty || OBSOverlay.config.showTestIcon || meteorFiltering()); }
    private static boolean meteorFiltering() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("meteor-client") &&
            (OBSOverlay.config.enabled(ModTarget.METEOR,false) || OBSOverlay.config.enabled(ModTarget.METEOR,true) || OBSOverlay.config.hideAllScreens);
    }
    public void hideLocalPresentation() { if(vulkan!=null) vulkan.hide(); }
    public String presentationDescription() {
        return vulkan!=null?"Vulkan: experimental / windowed or borderless / 30 FPS":compositor!=null?"OpenGL: native composition":"Presentation not initialized";
    }
    private GuiRenderer createLocalGuiRenderer() {
        var client=Minecraft.getInstance();
        java.util.List<net.minecraft.client.gui.render.pip.PictureInPictureRenderer<?>> pictures=new java.util.ArrayList<>(java.util.List.of(
            new net.minecraft.client.gui.render.pip.GuiEntityRenderer(client.getEntityRenderDispatcher()),
            new net.minecraft.client.gui.render.pip.GuiSkinRenderer(),
            new net.minecraft.client.gui.render.pip.GuiBookModelRenderer(),
            new net.minecraft.client.gui.render.pip.GuiBannerResultRenderer(client.getAtlasManager()),
            new net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer()));
        GuiRenderer result;
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("meteor-client")) {
            try {
                var banner=Class.forName("meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderer");
                var constructor=java.util.Arrays.stream(banner.getConstructors()).filter(c->c.getParameterCount()==1 && c.getParameterTypes()[0].isInstance(client.getAtlasManager())).findFirst().orElseThrow();
                pictures.add((net.minecraft.client.gui.render.pip.PictureInPictureRenderer<?>)constructor.newInstance(client.getAtlasManager()));
                var type=Class.forName("meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer");
                result=(GuiRenderer)type.getConstructor(GuiRenderState.class,net.minecraft.client.renderer.feature.FeatureRenderDispatcher.class,java.util.List.class)
                    .newInstance(state,client.gameRenderer.featureRenderDispatcher(),pictures);
            } catch(ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Meteor GUI renderer",e); }
        } else result=new GuiRenderer(state,client.gameRenderer.featureRenderDispatcher(),pictures);
        // Panorama textures belong to Minecraft's resource reload lifecycle. A second
        // registration replaces and closes the shared texture without loading its replacement.
        // CubeMap.render looks it up by the same identifier, so the local renderer reuses it.
        return result;
    }
    public void renderGui() {
        if (ready() && OBSOverlay.config.showTestIcon && !testIconDrawn) {
            var client = Minecraft.getInstance();
            var context = new net.minecraft.client.gui.GuiGraphicsExtractor(client, state, 0, 0);
            context.fill(4, 4, 112, 20, 0xE0008800);
            context.text(client.font, "OBS LOCAL ONLY", 8, 8, 0xFFFFFFFF);
            dirty = true;
            testIconDrawn = true;
        }
        if (!ready() || !sceneActive) return;
        try {
            RenderRouter.renderingOverlay = true;
            localGuiRenderer.render();
            localGuiRenderer.endFrame();
        } finally {
            RenderRouter.renderingOverlay = false;
        }
    }
    public void captureLocalScene() { if(vulkan!=null && sceneActive) vulkan.capture(localScene); }
    private void present() {
        if (!ready()) return;
        if (sceneActive) {
            compositor.draw(((GlTexture)localScene.getColorTexture()).glId(), localScene.width, localScene.height, true);
        } else if (dirty && framebuffer.getColorTexture() instanceof GlTexture texture) compositor.draw(texture.glId(), framebuffer.width, framebuffer.height);
    }
    @Override public void close() {
        if(closed) return;
        closed=true;
        active = false;
        NativeSwapHook.uninstall();
        if (framebuffer != null) framebuffer.destroyBuffers();
        if (localScene != null) localScene.destroyBuffers();
        if(compositor!=null) compositor.close();
        if(vulkan!=null) vulkan.close();
        if(localGuiRenderer!=null) { localGuiRenderer.close(); localGuiRenderer=null; }
    }
}
