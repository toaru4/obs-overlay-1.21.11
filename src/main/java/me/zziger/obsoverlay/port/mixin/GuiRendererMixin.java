package me.zziger.obsoverlay.port.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import java.util.function.Supplier;
import me.zziger.obsoverlay.port.OBSOverlay;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @WrapMethod(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
    private void obs$renderQueues(GpuBufferSlice fog, Operation<Void> original) {
        if (RenderRouter.renderingOverlay) { original.call(fog); return; }
        boolean extracting = RenderRouter.extracting;
        try {
            // Preparing already extracted states must not route intermediate texture work.
            RenderRouter.extracting = false;
            var overlay = OBSOverlay.renderer;
            boolean main = overlay != null && ((GuiRendererAccessor)this).obs$getState() == RenderRouter.mainGuiState;
            // Once the screen starts, pending local commands precede the visible screen batch.
            boolean scene = main && overlay.localScene() != null;
            if (scene) overlay.renderGui((GuiRenderer)(Object)this, fog);
            original.call(fog);
            if (main && !scene) overlay.renderGui((GuiRenderer)(Object)this, fog);
        } finally { RenderRouter.extracting = extracting; }
    }
    @WrapMethod(method = "render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V")
    private void obs$mirrorScreenBatch(Supplier<String> name, Framebuffer target, GpuBufferSlice fog,
            GpuBufferSlice transforms, GpuBuffer indices, VertexFormat.IndexType indexType, int from, int to,
            Operation<Void> original) {
        original.call(name, target, fog, transforms, indices, indexType, from, to);
        var overlay = OBSOverlay.renderer;
        if (overlay != null && ((GuiRendererAccessor)this).obs$getState() == RenderRouter.mainGuiState
                && overlay.mirrorScreen(target)) {
            // Reuse prepared GPU draws. Preparing items/text/special elements twice is unsafe.
            original.call(name, overlay.localScene(), fog, transforms, indices, indexType, from, to);
        }
    }
}
