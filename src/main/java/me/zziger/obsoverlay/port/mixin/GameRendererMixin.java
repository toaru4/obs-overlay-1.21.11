package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.ObjectAllocator;
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
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void obs$beforeScreen(CallbackInfo ci) {
        if (OBSOverlay.renderer == null || !OBSOverlay.renderer.ready()) return;
        // Submit HUD while it still precedes the screen, then fork only the local color.
        guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
        OBSOverlay.renderer.beginScreenScene();
    }
    @WrapOperation(method = "renderBlur", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(Lnet/minecraft/client/gl/Framebuffer;Lnet/minecraft/client/util/ObjectAllocator;)V"))
    private void obs$mirrorBlur(PostEffectProcessor effect, Framebuffer target, ObjectAllocator allocator, Operation<Void> original) {
        original.call(effect, target, allocator);
        var overlay = OBSOverlay.renderer;
        if (overlay != null && overlay.mirrorScreen(target)) original.call(effect, overlay.localScene(), allocator);
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void begin(CallbackInfo ci) {
        var main = MinecraftClient.getInstance().getFramebuffer();
        RenderRouter.mainColorTexture = main.getColorAttachment();
        RenderRouter.mainDepthTexture = main.getDepthAttachment();
        RenderRouter.mainGuiState = guiState;
        OBSOverlay.beginFrame();
    }
    @Inject(method = "render", at = @At(value = "NEW", target = "net/minecraft/client/gui/DrawContext"))
    private void extract(CallbackInfo ci) { RenderRouter.extracting = true; }
    @Inject(method = "render", at = @At("RETURN"))
    private void finish(CallbackInfo ci) { RenderRouter.extracting = false; RenderRouter.screen = false; RenderRouter.screenOverlay = false; }
}
