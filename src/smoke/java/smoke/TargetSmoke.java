package smoke;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import me.zziger.obsoverlay.port.RenderRouter;

/** Models Xaero's temporary main-framebuffer replacement during scaling and rotation. */
public final class TargetSmoke {
    public static void run() {
        GpuTexture oldScreen = RenderRouter.mainColorTexture;
        GpuTexture screen = texture("screen");
        GpuTexture scaling = texture("Xaero scaling");
        GpuTexture rotation = texture("Xaero rotation");
        GpuTexture pip = texture("Xaero PiP");
        try {
            RenderRouter.mainColorTexture = screen;
            for (GpuTexture temporaryMain : new GpuTexture[]{scaling, rotation, pip, screen}) {
                boolean redirected = RenderRouter.isScreenTarget(temporaryMain);
                if (redirected != (temporaryMain == screen)) throw new AssertionError("Temporary Xaero target was redirected: " + temporaryMain.getLabel());
                if (!RenderRouter.isScreenTarget(screen)) throw new AssertionError("Real screen was lost during temporary target replacement");
            }
            if (RenderRouter.isScreenTarget(null)) throw new AssertionError("Null target recognized as screen");
        } finally { RenderRouter.mainColorTexture = oldScreen; }
        System.out.println("TARGET_SMOKE_PASS: stable screen identity; Xaero scaling, rotation and PiP targets preserved");
    }
    private static GpuTexture texture(String name) {
        return new GpuTexture(12, name, TextureFormat.RGBA8, 64, 64, 1, 1) {
            @Override public void close() {}
            @Override public boolean isClosed() { return false; }
        };
    }
}
