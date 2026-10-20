package me.zziger.obsoverlay.port;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.GpuSampler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Replay direct, visible screen draws only after closing the captured pass (GL forbids nested passes). */
public final class MirroredRenderPass implements RenderPass {
    private final RenderPass captured;
    private final Supplier<RenderPass> local;
    private final List<Consumer<RenderPass>> commands = new ArrayList<>();
    private boolean closed;
    public MirroredRenderPass(RenderPass captured, Supplier<RenderPass> local) {
        this.captured = captured;
        this.local = local;
    }
    private void submit(Consumer<RenderPass> command) {
        if (closed) throw new IllegalStateException("Closed mirrored screen pass");
        command.accept(captured);
        commands.add(command);
    }
    @Override public void pushDebugGroup(Supplier<String> name) { submit(pass -> pass.pushDebugGroup(name)); }
    @Override public void popDebugGroup() { submit(RenderPass::popDebugGroup); }
    @Override public void setPipeline(RenderPipeline pipeline) { submit(pass -> pass.setPipeline(pipeline)); }
    @Override public void bindTexture(String name, GpuTextureView texture, GpuSampler sampler) { submit(pass -> pass.bindTexture(name, texture, sampler)); }
    @Override public void setUniform(String name, GpuBuffer value) { submit(pass -> pass.setUniform(name, value)); }
    @Override public void setUniform(String name, GpuBufferSlice value) { submit(pass -> pass.setUniform(name, value)); }
    @Override public void enableScissor(int x, int y, int width, int height) { submit(pass -> pass.enableScissor(x, y, width, height)); }
    @Override public void disableScissor() { submit(RenderPass::disableScissor); }
    @Override public void setVertexBuffer(int slot, GpuBuffer buffer) { submit(pass -> pass.setVertexBuffer(slot, buffer)); }
    @Override public void setIndexBuffer(GpuBuffer buffer, VertexFormat.IndexType type) { submit(pass -> pass.setIndexBuffer(buffer, type)); }
    @Override public void drawIndexed(int baseVertex, int firstIndex, int indexCount, int instanceCount) { submit(pass -> pass.drawIndexed(baseVertex, firstIndex, indexCount, instanceCount)); }
    @Override public <T> void drawMultipleIndexed(Collection<RenderObject<T>> objects, GpuBuffer indices, VertexFormat.IndexType type, Collection<String> uniforms, T argument) {
        var savedObjects = List.copyOf(objects);
        var savedUniforms = List.copyOf(uniforms);
        submit(pass -> pass.drawMultipleIndexed(savedObjects, indices, type, savedUniforms, argument));
    }
    @Override public void draw(int firstVertex, int vertexCount) { submit(pass -> pass.draw(firstVertex, vertexCount)); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        try {
            captured.close();
            try (RenderPass pass = local.get()) {
                for (var command : commands) command.accept(pass);
            }
        } finally { commands.clear(); }
    }
}
