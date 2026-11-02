package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Unique private boolean obs$localFrame;
    @WrapMethod(method="render()V")
    private void obs$renderQueues(Operation<Void> original) {
        if(RenderRouter.renderingOverlay) { original.call(); return; }
        var overlay=OBSOverlay.renderer;
        boolean extracting=RenderRouter.extracting, submitting=RenderRouter.submittingGui;
        try {
            RenderRouter.submittingGui=true;
            RenderRouter.extracting=true;
            if(overlay!=null && overlay.ready() && getClass().getName().equals("meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer")) {
                boolean hidden=RenderRouter.consumeMeteorQueuePolicy();
                try(var scope=RenderRouter.screenScope(hidden)) {
                    RenderRouter.renderingOverlay=hidden;
                    original.call();
                } finally { RenderRouter.renderingOverlay=false; }
                return;
            }
            boolean main=overlay!=null && ((GuiRendererAccessor)this).obs$getState()==RenderRouter.mainGuiState;
            obs$localFrame=main && overlay.needsLocalScene();
            if(obs$localFrame) {
                var state=((GuiRendererAccessor)this).obs$getState();
                overlay.state.panoramaRenderState=state.panoramaRenderState;
                overlay.state.clearColorOverride=state.clearColorOverride;
                overlay.state.isHudHidden=state.isHudHidden;
                overlay.beginScreenScene();
            }
            original.call();
            if(obs$localFrame) overlay.captureLocalScene();
            else if(main) overlay.hideLocalPresentation();
        } finally { obs$localFrame=false; RenderRouter.extracting=extracting; RenderRouter.submittingGui=submitting; }
    }
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/GuiRenderer;draw()V"))
    private void obs$drawLocalAtSameLayer(GuiRenderer renderer,Operation<Void> original) {
        boolean drawing=RenderRouter.mainGuiDraw;
        try { RenderRouter.mainGuiDraw=obs$localFrame; original.call(renderer); }
        finally { RenderRouter.mainGuiDraw=drawing; }
        if(obs$localFrame && !RenderRouter.renderingOverlay) OBSOverlay.renderer.renderGui();
    }
}
