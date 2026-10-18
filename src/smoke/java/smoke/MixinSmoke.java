package smoke;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
public final class MixinSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        try {
            String[] types = {
                "net.minecraft.client.MinecraftClient", "net.minecraft.client.render.GameRenderer",
                "net.minecraft.client.gui.render.GuiRenderer", "net.minecraft.client.gui.render.state.GuiRenderState",
                "net.minecraft.client.gl.GlCommandEncoder", "net.minecraft.client.gui.screen.Screen",
                "net.minecraft.client.gui.screen.option.VideoOptionsScreen", "net.minecraft.client.gui.hud.InGameHud"
            };
            for (String type : types) {
                Class<?> target = Class.forName(type, false, Thread.currentThread().getContextClassLoader());
                var methods = target.getDeclaredMethods();
                if (java.util.Arrays.stream(methods).noneMatch(m -> m.getName().contains("obs") || m.getName().contains("scope") || m.getName().contains("overlay") || m.getName().contains("Queues") || m.getName().contains("route") || m.getName().contains("simple"))) {
                    throw new AssertionError("Missing overlay mixin on " + type);
                }
                System.out.println("MIXIN_SMOKE_TRANSFORMED: " + type);
            }
            System.out.println("MIXIN_SMOKE_PASS");
            TargetSmoke.run();
            if (Boolean.getBoolean("obs.overlay.glSmoke")) GlSmoke.run();
            System.exit(0);
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
