package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(InGameHud.class)
public class VanillaHudMixin {
    @WrapMethod(method = "renderDebugHud")
    private void scope0(DrawContext context, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("debug"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderChat")
    private void scope1(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("chat"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderPlayerList")
    private void scope2(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("player_list"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderSubtitlesHud")
    private void scope3(DrawContext context, boolean deferred, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("subtitles"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, deferred); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V")
    private void scope4(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("scoreboard"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderOverlayMessage")
    private void scope5(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("actionbar"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderTitleAndSubtitle")
    private void scope6(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("title"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderStatusEffectOverlay")
    private void scope7(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("effects"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
    @WrapMethod(method = "renderMainHud")
    private void scope8(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hidden = Boolean.TRUE.equals(OBSOverlay.config.vanilla.get("main_hud"));
        if (hidden) RenderRouter.vanillaScope++;
        try { original.call(context, tickCounter); }
        finally { if (hidden) RenderRouter.vanillaScope--; }
    }
}

