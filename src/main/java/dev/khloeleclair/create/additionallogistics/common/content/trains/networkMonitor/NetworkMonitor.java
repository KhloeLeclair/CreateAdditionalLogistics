package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;

public class NetworkMonitor extends SingleBlockEntityEdgePoint {

    public static final EdgePointType<NetworkMonitor> NETWORK_MONITOR = EdgePointType.register(
            CreateAdditionalLogistics.asResource("network_monitor"),
            NetworkMonitor::new
    );

    public NetworkMonitor() {

    }

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);
    }

}
