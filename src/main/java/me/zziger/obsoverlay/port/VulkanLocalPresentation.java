package me.zziger.obsoverlay.port;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;

/** Experimental readback presentation, capped at 30 FPS and one GPU readback in flight. */
public final class VulkanLocalPresentation implements AutoCloseable {
    private final CaptureExcludedWindow window;
    private final AtomicBoolean pending=new AtomicBoolean();
    private volatile boolean closed;
    private long nextCapture;
    public VulkanLocalPresentation() {
        var gameWindow=Minecraft.getInstance().getWindow();
        long owner=SDLProperties.SDL_GetPointerProperty(SDLVideo.SDL_GetWindowProperties(gameWindow.handle()),SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER,0);
        window=new CaptureExcludedWindow(owner);
    }
    public boolean ready() { return !closed && window.isAvailable() && !Minecraft.getInstance().getWindow().isExclusiveFullscreen(); }
    public void capture(RenderTarget scene) {
        if(closed) return;
        if(Minecraft.getInstance().getWindow().isExclusiveFullscreen()) { window.hide(); return; }
        long now=System.nanoTime();
        long epoch=window.requestFrame();
        if(now<nextCapture || !pending.compareAndSet(false,true)) return;
        nextCapture=now+33_333_333L;
        try {
            // Vanilla's helper handles backend image layout, format conversion and row orientation.
            Screenshot.takeScreenshot(scene,image-> {
                try(image) { if(!closed) window.submit(epoch,image.getWidth(),image.getHeight(),image.getPixels()); }
                catch(RuntimeException | LinkageError e) { close(); OBSOverlay.LOGGER.error("Vulkan local readback failed; filtering disabled",e); }
                finally { pending.set(false); }
            });
        } catch(RuntimeException | LinkageError e) { pending.set(false); close(); throw e; }
    }
    public void hide() { window.hide(); }
    @Override public void close() { closed=true; window.close(); }
}
