package smoke;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.GpuFormat;
import java.lang.reflect.Proxy;
import java.util.*;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.*;

public final class QueueOrderSmoke {
    static GuiElementRenderState quad() {
        return (GuiElementRenderState)Proxy.newProxyInstance(GuiElementRenderState.class.getClassLoader(),new Class<?>[]{GuiElementRenderState.class},(p,m,a)-> {
            if(m.getName().equals("bounds")) return new ScreenRectangle(0,0,10,10);
            return null;
        });
    }
    static List<GuiElementRenderState> elements(GuiRenderState state) {
        var result=new ArrayList<GuiElementRenderState>();
        state.forEachElement(result::add,GuiRenderState.TraverseRange.ALL);
        return result;
    }
    static void same(List<GuiElementRenderState> actual,GuiElementRenderState... expected) {
        if(actual.size()!=expected.length) throw new AssertionError("Unexpected queue size: "+actual.size());
        for(int i=0;i<expected.length;i++) if(actual.get(i)!=expected[i]) throw new AssertionError("Local ordering changed at "+i);
    }
    public static void run() throws Exception {
        var oldConfig=OBSOverlay.config; var oldRenderer=OBSOverlay.renderer; var oldState=RenderRouter.mainGuiState;
        var renderer=new OverlayRenderer();
        var target=OverlayRenderer.class.getDeclaredField("framebuffer"); target.setAccessible(true);
        target.set(renderer,new RenderTarget("unallocated smoke target",GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT) { @Override public void destroyBuffers() {} });
        var active=OverlayRenderer.class.getDeclaredField("active"); active.setAccessible(true); active.setBoolean(renderer,true);
        OBSOverlay.renderer=renderer; OBSOverlay.config=new OverlayConfig(); OBSOverlay.config.hud.put("meteor",true);
        var captured=new GuiRenderState(); RenderRouter.mainGuiState=captured;
        var normal=quad(); var hidden=quad(); var screen=quad();
        try {
            renderer.beginExtraction(); RenderRouter.extracting=true;
            captured.addGuiElement(normal);
            meteordevelopment.meteorclient.smoketest.Producer.submit(captured,hidden);
            try(var scope=RenderRouter.screenScope(false)) { captured.nextStratum(); captured.addGuiElement(screen); }
            same(elements(captured),normal,screen);
            same(elements(renderer.state),normal,hidden,screen);
            if(!renderer.needsLocalScene()) throw new AssertionError("Hidden HUD did not request local scene");
            var privateMap=new GuiRenderState();
            meteordevelopment.meteorclient.smoketest.Producer.submit(privateMap,hidden);
            same(elements(privateMap),hidden); same(elements(renderer.state),normal,hidden,screen);
            RenderRouter.meteorScreenQueue=false;
            if(RenderRouter.consumeMeteorQueuePolicy()) throw new AssertionError("Visible ClickGUI inherited HUD filtering");
            if(!RenderRouter.consumeMeteorQueuePolicy()) throw new AssertionError("HUD filtering did not resume after ClickGUI");
            renderer.beginExtraction(); captured.reset(); OBSOverlay.config.hud.put("meteor",false);
            RenderRouter.meteorScreenQueue=true;
            if(!RenderRouter.consumeMeteorQueuePolicy() || RenderRouter.consumeMeteorQueuePolicy()) throw new AssertionError("ClickGUI filtering leaked into HUD");
            meteordevelopment.meteorclient.smoketest.Producer.submit(captured,hidden);
            same(elements(captured),hidden); same(elements(renderer.state),hidden);
            RenderRouter.submittingGui=true;
            captured.addGuiElement(normal);
            same(elements(renderer.state),hidden);
            RenderRouter.submittingGui=false;
            if(renderer.needsLocalScene()) throw new AssertionError("Visible-only frame requested local composition");
            System.out.println("QUEUE_ORDER_26_PASS: captured queue excludes hidden HUD, complete local queue preserves HUD-before-screen order, private queue isolation, filter OFF, CPU-only extraction");
        } finally {
            RenderRouter.extracting=false; RenderRouter.submittingGui=false; RenderRouter.meteorScreenQueue=null; renderer.close(); OBSOverlay.renderer=oldRenderer; OBSOverlay.config=oldConfig; RenderRouter.mainGuiState=oldState;
        }
    }
}
