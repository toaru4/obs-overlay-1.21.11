package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void closeOverlay(CallbackInfo ci) { if (OBSOverlay.renderer != null) OBSOverlay.renderer.close(); }
}
