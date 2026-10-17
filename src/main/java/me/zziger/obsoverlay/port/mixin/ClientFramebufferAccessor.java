package me.zziger.obsoverlay.port.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MinecraftClient.class)
public interface ClientFramebufferAccessor {
    @Accessor("framebuffer") Framebuffer obs$getMainFramebuffer();
}
