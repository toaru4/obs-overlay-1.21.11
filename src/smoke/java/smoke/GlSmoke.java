package smoke;

import me.zziger.obsoverlay.port.LocalCompositor;
import me.zziger.obsoverlay.port.NativeSwapHook;
import org.lwjgl.opengl.GL;
import java.nio.ByteBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** A hidden real Windows OpenGL window; no account or Minecraft assets are needed. */
public final class GlSmoke {
    public static void run() {
        if (!glfwInit()) throw new AssertionError("GLFW initialization failed");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(64, 64, "OBS Overlay automated test", 0, 0);
        if (window == 0) throw new AssertionError("Hidden OpenGL context unavailable");
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        int overlay = texture((byte)0, (byte)255, (byte)0, (byte)255);
        int captured = texture((byte)255, (byte)0, (byte)0, (byte)255);
        int capturedFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, capturedFbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, captured, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw new AssertionError("Test framebuffer incomplete");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClearColor(0, 0, 0, 1); glClear(GL_COLOR_BUFFER_BIT);
        glBindFramebuffer(GL_FRAMEBUFFER, capturedFbo);
        glViewport(3, 4, 19, 23);
        glEnable(GL_SCISSOR_TEST);
        glScissor(1, 2, 5, 6);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        LocalCompositor compositor = new LocalCompositor();
        int[] callbacks = {0};
        try {
            NativeSwapHook.install(() -> {
                compositor.draw(overlay, 64, 64);
                if (glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) != capturedFbo || glGetInteger(GL_READ_FRAMEBUFFER_BINDING) != capturedFbo) throw new AssertionError("Framebuffer state not restored");
                if (!glIsEnabled(GL_SCISSOR_TEST) || !glIsEnabled(GL_DEPTH_TEST) || glIsEnabled(GL_BLEND)) throw new AssertionError("GL capability state not restored");
                int[] viewport = new int[4]; glGetIntegerv(GL_VIEWPORT, viewport);
                if (!java.util.Arrays.equals(viewport, new int[]{3,4,19,23})) throw new AssertionError("Viewport not restored");
                expectPixel(255, 0, 0); // The captured main texture remains unchanged.
                glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
                glReadBuffer(GL_BACK);
                expectPixel(0, 255, 0); // The local window receives the overlay just before swap.
                glBindFramebuffer(GL_READ_FRAMEBUFFER, capturedFbo);
                callbacks[0]++;
            });
            glfwSwapBuffers(window);
            if (callbacks[0] != 1) throw new AssertionError("Native swap callback failed; count=" + callbacks[0]);
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("OpenGL error during native composition");
            System.out.println("GL_SMOKE_PASS: native swap hook, local pixels, clean captured texture, GL state restoration");
        } finally {
            NativeSwapHook.uninstall(); compositor.close();
            glDeleteFramebuffers(capturedFbo); glDeleteTextures(overlay); glDeleteTextures(captured);
        }
        try { TransparencyGlSmoke.run(); QueueSmoke.run(); }
        catch (Exception e) { throw new AssertionError("Queue routing smoke failed", e); }
        finally { glfwDestroyWindow(window); glfwTerminate(); }
    }
    private static int texture(byte r, byte g, byte b, byte a) {
        int id = glGenTextures(); glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer data = org.lwjgl.BufferUtils.createByteBuffer(4).put(new byte[]{r,g,b,a}); data.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        return id;
    }
    private static void expectPixel(int r, int g, int b) {
        ByteBuffer pixel = org.lwjgl.BufferUtils.createByteBuffer(4);
        glReadPixels(0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        if (Byte.toUnsignedInt(pixel.get(0)) != r || Byte.toUnsignedInt(pixel.get(1)) != g || Byte.toUnsignedInt(pixel.get(2)) != b) throw new AssertionError("Unexpected framebuffer pixel: " + Byte.toUnsignedInt(pixel.get(0)) + "," + Byte.toUnsignedInt(pixel.get(1)) + "," + Byte.toUnsignedInt(pixel.get(2)));
    }
}
