package me.zziger.obsoverlay.port.mixin;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("renderState") GuiRenderState obs$getState();
    @Mutable @Accessor("renderState") void obs$setState(GuiRenderState state);
}
