package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.SettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(VideoSettingsScreen.class)
public abstract class VideoOptionsScreenMixin extends OptionsSubScreen {
    protected VideoOptionsScreenMixin(Screen parent, Options options, Component title) { super(parent, options, title); }
    @Inject(method = "addOptions", at = @At("RETURN"))
    private void addOverlayButton(CallbackInfo ci) {
        list.addSmall(Button.builder(Component.translatable("obs_overlay.settings"), b -> minecraft.setScreenAndShow(new SettingsScreen((Screen)(Object)this))).width(310).build(), null);
    }
}
