package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.trains.station.GlobalPackagePort;
import com.simibubi.create.content.trains.station.GlobalStation;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.ComputerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LuaPostboxObject implements LuaComparable {

    protected final GlobalStation station;
    protected final BlockPos pos;
    protected final GlobalPackagePort port;

    protected LuaPostboxObject(GlobalStation station, BlockPos pos, GlobalPackagePort port) {
        this.station = station;
        this.pos = pos;
        this.port = port;
    }

    public static LuaPostboxObject of(GlobalStation station, BlockPos pos, GlobalPackagePort port, boolean writeable) {
        return writeable ? new LuaWriteablePostboxObject(station, pos, port) : new LuaPostboxObject(station, pos, port);
    }

    protected boolean isWriteable() {
        return false;
    }

    @Nullable
    protected Level getLevel() {
        var server = CreateAdditionalLogistics.getServer();
        if (server == null)
            return null;

        return server.getLevel(station.blockEntityDimension);
    }

    @Nullable
    protected PostboxBlockEntity getBlockEntity() {
        var level = getLevel();
        if (level == null || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof PostboxBlockEntity pbbe))
            return null;

        return pbbe;
    }

    protected IItemHandlerModifiable getInventory() {
        var ppbe = getBlockEntity();
        if (ppbe == null || port.primed)
            return port.offlineBuffer;

        return ppbe.inventory;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getPosition() {
        return ComputerUtil.getPosition(pos, station.blockEntityDimension);
    }

    @LuaFunction(mainThread = true)
    public final String getAddress() {
        return port.address;
    }

    @LuaFunction(mainThread = true)
    public final boolean isLoaded() {
        return getBlockEntity() != null;
    }

    @LuaFunction(mainThread = true)
    public final boolean hasIncomingPackages() {
        var inv = getInventory();

        for(int slot = 0; slot < inv.getSlots(); slot++) {
            var stack = inv.getStackInSlot(slot);
            if (PackageItem.isPackage(stack) && PackageItem.matchAddress(stack, port.address))
                return true;
        }

        return false;
    }

    @LuaFunction(mainThread = true)
    public final boolean hasOutgoingPackages() {
        var inv = getInventory();

        for(int slot = 0; slot < inv.getSlots(); slot++) {
            var stack = inv.getStackInSlot(slot);
            if (PackageItem.isPackage(stack) && !PackageItem.matchAddress(stack, port.address))
                return true;
        }

        return false;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Integer> listPackageDestinations() {
        var inv = getInventory();

        Map<String, Integer> result = new HashMap<>();

        for(int slot = 0; slot < inv.getSlots(); slot++) {
            var stack = inv.getStackInSlot(slot);
            if (PackageItem.isPackage(stack)) {
                var boxAddress = PackageItem.getAddress(stack);
                if (!PackageItem.matchAddress(boxAddress, port.address))
                    result.put(boxAddress, result.getOrDefault(boxAddress, 0) + 1);
            }
        }

        return result;
    }

    @LuaFunction(mainThread = true)
    public final LuaStationObject getStation() {
        return LuaStationObject.of(station, isWriteable());
    }

    @LuaFunction(mainThread = true)
    public final String getConfiguration() throws LuaException {
        var pbbe = getBlockEntity();
        if (pbbe == null)
            throw new LuaException("postbox must be in loaded chunk");

        if (pbbe.acceptsPackages)
            return "send_receive";
        else
            return "send";
    }

    @Override
    public Map<?, ?> getTableRepresentation() {
        Map<String, Object> result = new HashMap<>();

        result.put("id", station.id);
        result.put("pos", ComputerUtil.getPosition(pos));

        return result;
    }
}
