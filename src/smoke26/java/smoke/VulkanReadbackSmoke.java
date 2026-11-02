package smoke;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Screenshot;
import org.joml.Vector4f;
import static org.lwjgl.sdl.SDLInit.*;

/** Actual RenderPearl Vulkan device and vanilla screenshot helper; no GUI/OBS acceptance claim. */
public final class VulkanReadbackSmoke {
    public static void run() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        NativeLibrariesBootstrap.loadLibraries();
        if(!SDL_Init(SDL_INIT_VIDEO)) throw new AssertionError("SDL initialization failed");
        var backend=new VulkanBackend();
        backend.loadLibrary();
        var device=backend.createDevice(new GpuDebugOptions(0,false,false,false));
        RenderSystem.initRenderThread();
        RenderSystem.initRenderer(device);
        var captured=new TextureTarget("capture",8,8,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
        var local=new TextureTarget("local",8,8,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
        var completed=new AtomicInteger();
        try {
            var encoder=device.createCommandEncoder();
            encoder.clearColorAndDepthTextures(captured.getColorTexture(),new Vector4f(1,0,0,1),captured.getDepthTexture(),0);
            local.copyColorFrom(captured);
            encoder.clearColorTexture(local.getColorTexture(),new Vector4f(0,1,0,1));
            Screenshot.takeScreenshot(captured,image-> {
                try(image) { if(image.getPixel(0,0)!=0xFFFF0000) throw new AssertionError("Captured Vulkan pixels changed"); completed.incrementAndGet(); }
            });
            Screenshot.takeScreenshot(local,image-> {
                try(image) { if(image.getPixel(7,7)!=0xFF00FF00) throw new AssertionError("Local Vulkan readback mismatch"); completed.incrementAndGet(); }
            });
            try(var fence=encoder.createFence()) {
                encoder.submit();
                if(!fence.awaitCompletion(5_000_000_000L)) throw new AssertionError("Vulkan readback timed out");
            }
            long until=System.nanoTime()+5_000_000_000L;
            // Vulkan dispatches readback callbacks through its frame-delayed destruction
            // queue. Advance submissions just as normal game frames do after a screenshot.
            while(completed.get()<2 && System.nanoTime()<until) { encoder.submit(); RenderSystem.executePendingTasks(); Thread.sleep(1); }
            if(completed.get()!=2) throw new AssertionError("Vulkan readback callbacks incomplete: "+completed.get());
            System.out.println("VULKAN_READBACK_PASS: actual "+device.getDeviceInfo().name()+", separate capture/local textures, vanilla async RGBA screenshot readback; no live OBS acceptance");
        } finally {
            captured.destroyBuffers(); local.destroyBuffers(); RenderSystem.shutdownRenderer(); backend.unloadLibrary(); SDL_Quit();
        }
    }
}
