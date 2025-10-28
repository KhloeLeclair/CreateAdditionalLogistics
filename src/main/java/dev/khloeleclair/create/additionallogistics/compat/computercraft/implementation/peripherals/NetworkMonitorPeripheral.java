package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaStationObject;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaTrainObject;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NetworkMonitorPeripheral extends SyncedPeripheral<NetworkMonitorBlockEntity> {

    public NetworkMonitorPeripheral(NetworkMonitorBlockEntity blockEntity) {
        super(blockEntity);
    }

    private Map<String, Object> serializeStation(GlobalStation station) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", station.id.toString());
        result.put("name", station.name);

        var server = CreateAdditionalLogistics.getServer();
        var level = server == null ? null : server.getLevel(station.blockEntityDimension);

        List<String> addresses = new ArrayList<>();
        int incoming = 0;
        int outgoing = 0;

        for(var portEntry : station.connectedPorts.entrySet()) {
            var port = portEntry.getValue();
            var pos = portEntry.getKey();

            if (port.address != null && !port.address.isBlank())
                addresses.add(port.address);

            IItemHandlerModifiable inventory = port.offlineBuffer;

            if (!port.primed && level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PostboxBlockEntity pbe)
                inventory = pbe.inventory;

            for(int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!PackageItem.isPackage(stack))
                    continue;

                boolean is_incoming = PackageItem.matchAddress(stack, port.address);
                if (is_incoming)
                    incoming++;
                else
                    outgoing++;
            }
        }

        result.put("addresses", addresses);
        result.put("packages_incoming", incoming);
        result.put("packages_outgoing", outgoing);

        return result;
    }

    @Nullable
    private Map<String, Object> serializeTrain(@Nullable Train train) {
        if (train == null || train.invalid)
            return null;

        Map<String, Object> result = new HashMap<>();

        result.put("id", train.id.toString());
        result.put("name", train.name.getString());

        int packages = 0;

        for(var carriage : train.carriages) {
            IItemHandlerModifiable inventory = carriage.storage.getAllItems();
            if (inventory == null)
                continue;

            for(int slot = 0; slot < inventory.getSlots(); slot++) {
                var box = inventory.getStackInSlot(slot);
                if (!PackageItem.isPackage(box))
                    continue;

                packages++;
            }
        }

        result.put("packages", packages);
        return result;
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final LuaTrainObject getTrain(String key) throws LuaException {
        var train = getTrainInternal(key);
        if (train == null)
            return null;

        return LuaTrainObject.of(train, isWriteable());
    }

    @Nullable
    private Train getTrainInternal(String key) throws LuaException {
        var pos = blockEntity.edgePoint.determineGraphLocation();
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch(IllegalArgumentException ex) {
            id = null;
        }

        Train train = null;
        if (id != null) {
            train = Create.RAILWAYS.trains.get(id);
            if (train == null || train.invalid || !train.graph.equals(pos.graph))
                train = null;

        } else {
            for (var thing : Create.RAILWAYS.trains.values()) {
                if (!thing.invalid && key.equals(thing.name.getString()) && thing.graph.equals(pos.graph)) {
                    if (train != null)
                        throw new LuaException("ambiguous name; use an id instead");
                    train = thing;
                }
            }
        }

        return train;
    }

    @Nullable
    private <T extends TrackEdgePoint> T getEdgePoint(EdgePointType<T> type, String key) throws LuaException {
        var pos = blockEntity.edgePoint.determineGraphLocation();
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch(IllegalArgumentException ex) {
            id = null;
        }

        if (id != null)
            return pos.graph.getPoint(type, id);

        // Only stations are named.
        if (type != EdgePointType.STATION)
            return null;

        GlobalStation station = null;
        for(var thing : pos.graph.getPoints(EdgePointType.STATION)) {
            if (key.equals(thing.name)) {
                if (station != null)
                    throw new LuaException("ambiguous name");
                station = thing;
            }
        }

        return (T) station;
    }

    private boolean isWriteable() {
        return Config.Server.trainWriting.get();
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final LuaStationObject getStation(String key) throws LuaException {
        var station = getEdgePoint(EdgePointType.STATION, key);
        if (station == null)
            return null;

        return LuaStationObject.of(station, isWriteable());
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String , ?>> listTrains() {
        var pos = blockEntity.edgePoint.determineGraphLocation();

        Map<Integer, Map<String , ?>> result = new HashMap<>();

        int i = 0;
        for(var train : Create.RAILWAYS.trains.values()) {
            if (train.invalid || !train.graph.equals(pos.graph))
                continue;

            i++;
            result.put(i, serializeTrain(train));
        }

        return result;
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, ?>> listStations() {
        var pos = blockEntity.edgePoint.determineGraphLocation();
        var stations = pos.graph.getPoints(EdgePointType.STATION);

        Map<Integer, Map<String, ?>> result = new HashMap<>();

        int i = 0;
        for(var entry : stations) {
            i++;
            result.put(i, serializeStation(entry));
        }

        return result;
    }

    @Override
    public String getType() {
        return "CreateAdditionalLogistics_TrainNetworkMonitor";
    }
}
