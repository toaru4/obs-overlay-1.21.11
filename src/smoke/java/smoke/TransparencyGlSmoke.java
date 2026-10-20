package smoke;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.DestFactor;
import me.zziger.obsoverlay.port.LocalCompositor;
import me.zziger.obsoverlay.port.OverlayBlendPolicy;
import java.nio.ByteBuffer;
import java.util.Optional;
import static org.lwjgl.opengl.GL33C.*;

/** Reproduce destination-alpha=0 brightening, then verify opaque, half-alpha and empty map pixels. */
public final class TransparencyGlSmoke {
    public static void run() {
        int texture = glGenTextures(); glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 64, 64, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        int fbo = glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        int vertex = shader(GL_VERTEX_SHADER, "#version 330 core\nvoid main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2.-1.,0.,1.);}");
        int fragment = shader(GL_FRAGMENT_SHADER, "#version 330 core\nuniform vec4 testColor;out vec4 color;void main(){color=testColor;}");
        int program = glCreateProgram(); glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new AssertionError(glGetProgramInfoLog(program));
        glDeleteShader(vertex); glDeleteShader(fragment);
        int vao = glGenVertexArrays();
        BlendFunction legacy = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE);
        BlendFunction fixed = OverlayBlendPolicy.adjust("xaerolib:pipeline/map_frame", Optional.of(legacy), true).orElseThrow();
        LocalCompositor compositor = new LocalCompositor();
        try {
            paint(fbo, program, vao, legacy, 1);
            compose(compositor, texture);
            pixel(8, 96, 160, 224); // The previous implementation adds map RGB to the world.
            paint(fbo, program, vao, fixed, 1);
            compose(compositor, texture);
            pixel(8, 64, 96, 128); // Opaque map is no longer added to world RGB.
            pixel(48, 32, 64, 96); // An empty background leaves the world unchanged.
            paint(fbo, program, vao, fixed, .5f);
            compose(compositor, texture);
            pixel(8, 48, 80, 112); // Premultiplied semi-transparent edges blend once.
            pixel(48, 32, 64, 96);
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("Transparent map GL error");
            System.out.println("TRANSPARENCY_GL_PASS: old brightening reproduced; opaque map, half-alpha edges and transparent world background corrected");
        } finally {
            compositor.close(); glUseProgram(0); glBindVertexArray(0); glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteVertexArrays(vao); glDeleteProgram(program); glDeleteFramebuffers(fbo); glDeleteTextures(texture);
        }
    }
    private static void paint(int fbo, int program, int vao, BlendFunction blend, float alpha) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo); glViewport(0, 0, 64, 64);
        glDisable(GL_SCISSOR_TEST); glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE); glDisable(GL_FRAMEBUFFER_SRGB);
        glClearColor(0, 0, 0, 0); glClear(GL_COLOR_BUFFER_BIT);
        glEnable(GL_BLEND); glBlendEquation(GL_FUNC_ADD);
        glBlendFuncSeparate(source(blend.sourceColor()), dest(blend.destColor()), source(blend.sourceAlpha()), dest(blend.destAlpha()));
        glEnable(GL_SCISSOR_TEST); glScissor(0, 0, 32, 64);
        glUseProgram(program); glBindVertexArray(vao);
        glUniform4f(glGetUniformLocation(program, "testColor"), 64f/255*alpha, 96f/255*alpha, 128f/255*alpha, alpha);
        glDrawArrays(GL_TRIANGLES, 0, 3); glDisable(GL_SCISSOR_TEST);
    }
    private static void compose(LocalCompositor compositor, int texture) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0); glClearColor(32f/255, 64f/255, 96f/255, 1); glClear(GL_COLOR_BUFFER_BIT);
        compositor.draw(texture, 64, 64); glBindFramebuffer(GL_READ_FRAMEBUFFER, 0); glReadBuffer(GL_BACK);
    }
    private static void pixel(int x, int r, int g, int b) {
        ByteBuffer bytes = org.lwjgl.BufferUtils.createByteBuffer(4); glReadPixels(x, 8, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, bytes);
        int[] expected = {r,g,b};
        for (int i=0;i<3;i++) if (Math.abs(Byte.toUnsignedInt(bytes.get(i))-expected[i])>1) throw new AssertionError("Transparent-map pixel mismatch at " + x + " channel " + i + ": " + Byte.toUnsignedInt(bytes.get(i)) + " expected " + expected[i]);
    }
    private static int source(SourceFactor factor) { return factor == SourceFactor.ONE ? GL_ONE : GL_ZERO; }
    private static int dest(DestFactor factor) { return factor == DestFactor.ONE ? GL_ONE : factor == DestFactor.ONE_MINUS_SRC_ALPHA ? GL_ONE_MINUS_SRC_ALPHA : GL_ZERO; }
    private static int shader(int type, String source) {
        int shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new AssertionError(glGetShaderInfoLog(shader));
        return shader;
    }
}
