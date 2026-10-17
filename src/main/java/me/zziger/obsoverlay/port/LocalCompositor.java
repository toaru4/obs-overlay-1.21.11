package me.zziger.obsoverlay.port;

import static org.lwjgl.opengl.GL33C.*;

/** Draw after OBS's game capture hook; do not modify Minecraft's captured main texture. */
public final class LocalCompositor implements AutoCloseable {
    private final int program;
    private final int vao;
    public LocalCompositor() {
        int vertex = compile(GL_VERTEX_SHADER, """
                #version 330 core
                out vec2 uv;
                void main() {
                    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                    uv = p;
                    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
                }
                """);
        int fragment = compile(GL_FRAGMENT_SHADER, """
                #version 330 core
                uniform sampler2D overlay;
                in vec2 uv;
                out vec4 color;
                void main() { color = texture(overlay, uv); }
                """);
        program = glCreateProgram(); glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program);
        glDeleteShader(vertex); glDeleteShader(fragment);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) throw new IllegalStateException(glGetProgramInfoLog(program));
        vao = glGenVertexArrays();
    }
    private static int compile(int type, String source) {
        int shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new IllegalStateException(glGetShaderInfoLog(shader));
        return shader;
    }
    public void draw(int texture, int width, int height) {
        int oldProgram = glGetInteger(GL_CURRENT_PROGRAM), oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int drawFb = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING), readFb = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        int[] viewport = new int[4]; glGetIntegerv(GL_VIEWPORT, viewport);
        boolean blend = glIsEnabled(GL_BLEND), depth = glIsEnabled(GL_DEPTH_TEST), cull = glIsEnabled(GL_CULL_FACE), scissor = glIsEnabled(GL_SCISSOR_TEST), srgb = glIsEnabled(GL_FRAMEBUFFER_SRGB);
        int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB), srcA = glGetInteger(GL_BLEND_SRC_ALPHA), dstA = glGetInteger(GL_BLEND_DST_ALPHA);
        int eqRgb = glGetInteger(GL_BLEND_EQUATION_RGB), eqA = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        glActiveTexture(GL_TEXTURE0);
        int oldTexture = glGetInteger(GL_TEXTURE_BINDING_2D), oldSampler = glGetInteger(GL_SAMPLER_BINDING);
        try {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0); glViewport(0, 0, width, height);
            glEnable(GL_BLEND); glDisable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE); glDisable(GL_SCISSOR_TEST); glDisable(GL_FRAMEBUFFER_SRGB);
            // Offscreen GUI color is already premultiplied by its translucent pipelines.
            glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA); glBlendEquation(GL_FUNC_ADD);
            glUseProgram(program); glBindVertexArray(vao); glBindSampler(0, 0); glBindTexture(GL_TEXTURE_2D, texture);
            glUniform1i(glGetUniformLocation(program, "overlay"), 0);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        } finally {
            glBindTexture(GL_TEXTURE_2D, oldTexture); glBindSampler(0, oldSampler); glActiveTexture(activeTexture);
            glUseProgram(oldProgram); glBindVertexArray(oldVao);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFb); glBindFramebuffer(GL_READ_FRAMEBUFFER, readFb);
            glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            glBlendFuncSeparate(srcRgb, dstRgb, srcA, dstA); glBlendEquationSeparate(eqRgb, eqA);
            enabled(GL_BLEND, blend); enabled(GL_DEPTH_TEST, depth); enabled(GL_CULL_FACE, cull); enabled(GL_SCISSOR_TEST, scissor); enabled(GL_FRAMEBUFFER_SRGB, srgb);
        }
    }
    private static void enabled(int capability, boolean enabled) { if (enabled) glEnable(capability); else glDisable(capability); }
    @Override public void close() { glDeleteProgram(program); glDeleteVertexArrays(vao); }
}
