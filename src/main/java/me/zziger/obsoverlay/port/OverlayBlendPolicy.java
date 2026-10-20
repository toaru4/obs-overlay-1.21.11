package me.zziger.obsoverlay.port;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.DestFactor;
import java.util.Optional;

/** Xaero's destination-alpha-preserving map blit assumes an opaque destination. */
public final class OverlayBlendPolicy {
    public static Optional<BlendFunction> adjust(String pipeline, Optional<BlendFunction> blend, boolean overlayTarget) {
        if (!overlayTarget || !pipeline.equals("xaerolib:pipeline/map_frame") || blend.isEmpty()) return blend;
        BlendFunction value = blend.get();
        if (value.sourceColor() != SourceFactor.ONE || value.destColor() != DestFactor.ZERO
                || value.sourceAlpha() != SourceFactor.ZERO || value.destAlpha() != DestFactor.ONE) return blend;
        // Its source texture has premultiplied edges. Preserve both coverage and color
        // when blitting into our initially transparent target instead of keeping alpha=0.
        return Optional.of(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA));
    }
}
