package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin {
    @Unique private boolean obs$recording() {
        return RenderRouter.extracting && !RenderRouter.submittingGui && !RenderRouter.renderingOverlay && OBSOverlay.renderer!=null && RenderRouter.isScreenQueue((GuiRenderState)(Object)this);
    }
    @Unique private void obs$hide(CallbackInfo ci) {
        if(RenderRouter.hiddenProducer()) { OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method="addGuiElement",at=@At("HEAD"),cancellable=true)
    private void obs$simple(GuiElementRenderState value,CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.addGuiElement(value); obs$hide(ci); }
    }
    @Inject(method="addText",at=@At("HEAD"),cancellable=true)
    private void obs$text(GuiTextRenderState value,CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.addText(value); obs$hide(ci); }
    }
    @Inject(method="addItem",at=@At("HEAD"),cancellable=true)
    private void obs$item(GuiItemRenderState value,CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.addItem(value); obs$hide(ci); }
    }
    @Inject(method="addPicturesInPictureState",at=@At("HEAD"),cancellable=true)
    private void obs$pip(PictureInPictureRenderState value,CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.addPicturesInPictureState(value); obs$hide(ci); }
    }
    @Inject(method="nextStratum",at=@At("HEAD"),cancellable=true)
    private void obs$layer(CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.nextStratum(); obs$hide(ci); }
    }
    @Inject(method="up",at=@At("HEAD"),cancellable=true)
    private void obs$up(CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.up(); obs$hide(ci); }
    }
    @Inject(method="blurBeforeThisStratum",at=@At("HEAD"),cancellable=true)
    private void obs$blur(CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.blurBeforeThisStratum(); obs$hide(ci); }
    }
    @Inject(method="addBlitToCurrentLayer",at=@At("HEAD"),cancellable=true)
    private void obs$blit(BlitRenderState value,CallbackInfo ci) {
        if(obs$recording()) { OBSOverlay.renderer.state.addBlitToCurrentLayer(value); obs$hide(ci); }
    }
}
