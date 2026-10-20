package smoke;

import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.render.state.*;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

public final class QueueSmoke {
    public static void run() throws Exception {
        OverlayConfig oldConfig = OBSOverlay.config;
        OverlayRenderer oldRenderer = OBSOverlay.renderer;
        GuiRenderState oldMainState = RenderRouter.mainGuiState;
        OverlayRenderer renderer = new OverlayRenderer();
        // No Minecraft client is created in preLaunch. A non-allocated target marks the router ready.
        var framebuffer = OverlayRenderer.class.getDeclaredField("framebuffer"); framebuffer.setAccessible(true);
        framebuffer.set(renderer, new Framebuffer("Queue smoke sentinel", true) { @Override public void delete() {} });
        var active = OverlayRenderer.class.getDeclaredField("active"); active.setAccessible(true); active.setBoolean(renderer, true);
        OBSOverlay.renderer = renderer; OBSOverlay.config = new OverlayConfig();
        RenderRouter.extracting = true;
        try {
            GuiRenderState captured = new GuiRenderState();
            RenderRouter.mainGuiState = captured;
            SimpleGuiElementRenderState quad = new ColoredQuadGuiElementRenderState(RenderPipelines.GUI, TextureSetup.empty(), new Matrix3x2f(), 0, 0, 10, 10, -1, -1, null);
            for (ModTarget target : ModTarget.values()) {
                captured.clear(); renderer.state.clear();
                OBSOverlay.config.hud.replaceAll((key, value) -> false);
                OBSOverlay.config.hud.put(target.id, true);
                Class<?> screenType = switch (target) {
                    case METEOR -> meteordevelopment.meteorclient.smoketest.Producer.class;
                    case RUSHERHACK -> org.rusherhack.smoketest.Producer.class;
                    case XAERO -> xaero.smoketest.Producer.class;
                    case MALILIB -> MaliChild.class;
                };
                OBSOverlay.config.gui.put(target.id, false);
                if (RenderRouter.hiddenScreenType(screenType, false)) throw new AssertionError("HUD switch affected GUI policy");
                submit(target, captured, quad);
                if (count(captured) != 0 || count(renderer.state) != 1) throw new AssertionError(target + " enabled HUD escaped into captured GUI");
                GuiRenderState intermediate = new GuiRenderState();
                submit(target, intermediate, quad);
                if (count(intermediate) != 1 || count(renderer.state) != 1) throw new AssertionError(target + " offscreen texture commands were moved into the screen overlay");
                OBSOverlay.config.hud.put(target.id, false);
                OBSOverlay.config.gui.put(target.id, true);
                if (!RenderRouter.hiddenScreenType(screenType, false)) throw new AssertionError("GUI switch ignored for " + target);
                submit(target, captured, quad);
                if (count(captured) != 1 || count(renderer.state) != 1) throw new AssertionError(target + " disabled HUD was still redirected");
            }
            if (ModTarget.ofScreen(MaliChild.class) != ModTarget.MALILIB) throw new AssertionError("MaLiLib GUI superclass not recognized");
            OBSOverlay.config.hideAllScreens = true;
            if (RenderRouter.hiddenScreenType(QueueSmoke.class, false) || !RenderRouter.hiddenScreenType(QueueSmoke.class, true)) throw new AssertionError("All screens policy affected menu or ignored in-game screens");
            if (RenderRouter.hiddenScreenType(SettingsScreen.class, true)) throw new AssertionError("Overlay settings must stay visible to capture");
            RenderRouter.extracting = false;
            renderer.state.clear(); captured.clear();
            OBSOverlay.config.hud.put("meteor", true);
            submit(ModTarget.METEOR, captured, quad);
            if (count(captured) != 1 || count(renderer.state) != 0) throw new AssertionError("World/non-extraction commands incorrectly routed");
            OBSOverlay.config.hud.put("meteor", false);
            captured.clear(); renderer.state.clear();
            // Meteor renders ClickGUI after the first vanilla submission, outside the normal scope.
            try (var lateScreen = RenderRouter.screenScope(true)) {
                if (!RenderRouter.extracting || !RenderRouter.screen) throw new AssertionError("Late screen extraction not enabled");
                submit(ModTarget.METEOR, captured, quad);
                if (count(captured) != 0 || count(renderer.state) != 1) throw new AssertionError("Late ClickGUI command escaped into capture with HUD disabled");
                GuiRenderState privateTexture = new GuiRenderState();
                submit(ModTarget.METEOR, privateTexture, quad);
                if (count(privateTexture) != 1 || count(renderer.state) != 1) throw new AssertionError("Late screen intercepted a private texture");
                try (var visible = RenderRouter.screenScope(false)) {
                    submit(ModTarget.METEOR, captured, quad);
                    if (count(captured) != 1) throw new AssertionError("Visible nested screen was hidden");
                }
                if (!RenderRouter.hiddenProducer()) throw new AssertionError("Nested scope failed to restore GUI policy");
            }
            if (RenderRouter.extracting || RenderRouter.screen || RenderRouter.screenOverlay) throw new AssertionError("Late screen scope leaked outside GUI drawing");
            try {
                try (var lateScreen = RenderRouter.screenScope(true)) { throw new IllegalStateException("Test callback error"); }
            } catch (IllegalStateException expected) {}
            if (RenderRouter.extracting || RenderRouter.screen) throw new AssertionError("Failed render callback leaked its scope");
        } finally {
            RenderRouter.extracting = false; renderer.close();
            OBSOverlay.config = oldConfig; OBSOverlay.renderer = oldRenderer;
            RenderRouter.mainGuiState = oldMainState;
        }
        System.out.println("QUEUE_SMOKE_PASS: eight independent HUD/GUI policies, ON/OFF routing, MaLiLib superclass, extraction boundary, offscreen queue isolation");
        System.out.println("LATE_GUI_SMOKE_PASS: ClickGUI extraction after vanilla submit, independent HUD toggle, nested scopes and exception cleanup");
    }
    private static int count(GuiRenderState state) {
        int[] count = {0}; state.forEachSimpleElement(value -> count[0]++, GuiRenderState.LayerFilter.ALL); return count[0];
    }
    private static void submit(ModTarget target, GuiRenderState state, SimpleGuiElementRenderState quad) {
        switch (target) {
            case METEOR -> meteordevelopment.meteorclient.smoketest.Producer.submit(state, quad);
            case RUSHERHACK -> org.rusherhack.smoketest.Producer.submit(state, quad);
            case XAERO -> xaero.smoketest.Producer.submit(state, quad);
            case MALILIB -> fi.dy.masa.malilib.smoketest.Producer.submit(state, quad);
        }
    }
    static class MaliChild extends fi.dy.masa.malilib.smoketest.Producer {}
}
