package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Train.class)
public class MixinTrain {

    @Shadow(remap = false)
    public UUID id;

    @Shadow(remap = false)
    public TrackGraph graph;

    @Inject(
        method = "leaveStation",
        at = @At("RETURN"),
        remap = false
    )
    private void CAL$onLeaveStation(CallbackInfo ci) {
        NetworkMonitor.onTrainDeparture(graph, id);
    }

    @Inject(
            method = "arriveAt",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onArriveAt(GlobalStation station, CallbackInfo ci) {
        NetworkMonitor.onTrainArrival(graph, id, station.id);
    }

}
