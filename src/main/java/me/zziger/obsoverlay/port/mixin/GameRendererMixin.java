package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void begin(CallbackInfo ci) { OBSOverlay.beginFrame(); }
    @Inject(method = "render", at = @At(value = "NEW", target = "net/minecraft/client/gui/DrawContext"))
    private void extract(CallbackInfo ci) { RenderRouter.extracting = true; }
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void renderQueues(GuiRenderer renderer, GpuBufferSlice fog, Operation<Void> original) {
        RenderRouter.extracting = false;
        original.call(renderer, fog);
        if (OBSOverlay.renderer != null) OBSOverlay.renderer.renderGui(renderer, fog);
    }
    @Inject(method = "render", at = @At("RETURN"))
    private void finish(CallbackInfo ci) { RenderRouter.extracting = false; RenderRouter.screen = false; }
}
