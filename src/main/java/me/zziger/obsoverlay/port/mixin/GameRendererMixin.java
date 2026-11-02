package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Inject(method="render",at=@At("HEAD"))
    private void obs$beginRender(CallbackInfo ci) {
        RenderRouter.mainGuiState=gameRenderState.guiRenderState;
        RenderRouter.mainColorTexture=mainRenderTarget.getColorTexture();
        RenderRouter.mainDepthTexture=mainRenderTarget.getDepthTexture();
        OBSOverlay.beginFrame();
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void obs$screenTarget(CallbackInfo ci) {
        RenderRouter.mainColorTexture=mainRenderTarget.getColorTexture();
        RenderRouter.mainDepthTexture=mainRenderTarget.getDepthTexture();
    }
    @WrapOperation(method="processBlurEffect",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/PostChain;process(Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;)V"))
    private void obs$localBlur(PostChain effect,RenderTarget target,GraphicsResourceAllocator allocator,Operation<Void> original) {
        var overlay=OBSOverlay.renderer;
        if(RenderRouter.renderingOverlay && overlay!=null && overlay.ready()) target=overlay.framebuffer();
        original.call(effect,target,allocator);
    }
}
