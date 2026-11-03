package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import me.zziger.obsoverlay.port.*;
import org.spongepowered.asm.mixin.Mixin;
/** Historical name retained; the frontend routes both OpenGL and Vulkan. */
@Mixin(FrontendCommandEncoder.class)
public class GlCommandEncoderMixin {
    @WrapMethod(method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;")
    private RenderPass obs$route(RenderPassDescriptor descriptor, Operation<RenderPass> original) {
        var overlay = OBSOverlay.renderer;
        if (overlay == null || !overlay.ready() || !RenderTargetRouter.targets(descriptor, RenderRouter.mainColorTexture)) return original.call(descriptor);
        if (RenderRouter.renderingOverlay || RenderRouter.hiddenProducer()) {
            var target = overlay.localScene()!=null ? overlay.localScene() : overlay.framebuffer();
            overlay.markDirty();
            return original.call(RenderTargetRouter.redirect(descriptor, RenderRouter.mainColorTexture, RenderRouter.mainDepthTexture, target.getColorTextureView(), target.getDepthTextureView()));
        }
        RenderPass pass = original.call(descriptor);
        if (RenderRouter.extracting && !RenderRouter.mainGuiDraw && overlay.localScene() != null) {
            var target = overlay.localScene();
            var local = RenderTargetRouter.redirect(descriptor, RenderRouter.mainColorTexture, RenderRouter.mainDepthTexture, target.getColorTextureView(), target.getDepthTextureView());
            return new MirroredRenderPass(pass, () -> original.call(local));
        }
        return pass;
    }
    @WrapMethod(method = "clearDepthTexture")
    private void obs$mirrorDepthClear(GpuTexture texture, double value, Operation<Void> original) {
        var overlay = OBSOverlay.renderer;
        if (overlay != null && overlay.ready() && RenderRouter.renderingOverlay && texture == RenderRouter.mainDepthTexture) {
            original.call(overlay.framebuffer().getDepthTexture(), value);
            return;
        }
        original.call(texture, value);
        if (overlay != null && overlay.localScene() != null && !RenderRouter.renderingOverlay && texture == RenderRouter.mainDepthTexture) original.call(overlay.localScene().getDepthTexture(), value);
    }
}
