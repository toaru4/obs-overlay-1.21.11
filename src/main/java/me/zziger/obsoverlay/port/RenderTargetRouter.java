package me.zziger.obsoverlay.port;

import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

/** Rewrite only stable screen attachments; backend-specific and private map targets stay untouched. */
public final class RenderTargetRouter {
    private RenderTargetRouter() {}
    public static boolean targets(RenderPassDescriptor descriptor, GpuTexture screen) {
        return screen != null && descriptor.colorAttachments().stream()
                .anyMatch(value -> value != null && value.textureView().texture() == screen);
    }
    public static RenderPassDescriptor redirect(RenderPassDescriptor descriptor,
            GpuTexture screenColor, GpuTexture screenDepth, GpuTextureView localColor, GpuTextureView localDepth) {
        if (!targets(descriptor, screenColor)) return descriptor;
        var colors = descriptor.colorAttachments().stream().map(value ->
                value != null && value.textureView().texture() == screenColor
                    ? new RenderPassDescriptor.Attachment<>(localColor, value.clearValue()) : value).toList();
        var depth = descriptor.depthAttachment();
        if (depth != null && screenDepth != null && depth.textureView().texture() == screenDepth)
            depth = new RenderPassDescriptor.Attachment<>(localDepth, depth.clearValue());
        return new RenderPassDescriptor(descriptor.label(), colors, depth, descriptor.renderArea());
    }
}
