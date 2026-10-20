package me.zziger.obsoverlay.port.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.zziger.obsoverlay.port.OBSOverlay;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @WrapMethod(method = "render")
    private void obs$renderQueues(GpuBufferSlice fog, Operation<Void> original) {
        if (RenderRouter.renderingOverlay) { original.call(fog); return; }
        boolean extracting = RenderRouter.extracting;
        try {
            // Preparing already extracted states must not route intermediate texture work.
            RenderRouter.extracting = false;
            original.call(fog);
            if (OBSOverlay.renderer != null
                    && ((GuiRendererAccessor)this).obs$getState() == RenderRouter.mainGuiState) {
                OBSOverlay.renderer.renderGui((GuiRenderer)(Object)this, fog);
            }
        } finally { RenderRouter.extracting = extracting; }
    }
}
