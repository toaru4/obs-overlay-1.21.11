package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private GuiRenderState guiState;
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
