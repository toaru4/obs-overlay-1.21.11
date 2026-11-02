package me.zziger.obsoverlay.port.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.OBSOverlay;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.*;

/** Extraction is separate from GameRenderer.render in 26.3; no GPU calls belong here. */
@Mixin(Gui.class)
public class GuiExtractionMixin {
    @Shadow @Final private GuiRenderState guiRenderState;
    @WrapMethod(method="extractRenderState")
    private void obs$extract(DeltaTracker delta, boolean level, boolean loaded, Operation<Void> original) {
        RenderRouter.mainGuiState=guiRenderState;
        if(OBSOverlay.renderer!=null) OBSOverlay.renderer.beginExtraction();
        RenderRouter.extracting=true;
        try { original.call(delta,level,loaded); }
        finally { RenderRouter.extracting=false; RenderRouter.screen=false; RenderRouter.screenOverlay=false; RenderRouter.vanillaScope=0; }
    }
}
