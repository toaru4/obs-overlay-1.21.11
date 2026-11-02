import com.mojang.renderpearl.api.commands.*;
import com.mojang.renderpearl.api.textures.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import me.zziger.obsoverlay.port.RenderTargetRouter;

public class RenderTargetRouterTest {
    static GpuTexture texture() {
        return (GpuTexture)Proxy.newProxyInstance(GpuTexture.class.getClassLoader(),new Class<?>[]{GpuTexture.class},(p,m,a)-> {
            if(m.getName().equals("isClosed")) return false;
            throw new UnsupportedOperationException(m.getName());
        });
    }
    static GpuTextureView view(GpuTexture texture) {
        return (GpuTextureView)Proxy.newProxyInstance(GpuTextureView.class.getClassLoader(),new Class<?>[]{GpuTextureView.class},(p,m,a)-> {
            if(m.getName().equals("texture")) return texture;
            throw new UnsupportedOperationException(m.getName());
        });
    }
    public static void main(String[] args) {
        var screen=texture(); var depth=texture(); var privateMap=texture();
        var local=view(texture()); var localDepth=view(texture());
        var screenView=view(screen); var mapView=view(privateMap); var depthView=view(depth);
        var clear=Optional.<Vector4fc>of(new Vector4f(0,0,0,0));
        var screenAttachment=new RenderPassDescriptor.Attachment<>(screenView,clear);
        var mapAttachment=new RenderPassDescriptor.Attachment<>(mapView,Optional.<Vector4fc>empty());
        var depthAttachment=new RenderPassDescriptor.Attachment<>(depthView,OptionalDouble.of(1));
        var area=new RenderPass.RenderArea(3,5,64,48);
        var descriptor=new RenderPassDescriptor(()->"screen",List.of(screenAttachment,mapAttachment),depthAttachment,area);
        var redirected=RenderTargetRouter.redirect(descriptor,screen,depth,local,localDepth);
        if(redirected.colorAttachments().get(0).textureView()!=local || redirected.depthAttachment().textureView()!=localDepth) throw new AssertionError("Screen not redirected");
        if(redirected.colorAttachments().get(1)!=mapAttachment) throw new AssertionError("Private map target changed");
        if(redirected.label()!=descriptor.label() || redirected.renderArea()!=area || redirected.colorAttachments().get(0).clearValue()!=clear) throw new AssertionError("Pass settings changed");
        if(descriptor.colorAttachments().get(0).textureView()!=screenView) throw new AssertionError("Captured descriptor mutated");
        var offscreen=new RenderPassDescriptor(()->"map",List.of(mapAttachment),depthAttachment,area);
        if(RenderTargetRouter.redirect(offscreen,screen,depth,local,localDepth)!=offscreen) throw new AssertionError("Offscreen pass rewritten");
        if(RenderTargetRouter.targets(descriptor,null)) throw new AssertionError("Missing screen matched");
        System.out.println("RENDER_TARGET_ROUTER_PASS: screen-only routing, private/MRT attachment isolation, depth, clear values, render area, immutable captured descriptor");
    }
}
