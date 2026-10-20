package smoke;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.DestFactor;
import me.zziger.obsoverlay.port.OverlayBlendPolicy;
import java.util.Optional;
public final class BlendSmoke {
    public static void run() {
        Optional<BlendFunction> original = Optional.of(new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE));
        if (OverlayBlendPolicy.adjust("xaerolib:pipeline/map_frame", original, false) != original) throw new AssertionError("Captured/offscreen map blend changed");
        if (OverlayBlendPolicy.adjust("xaerolib:pipeline/map", original, true) != original) throw new AssertionError("Internal map generation blend changed");
        if (OverlayBlendPolicy.adjust("minecraft:gui", original, true) != original) throw new AssertionError("Unrelated GUI blend changed");
        var corrected = OverlayBlendPolicy.adjust("xaerolib:pipeline/map_frame", original, true).orElseThrow();
        if (corrected.sourceColor() != SourceFactor.ONE || corrected.destColor() != DestFactor.ONE_MINUS_SRC_ALPHA
                || corrected.sourceAlpha() != SourceFactor.ONE || corrected.destAlpha() != DestFactor.ONE_MINUS_SRC_ALPHA) throw new AssertionError("Wrong premultiplied map blend");
        Optional<BlendFunction> additive = Optional.of(new BlendFunction(SourceFactor.ONE, DestFactor.ONE));
        if (OverlayBlendPolicy.adjust("xaerolib:pipeline/map_frame", additive, true) != additive) throw new AssertionError("Other map blend was changed");
        System.out.println("BLEND_SMOKE_PASS: map frame coverage fixed only on local overlay; other blends unchanged");
    }
}
