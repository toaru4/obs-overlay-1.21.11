package me.zziger.obsoverlay.port.mixin;
import me.zziger.obsoverlay.port.SettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(VideoOptionsScreen.class)
public abstract class VideoOptionsScreenMixin extends GameOptionsScreen {
    protected VideoOptionsScreenMixin(Screen parent, GameOptions options, Text title) { super(parent, options, title); }
    @Inject(method = "addOptions", at = @At("RETURN"))
    private void addOverlayButton(CallbackInfo ci) {
        body.addWidgetEntry(ButtonWidget.builder(Text.translatable("obs_overlay.settings"), b -> client.setScreen(new SettingsScreen((Screen)(Object)this))).width(310).build(), null);
    }
}
