package me.zziger.obsoverlay.port;

import com.mojang.renderpearl.api.commands.*;
import com.mojang.renderpearl.api.buffers.*;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import java.nio.*;
import java.util.*;
import java.util.function.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.BufferUtils;

/** Backend-neutral replay: close the captured pass before opening the local pass. */
public final class MirroredRenderPass implements RenderPass {
    private final RenderPass captured;
    private final Supplier<RenderPass> local;
    private final List<Consumer<RenderPass>> commands = new ArrayList<>();
    private boolean closed;
    public MirroredRenderPass(RenderPass captured, Supplier<RenderPass> local) {
        this.captured = captured; this.local = local;
    }
    private void submit(Consumer<RenderPass> command) {
        if (closed) throw new IllegalStateException("Closed mirrored pass");
        command.accept(captured); commands.add(command);
    }
    private static ByteBuffer snapshot(ByteBuffer value) { var copy=BufferUtils.createByteBuffer(value.remaining()); copy.put(value.duplicate()); return copy.flip(); }
    private static IntBuffer snapshot(IntBuffer value) { var copy=BufferUtils.createIntBuffer(value.remaining()); copy.put(value.duplicate()); return copy.flip(); }
    private static PointerBuffer snapshot(PointerBuffer value) { var copy=BufferUtils.createPointerBuffer(value.remaining()); copy.put(value.duplicate()); return copy.flip(); }
    @Override public void pushDebugGroup(Supplier<String> name) { submit(pass -> pass.pushDebugGroup(name)); }
    @Override public void popDebugGroup() { submit(pass -> pass.popDebugGroup()); }
    @Override public void writeTimestamp(GpuQueryPool pool, int index) { submit(pass -> pass.writeTimestamp(pool, index)); }
    @Override public void setPipeline(CompiledRenderPipeline pipeline) { submit(pass -> pass.setPipeline(pipeline)); }
    @Override public void setUniform(String name, GpuTextureView texture, GpuSampler sampler) { submit(pass -> pass.setUniform(name, texture, sampler)); }
    @Override public void setUniform(String name, GpuBuffer buffer) { submit(pass -> pass.setUniform(name, buffer)); }
    @Override public void setUniform(String name, GpuBufferSlice buffer) { submit(pass -> pass.setUniform(name, buffer)); }
    @Override public void pushConstants(ByteBuffer data) { var saveddata=snapshot(data); submit(pass -> pass.pushConstants(saveddata.duplicate())); }
    @Override public void enableScissor(int x, int y, int width, int height) { submit(pass -> pass.enableScissor(x, y, width, height)); }
    @Override public void disableScissor() { submit(pass -> pass.disableScissor()); }
    @Override public void setVertexBuffer(int slot, GpuBufferSlice buffer) { submit(pass -> pass.setVertexBuffer(slot, buffer)); }
    @Override public void setIndexBuffer(GpuBuffer buffer, IndexType type) { submit(pass -> pass.setIndexBuffer(buffer, type)); }
    @Override public void drawIndexed(int baseVertex, int firstIndex, int count, int instances, int firstInstance) { submit(pass -> pass.drawIndexed(baseVertex, firstIndex, count, instances, firstInstance)); }
    @Override public void draw(int firstVertex, int count, int instances, int firstInstance) { submit(pass -> pass.draw(firstVertex, count, instances, firstInstance)); }
    @Override public void multiDrawIndexed(IntBuffer data, int offset, int count, int stride) { var saveddata=snapshot(data); submit(pass -> pass.multiDrawIndexed(saveddata.duplicate(), offset, count, stride)); }
    @Override public void multiDrawIndexed(PointerBuffer offsets, IntBuffer counts, IntBuffer bases, int instances) { var savedoffsets=snapshot(offsets); var savedcounts=snapshot(counts); var savedbases=snapshot(bases); submit(pass -> pass.multiDrawIndexed(savedoffsets.duplicate(), savedcounts.duplicate(), savedbases.duplicate(), instances)); }
    @Override public void drawIndexedIndirect(GpuBufferSlice commands, int count) { submit(pass -> pass.drawIndexedIndirect(commands, count)); }
    @Override public void multiDraw(IntBuffer data, int offset, int count, int stride) { var saveddata=snapshot(data); submit(pass -> pass.multiDraw(saveddata.duplicate(), offset, count, stride)); }
    @Override public void multiDraw(IntBuffer starts, IntBuffer counts, int instances) { var savedstarts=snapshot(starts); var savedcounts=snapshot(counts); submit(pass -> pass.multiDraw(savedstarts.duplicate(), savedcounts.duplicate(), instances)); }
    @Override public void drawIndirect(GpuBufferSlice commands, int count) { submit(pass -> pass.drawIndirect(commands, count)); }
    @Override public <T> void drawMultipleIndexed(Collection<Draw<T>> draws, GpuBuffer indices, IndexType type, Collection<String> uniforms, T argument) {
        var savedDraws=List.copyOf(draws); var savedUniforms=List.copyOf(uniforms);
        submit(pass -> pass.drawMultipleIndexed(savedDraws, indices, type, savedUniforms, argument));
    }
    @Override public void close() {
        if (closed) return;
        closed=true;
        try {
            captured.close();
            try (RenderPass pass=local.get()) { for (var command:commands) command.accept(pass); }
        } finally { commands.clear(); }
    }
}
