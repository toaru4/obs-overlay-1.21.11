package smoke.mixin;
import smoke.GameLaunchSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public class GameFrameHook {
    @Inject(method="render",at=@At("TAIL"))
    private void smoke$frame(CallbackInfo ci) {
        if(Boolean.getBoolean("obs.overlay.gameSmoke")) GameLaunchSmoke.frame();
    }
}
