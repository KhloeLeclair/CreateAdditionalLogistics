package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.trains.station.GlobalPackagePort;
import com.simibubi.create.content.trains.station.GlobalStation;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;

public class LuaWriteablePostboxObject extends LuaPostboxObject {

    protected LuaWriteablePostboxObject(GlobalStation station, BlockPos pos, GlobalPackagePort port) {
        super(station, pos, port);
    }

    @Override
    protected boolean isWriteable() {
        return true;
    }

    @LuaFunction(mainThread = true)
    public final void setAddress(String address) throws LuaException {
        var pbbe = getBlockEntity();
        if (pbbe == null)
            throw new LuaException("postbox must be in loaded chunk");

        pbbe.addressFilter = address;
        pbbe.filterChanged();
        pbbe.notifyUpdate();
    }

    @LuaFunction(mainThread = true)
    public final boolean setConfiguration(String config) throws LuaException {
        var pbbe = getBlockEntity();
        if (pbbe == null)
            throw new LuaException("postbox must be in loaded chunk");

        // Should never happen, but this is for the sake of parity.
        if (pbbe.target == null)
            return false;

        if (config.equals("send_receive") || config.equals("send_recieve")) {
            pbbe.acceptsPackages = true;
            pbbe.filterChanged();
            pbbe.notifyUpdate();
            return true;
        }

        if (config.equals("send")) {
            pbbe.acceptsPackages = false;
            pbbe.filterChanged();
            pbbe.notifyUpdate();
            return true;
        }

        throw new LuaException("Unknown configuration: \"" + config + "\" Possible configurations are: \"send_receive\" and \"send\".");
    }

}
