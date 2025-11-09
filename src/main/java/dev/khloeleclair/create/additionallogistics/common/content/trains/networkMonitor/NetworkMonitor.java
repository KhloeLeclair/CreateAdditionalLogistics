package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NetworkMonitor extends SingleBlockEntityEdgePoint {

    public static final EdgePointType<NetworkMonitor> NETWORK_MONITOR = EdgePointType.register(
            CreateAdditionalLogistics.asResource("network_monitor"),
            NetworkMonitor::new
    );

    public static void onTrainArrival(@Nullable TrackGraph graph, @Nullable UUID train, @Nullable UUID station) {
        if (graph == null || train == null || station == null)
            return;
        graph.getPoints(NETWORK_MONITOR).forEach(point -> point.onTrainArrival(train, station));
    }

    public static void onTrainDeparture(@Nullable TrackGraph graph, @Nullable UUID train) {
        if (graph == null || train == null)
            return;
        graph.getPoints(NETWORK_MONITOR).forEach(point -> point.onTrainDeparture(train));
    }


    public NetworkMonitor() {

    }

    @Nullable
    public NetworkMonitorBlockEntity getEntity() {
        var server = CreateAdditionalLogistics.getServer();
        if (server == null || blockEntityDimension == null)
            return null;

        var level = server.getLevel(blockEntityDimension);
        if (level == null || !level.isLoaded(blockEntityPos) || !(level.getBlockEntity(blockEntityPos) instanceof NetworkMonitorBlockEntity nmbe))
            return null;

        return nmbe;
    }

    public void onTrainArrival(UUID train, UUID station) {
        var nmbe = getEntity();
        if (nmbe != null)
            nmbe.onTrainArrival(train, station);
    }

    public void onTrainDeparture(UUID train) {
        var nmbe = getEntity();
        if (nmbe != null)
            nmbe.onTrainDeparture(train);
    }

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);
    }

}
