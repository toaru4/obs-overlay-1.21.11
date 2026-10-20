package me.zziger.obsoverlay.port.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Meteor renders its widgets outside Screen.renderWithTooltip, after vanilla GUI submission. */
@Pseudo
@Mixin(targets = "meteordevelopment.meteorclient.gui.WidgetScreen", remap = false)
public class MeteorWidgetScreenMixin {
    @WrapMethod(method = "renderCustom")
    private void obs$renderCustom(DrawContext context, int mouseX, int mouseY, float delta, Operation<Void> original) {
        try (var scope = RenderRouter.screenScope(RenderRouter.hiddenScreen((Screen)(Object)this))) {
            original.call(context, mouseX, mouseY, delta);
        }
    }
}
