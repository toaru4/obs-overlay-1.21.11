package me.zziger.obsoverlay.port.mixin;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("state") GuiRenderState obs$getState();
    @Mutable @Accessor("state") void obs$setState(GuiRenderState state);
}
