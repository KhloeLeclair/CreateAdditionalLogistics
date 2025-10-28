package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TrackTargetingClient.class)
public class MixinTrackTargetingClient {

    @Shadow
    static EdgePointType<?> lastType;

    @ModifyVariable(
            method = "render",
            name = "type",
            at = @At("STORE")
    )
    private static TrackTargetingBehaviour.RenderedTrackOverlayType CAL$onGetType(TrackTargetingBehaviour.RenderedTrackOverlayType previous) {
        if (lastType == NetworkMonitor.NETWORK_MONITOR)
            return TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER;
        return previous;
    }

}
