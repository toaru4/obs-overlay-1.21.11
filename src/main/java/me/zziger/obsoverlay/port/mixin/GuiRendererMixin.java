package me.zziger.obsoverlay.port.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;
import me.zziger.obsoverlay.port.OBSOverlay;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @WrapMethod(method = "render()V")
    private void obs$renderQueues(Operation<Void> original) {
        if (RenderRouter.renderingOverlay) { original.call(); return; }
        boolean extracting = RenderRouter.extracting;
        try {
            // Preparing already extracted states must not route intermediate texture work.
            RenderRouter.extracting = false;
            var overlay = OBSOverlay.renderer;
            boolean main = overlay != null && ((GuiRendererAccessor)this).obs$getState() == RenderRouter.mainGuiState;
            // Once the screen starts, pending local commands precede the visible screen batch.
            boolean scene = main && overlay.localScene() != null;
            if (scene) overlay.renderGui((GuiRenderer)(Object)this);
            original.call();
            if (main && !scene) overlay.renderGui((GuiRenderer)(Object)this);
        } finally { RenderRouter.extracting = extracting; }
    }
    @WrapMethod(method = "executeDrawRange")
    private void obs$mirrorScreenBatch(Supplier<String> name, RenderTarget target, GpuBufferSlice transforms,
            int from, int to, Operation<Void> original) {
        original.call(name, target, transforms, from, to);
        var overlay = OBSOverlay.renderer;
        if (overlay != null && ((GuiRendererAccessor)this).obs$getState() == RenderRouter.mainGuiState && overlay.mirrorScreen(target)) {
            original.call(name, overlay.localScene(), transforms, from, to);
        }
    }
}
