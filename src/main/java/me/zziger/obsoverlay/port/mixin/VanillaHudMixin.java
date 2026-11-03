package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Hud.class)
public class VanillaHudMixin {
    @WrapMethod(method = "extractDebugOverlay")
    private void scope0(GuiGraphicsExtractor context, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("debug"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractChat")
    private void scope1(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("chat"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractTabList")
    private void scope2(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("player_list"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractSubtitleOverlay")
    private void scope3(GuiGraphicsExtractor context, boolean deferred, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("subtitles"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, deferred); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
    private void scope4(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("scoreboard"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractOverlayMessage")
    private void scope5(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("actionbar"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractTitle")
    private void scope6(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("title"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractEffects")
    private void scope7(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("effects"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "extractHotbarAndDecorations")
    private void scope8(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("main_hud"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
}

