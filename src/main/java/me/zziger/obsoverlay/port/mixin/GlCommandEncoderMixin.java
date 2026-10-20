package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gl.GlCommandEncoder;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
    @Shadow private RenderPipeline currentPipeline;
    @Unique private boolean obs$overlayPass;
    @WrapMethod(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;")
    private RenderPass route(Supplier<String> name, GpuTextureView color, OptionalInt clearColor, GpuTextureView depth, OptionalDouble clearDepth, Operation<RenderPass> original) {
        if (OBSOverlay.renderer != null && OBSOverlay.renderer.ready()
                && color != null && RenderRouter.isScreenTarget(color.texture())
                && (RenderRouter.renderingOverlay || RenderRouter.hiddenProducer())) {
                var overlay = OBSOverlay.renderer.framebuffer();
                color = overlay.getColorAttachmentView();
                if (depth != null && depth.texture() == RenderRouter.mainDepthTexture) depth = overlay.getDepthAttachmentView();
                OBSOverlay.renderer.markDirty();
        }
        RenderPass pass = original.call(name, color, clearColor, depth, clearDepth);
        boolean overlay = OBSOverlay.renderer != null && OBSOverlay.renderer.ready() && color != null
                && color.texture() == OBSOverlay.renderer.framebuffer().getColorAttachment();
        if (overlay != obs$overlayPass) currentPipeline = null;
        obs$overlayPass = overlay;
        return pass;
    }
    @WrapOperation(method = "setPipelineAndApplyState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBlendFunction()Ljava/util/Optional;", remap = false))
    private Optional<BlendFunction> overlayBlend(RenderPipeline pipeline, Operation<Optional<BlendFunction>> original) {
        return OverlayBlendPolicy.adjust(pipeline.getLocation().toString(), original.call(pipeline), obs$overlayPass);
    }
    @Inject(method = "closePass", at = @At("RETURN"))
    private void closeOverlayPass(CallbackInfo ci) {
        if (obs$overlayPass) currentPipeline = null;
        obs$overlayPass = false;
    }
}
