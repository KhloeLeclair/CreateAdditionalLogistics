package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.CALComputerUtil;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LuaStationObject implements LuaComparable {

    protected final GlobalStation station;

    protected LuaStationObject(GlobalStation station) {
        this.station = station;
    }

    public static LuaStationObject of(GlobalStation station, boolean writeable) {
        return writeable ? new LuaWriteableStationObject(station) : new LuaStationObject(station);
    }

    protected boolean isWritable() {
        return false;
    }

    protected void assertTrainPresent() throws LuaException {
        if (station.getPresentTrain() == null)
            throw new LuaException("there is no train present");
    }

    @Nullable
    protected Level getLevel() {
        var server = CreateAdditionalLogistics.getServer();
        if (server == null)
            return null;

        return server.getLevel(station.blockEntityDimension);
    }

    @Nullable
    protected StationBlockEntity getBlockEntity() {
        var level = getLevel();
        if (level == null || !level.isLoaded(station.blockEntityPos) || !(level.getBlockEntity(station.blockEntityPos) instanceof StationBlockEntity sbe))
            return null;

        return sbe;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getPosition() {
        return CALComputerUtil.getPosition(station.blockEntityPos, station.blockEntityDimension);
    }

    @LuaFunction(mainThread = true)
    public final String getName() {
        return station.name;
    }

    @LuaFunction(mainThread = true)
    public final String getId() {
        return station.id.toString();
    }

    @LuaFunction(mainThread = true)
    public final boolean isLoaded() {
        return getBlockEntity() != null;
    }

    @LuaFunction(mainThread = true)
    public final boolean isInAssemblyMode() {
        return station.assembling;
    }

    @LuaFunction(mainThread = true)
    public final boolean isTrainPresent() {
        return station.getPresentTrain() != null;
    }

    @LuaFunction(mainThread = true)
    public final boolean isTrainImminent() {
        return station.getImminentTrain() != null;
    }

    @LuaFunction(mainThread = true)
    public final boolean isTrainEnroute() {
        return station.getNearestTrain() != null;
    }

    @LuaFunction(mainThread = true)
    public final int sizePostboxes() {
        return station.connectedPorts.size();
    }

    @LuaFunction(mainThread = true)
    public final LuaPostboxObject getPostbox(int slot) throws LuaException {
        int i = 0;
        for(var portEntry : station.connectedPorts.entrySet()) {
            i++;

            if (i == slot)
                return LuaPostboxObject.of(station, portEntry.getKey(), portEntry.getValue(), isWritable());
        }

        throw new LuaException("Slot " + slot + " out of range, expected between " + 1 + " and " + station.connectedPorts.size());
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final LuaTrainObject getTrain() {
        var train = station.getPresentTrain();
        if (train == null)
            return null;

        return LuaTrainObject.of(train, isWritable());
    }

    @Override
    public Map<?, ?> getTableRepresentation() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", station.id);
        return result;
    }

}
