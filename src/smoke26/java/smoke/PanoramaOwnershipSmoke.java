package smoke;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Reproduces the original exception using actual TextureManager and GPU textures. */
public final class PanoramaOwnershipSmoke {
    public static void run() {
        var id=Identifier.withDefaultNamespace("textures/gui/title/background/panorama");
        try(var textures=new TextureManager(null)) {
            var image=new NativeImage(1,1,false); image.setPixel(0,0,0xFF123456);
            var original=new DynamicTexture(()->"panorama ownership sentinel",image);
            textures.register(id,original);
            var view=original.getTextureView();
            try(var first=new GuiRenderer(new GuiRenderState(),null,List.of());
                var second=new GuiRenderer(new GuiRenderState(),null,List.of())) {
                if(textures.getTexture(id)!=original || original.getTextureView()!=view) throw new AssertionError("Constructing local renderers replaced the shared texture");
            }
            if(textures.getTexture(id)!=original || original.getTextureView()!=view) throw new AssertionError("Closing local renderers invalidated the shared texture");
            // Negative control: the removed alpha.1 call must reproduce the reported failure.
            try(var broken=new GuiRenderer(new GuiRenderState(),null,List.of())) {
                broken.registerPanoramaTextures(textures);
                try { textures.getTexture(id).getTextureView(); throw new AssertionError("Original crash path did not reproduce"); }
                catch(IllegalStateException expected) {
                    if(!expected.getMessage().contains("before something initializes it")) throw expected;
                }
            }
        }
        System.out.println("PANORAMA_OWNERSHIP_PASS: real GPU texture survives local renderer creation/close; removed registration reproduces alpha.1 uninitialized texture-view crash");
    }
}
