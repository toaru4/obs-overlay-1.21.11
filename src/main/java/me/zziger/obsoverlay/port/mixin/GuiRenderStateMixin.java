package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
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
    @Inject(method = "addGuiElement", at = @At("HEAD"), cancellable = true)
    private void simple(GuiElementRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addGuiElement(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addText", at = @At("HEAD"), cancellable = true)
    private void text(GuiTextRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addText(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private void item(GuiItemRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addItem(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "addPicturesInPictureState", at = @At("HEAD"), cancellable = true)
    private void special(PictureInPictureRenderState value, CallbackInfo ci) {
        if (route()) { OBSOverlay.renderer.state.addPicturesInPictureState(value); OBSOverlay.renderer.markDirty(); ci.cancel(); }
    }
    @Inject(method = "nextStratum", at = @At("HEAD"), cancellable = true)
    private void layer(CallbackInfo ci) { if (route()) { OBSOverlay.renderer.state.nextStratum(); ci.cancel(); } }
    @Inject(method = "blurBeforeThisStratum", at = @At("HEAD"), cancellable = true)
    private void blur(CallbackInfo ci) {
        // A local-only screen must never blur the captured world's main framebuffer.
        if (route() || RenderRouter.renderingOverlay) ci.cancel();
    }
}
