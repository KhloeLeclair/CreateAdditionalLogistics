package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.station.GlobalStation;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NetworkMonitorPeripheral extends SyncedPeripheral<NetworkMonitorBlockEntity> {

    public NetworkMonitorPeripheral(NetworkMonitorBlockEntity blockEntity) {
        super(blockEntity);
    }

    private Map<String, Object> serializeStation(GlobalStation station, boolean include_trains) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", station.id.toString());
        result.put("name", station.name);
        //result.put("position", ComputerUtil.getPosition(station.blockEntityPos, station.blockEntityDimension));

        if (include_trains)
            result.put("train", serializeTrain(station.getPresentTrain()));

        Map<Integer, Map<String, ?>> ports = new HashMap<>();
        int j = 0;

        var server = CreateAdditionalLogistics.getServer();
        var level = server == null ? null : server.getLevel(station.blockEntityDimension);

        for(var portEntry : station.connectedPorts.entrySet()) {
            j++;
            var port = portEntry.getValue();
            Map<String, Object> portData = new HashMap<>();
            portData.put("address", port.address);

            if (level != null && level.isLoaded(portEntry.getKey()) && level.getBlockEntity(portEntry.getKey()) instanceof PostboxBlockEntity pbe)
                portData.put("send_only", !pbe.acceptsPackages);

            ports.put(j, portData);
        }

        result.put("postboxes", ports);

        return result;
    }

    @Nullable
    private Map<String, Object> serializeTrain(@Nullable Train train) {
        if (train == null || train.invalid)
            return null;

        Map<String, Object> result = new HashMap<>();

        result.put("id", train.id.toString());
        result.put("name", train.name.getString());

        return result;
    }



    @Nullable
    @LuaFunction(mainThread = true)
    public Map<String, ?> getStation(String key, Optional<Boolean> trains) {
        var pos = blockEntity.edgePoint.determineGraphLocation();
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch(IllegalArgumentException ex) {
            id = null;
        }

        GlobalStation station;
        if (id != null)
            station = pos.graph.getPoint(EdgePointType.STATION, id);
        else
            station = pos.graph.getPoints(EdgePointType.STATION).stream().filter(x -> Objects.equals(x.name, key)).findFirst().orElse(null);

        if (station == null)
            return null;

        return serializeStation(station, trains.orElse(false));
    }

    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> listStations() {
        var pos = blockEntity.edgePoint.determineGraphLocation();
        var stations = pos.graph.getPoints(EdgePointType.STATION);

        Map<Integer, Map<String, ?>> result = new HashMap<>();

        int i = 0;
        for(var entry : stations) {
            i++;
            result.put(i, serializeStation(entry, false));
        }

        return result;
    }

    @Override
    public String getType() {
        return "CreateAdditionalLogistics_NetworkMonitor";
    }
}
