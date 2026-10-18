package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.render.state.*;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderState.class)
public class GuiRenderStateMixin {
    @Unique private boolean route() {
        return RenderRouter.isScreenQueue((GuiRenderState)(Object)this) && RenderRouter.hiddenProducer();
    }
    @Inject(method = "addSimpleElement", at = @At("HEAD"), cancellable = true)
    private void simple(SimpleGuiElementRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addSimpleElement(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addText", at = @At("HEAD"), cancellable = true)
    private void text(TextGuiElementRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addText(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private void item(ItemGuiElementRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addItem(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addSpecialElement", at = @At("HEAD"), cancellable = true)
    private void special(SpecialGuiElementRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addSpecialElement(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "createNewRootLayer", at = @At("HEAD"), cancellable = true)
    private void layer(CallbackInfo ci) { if (route()) { OBSOverlay.renderer.state.createNewRootLayer(); ci.cancel(); } }
    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void blur(CallbackInfo ci) {
        // A local-only screen must never blur the captured world's main framebuffer.
        if (route() || RenderRouter.renderingOverlay) ci.cancel();
    }
}
