package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlCommandEncoder;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
    @WrapMethod(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;")
    private RenderPass route(Supplier<String> name, GpuTextureView color, OptionalInt clearColor, GpuTextureView depth, OptionalDouble clearDepth, Operation<RenderPass> original) {
        if (OBSOverlay.renderer != null && OBSOverlay.renderer.ready() && (RenderRouter.renderingOverlay || RenderRouter.hiddenProducer())) {
            var main = ((ClientFramebufferAccessor) MinecraftClient.getInstance()).obs$getMainFramebuffer();
            if (color != null && color.texture() == main.getColorAttachment()) {
                var overlay = OBSOverlay.renderer.framebuffer();
                color = overlay.getColorAttachmentView();
                if (depth != null && depth.texture() == main.getDepthAttachment()) depth = overlay.getDepthAttachmentView();
                OBSOverlay.renderer.markDirty();
            }
        }
        return original.call(name, color, clearColor, depth, clearDepth);
    }
}
