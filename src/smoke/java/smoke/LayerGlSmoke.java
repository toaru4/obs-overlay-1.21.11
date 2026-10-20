package smoke;

import com.mojang.blaze3d.systems.RenderPass;
import me.zziger.obsoverlay.port.LocalCompositor;
import me.zziger.obsoverlay.port.MirroredRenderPass;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL33C.*;

/** Opaque inventory, half-transparent menu and empty pixels above a local-only HUD. */
public final class LayerGlSmoke {
    public static void run() {
        try (LocalCompositor compositor = new LocalCompositor()) {
            check(compositor, 64, 64);
            check(compositor, 32, 16);
        }
        System.out.println("LAYER_GL_PASS: HUD retained below opaque/translucent screens; capture clean; direct-pass replay ordered; no double blend; resized target");
    }
    private static void check(LocalCompositor compositor, int width, int height) {
        int world = texture(width, height, 0), hud = texture(width, height, 1);
        int screen = texture(width, height, 2), local = texture(width, height, 0);
        int read = glGenFramebuffers();
        try {
            glDisable(GL_SCISSOR_TEST); glDisable(GL_FRAMEBUFFER_SRGB);
            compositor.drawToTexture(world, local, width, height, true);
            compositor.drawToTexture(hud, local, width, height, false);
            List<String> calls = new ArrayList<>();
            boolean[] open = {true};
            RenderPass captured = pass("captured", world, screen, width, height, compositor, calls, open);
            RenderPass mirrored = new MirroredRenderPass(captured, () -> {
                if (open[0]) throw new AssertionError("Nested GL render passes");
                calls.add("local-open");
                return pass("local", local, screen, width, height, compositor, calls, new boolean[]{true});
            });
            mirrored.enableScissor(0, 0, width, height);
            mirrored.draw(0, 3);
            pixel(read, local, width / 8, 0, 255, 0); // No local replay until the captured pass closes.
            mirrored.close(); mirrored.close();
            if (!calls.equals(List.of("captured-scissor", "captured-draw", "captured-close", "local-open", "local-scissor", "local-draw", "local-close"))) throw new AssertionError("Screen replay order: " + calls);
            pixel(read, world, width / 8, 0, 0, 255);
            pixel(read, world, width / 2, 127, 0, 128);
            pixel(read, world, width * 7 / 8, 255, 0, 0);
            pixel(read, local, width / 8, 0, 0, 255); // Opaque screen occludes HUD.
            pixel(read, local, width / 2, 0, 127, 128); // Translucent screen blends with HUD exactly once.
            pixel(read, local, width * 7 / 8, 0, 255, 0); // HUD still present outside screen.
            compositor.draw(world, width, height, true);
            compositor.draw(local, width, height, true);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0); glReadBuffer(GL_BACK);
            readPixel(width / 2, 0, 127, 128); // Replace complete local image, no repeated menu opacity.
            pixel(read, world, width / 2, 127, 0, 128); // Window composition never modifies capture.
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("Screen layering GL error");
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(read);
            glDeleteTextures(world); glDeleteTextures(hud); glDeleteTextures(screen); glDeleteTextures(local);
        }
    }
    private static RenderPass pass(String name, int target, int screen, int width, int height,
            LocalCompositor compositor, List<String> calls, boolean[] open) {
        return (RenderPass)Proxy.newProxyInstance(RenderPass.class.getClassLoader(), new Class<?>[]{RenderPass.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "enableScissor" -> calls.add(name + "-scissor");
                case "draw" -> { calls.add(name + "-draw"); compositor.drawToTexture(screen, target, width, height, false); }
                case "close" -> { calls.add(name + "-close"); open[0] = false; }
                default -> throw new AssertionError("Unexpected command " + method);
            }
            return null;
        });
    }
    private static int texture(int width, int height, int kind) {
        int id = glGenTextures(); glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer bytes = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int alpha = x < width / 3 ? 255 : x < width * 2 / 3 ? 128 : 0;
            bytes.put((byte)(kind == 0 ? 255 : 0)).put((byte)(kind == 1 ? 255 : 0))
                    .put((byte)(kind == 2 ? alpha : 0)).put((byte)(kind == 2 ? alpha : 255));
        }
        bytes.flip(); glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, bytes);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return id;
    }
    private static void pixel(int read, int texture, int x, int r, int g, int b) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
        glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        glReadBuffer(GL_COLOR_ATTACHMENT0); readPixel(x, r, g, b);
    }
    private static void readPixel(int x, int r, int g, int b) {
        ByteBuffer bytes = org.lwjgl.BufferUtils.createByteBuffer(4);
        glReadPixels(x, 1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, bytes);
        int[] expected = {r, g, b};
        for (int i = 0; i < 3; i++) if (Math.abs(Byte.toUnsignedInt(bytes.get(i)) - expected[i]) > 1)
            throw new AssertionError("Screen layering pixel " + x + " channel " + i + ": " + Byte.toUnsignedInt(bytes.get(i)) + " expected " + expected[i]);
    }
}
