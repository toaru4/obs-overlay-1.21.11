package smoke;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
public final class MixinSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        try {
            String[] types={"net.minecraft.client.Minecraft","net.minecraft.client.renderer.GameRenderer",
                "net.minecraft.client.gui.Gui","net.minecraft.client.gui.Hud","net.minecraft.client.gui.render.GuiRenderer",
                "net.minecraft.client.renderer.state.gui.GuiRenderState","com.mojang.renderpearl.frontend.FrontendCommandEncoder",
                "net.minecraft.client.gui.screens.Screen","net.minecraft.client.gui.screens.options.VideoSettingsScreen"};
            for(String name:types) {
                var type=Class.forName(name,false,Thread.currentThread().getContextClassLoader());
                var methods=type.getDeclaredMethods();
                if(java.util.Arrays.stream(methods).noneMatch(m->m.getName().contains("obs")||m.getName().contains("scope")||m.getName().contains("simple")||m.getName().contains("Overlay")))
                    throw new AssertionError("Missing transformation: "+name);
                System.out.println("MIXIN_26_TRANSFORMED: "+name);
            }
            QueueOrderSmoke.run();
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("meteor-client")) {
                var widget=Class.forName("meteordevelopment.meteorclient.gui.WidgetScreen",false,Thread.currentThread().getContextClassLoader());
                if(java.util.Arrays.stream(widget.getDeclaredMethods()).noneMatch(m->m.getName().contains("obs$renderCustom"))) throw new AssertionError("Meteor WidgetScreen mixin missing");
                var renderer=Class.forName("meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer",false,Thread.currentThread().getContextClassLoader());
                if(!net.minecraft.client.gui.render.GuiRenderer.class.isAssignableFrom(renderer)) throw new AssertionError("Unexpected Meteor renderer");
                System.out.println("METEOR_26_CONTRACT_PASS: real 26.3-15 WidgetScreen hook and private GUI renderer; no in-world acceptance");
            }
            if(Boolean.getBoolean("obs.overlay.desktopSmoke")) WindowPresentationSmoke.run();
            if(Boolean.getBoolean("obs.overlay.glSmoke")) GlPresentationSmoke.run();
            if(Boolean.getBoolean("obs.overlay.vkSmoke")) VulkanReadbackSmoke.run();
            System.out.println("MIXIN_26_PASS: required targets transformed; no full game world or OBS acceptance tested");
            System.exit(0);
        } catch(Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
