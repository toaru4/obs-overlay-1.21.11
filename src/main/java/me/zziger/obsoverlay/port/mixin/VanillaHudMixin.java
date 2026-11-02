package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Gui.class)
public class VanillaHudMixin {
    @WrapMethod(method = "renderDebugHud")
    private void scope0(GuiGraphicsExtractor context, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("debug"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderChat")
    private void scope1(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("chat"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderPlayerList")
    private void scope2(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("player_list"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderSubtitlesHud")
    private void scope3(GuiGraphicsExtractor context, boolean deferred, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("subtitles"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, deferred); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
    private void scope4(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("scoreboard"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderOverlayMessage")
    private void scope5(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("actionbar"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderTitleAndSubtitle")
    private void scope6(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("title"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderStatusEffectOverlay")
    private void scope7(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("effects"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderMainHud")
    private void scope8(GuiGraphicsExtractor context, DeltaTracker tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("main_hud"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
}

