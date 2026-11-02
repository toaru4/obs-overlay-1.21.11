package me.zziger.obsoverlay.port;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendFactor;

import java.util.Optional;

/** Xaero's destination-alpha-preserving map blit assumes an opaque destination. */
public final class OverlayBlendPolicy {
    public static Optional<BlendFunction> adjust(String pipeline, Optional<BlendFunction> blend, boolean overlayTarget) {
        if (!overlayTarget || !pipeline.equals("xaerolib:pipeline/map_frame") || blend.isEmpty()) return blend;
        BlendFunction value = blend.get();
        if (value.color().sourceFactor() != BlendFactor.ONE || value.color().destFactor() != BlendFactor.ZERO
                || value.alpha().sourceFactor() != BlendFactor.ZERO || value.alpha().destFactor() != BlendFactor.ONE) return blend;
        // Its source texture has premultiplied edges. Preserve both coverage and color
        // when blitting into our initially transparent target instead of keeping alpha=0.
        return Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA));
    }
}
