package me.zziger.obsoverlay.port.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.zziger.obsoverlay.port.RenderRouter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Screen.class)
public class ScreenMixin {
    @WrapMethod(method = "renderWithTooltip")
    private void render(DrawContext context, int mouseX, int mouseY, float delta, Operation<Void> original) {
        boolean previous = RenderRouter.screen;
        RenderRouter.screen = true;
        try { original.call(context, mouseX, mouseY, delta); }
        finally { RenderRouter.screen = previous; }
    }
}
