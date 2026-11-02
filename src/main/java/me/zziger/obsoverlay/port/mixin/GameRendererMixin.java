package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private GuiRenderState guiState;
    @Shadow @Final private GuiRenderer guiRenderer;
    @Shadow @Final private FogRenderer fogRenderer;
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void obs$beforeScreen(CallbackInfo ci) {
        if (OBSOverlay.renderer == null || !OBSOverlay.renderer.ready()) return;
        // Submit HUD while it still precedes the screen, then fork only the local color.
        guiRenderer.render();
        OBSOverlay.renderer.beginScreenScene();
    }
    @WrapOperation(method = "renderBlur", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;render(Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;)V"))
    private void obs$mirrorBlur(PostChain effect, RenderTarget target, GraphicsResourceAllocator allocator, Operation<Void> original) {
        original.call(effect, target, allocator);
        var overlay = OBSOverlay.renderer;
        if (overlay != null && overlay.mirrorScreen(target)) original.call(effect, overlay.localScene(), allocator);
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void begin(CallbackInfo ci) {
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderRouter.mainColorTexture = main.getColorTexture();
        RenderRouter.mainDepthTexture = main.getDepthTexture();
        RenderRouter.mainGuiState = guiState;
        OBSOverlay.beginFrame();
    }
    @Inject(method = "render", at = @At(value = "NEW", target = "net/minecraft/client/gui/GuiGraphicsExtractor"))
    private void extract(CallbackInfo ci) { RenderRouter.extracting = true; }
    @Inject(method = "render", at = @At("RETURN"))
    private void finish(CallbackInfo ci) { RenderRouter.extracting = false; RenderRouter.screen = false; RenderRouter.screenOverlay = false; }
}
