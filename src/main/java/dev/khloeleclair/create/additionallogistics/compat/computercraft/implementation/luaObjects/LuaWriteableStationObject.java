package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.trains.station.GlobalStation;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public class LuaWriteableStationObject extends LuaStationObject {

    protected LuaWriteableStationObject(GlobalStation station) {
        super(station);
    }

    @Override
    protected boolean isWritable() {
        return true;
    }

    @LuaFunction(mainThread = true)
    public final void assemble() throws LuaException {
        var sbe = getBlockEntity();
        if (sbe == null)
            throw new LuaException("station must be in loaded chunk");

        if (!sbe.isAssembling())
            throw new LuaException("station must be in assembly mode");

        sbe.assemble(null);

        if (sbe.getStation() == null || sbe.getStation().getPresentTrain() == null)
            throw new LuaException("failed to assemble train");

        if (!sbe.exitAssemblyMode())
            throw new LuaException("failed to exit assembly mode");
    }

    @LuaFunction(mainThread = true)
    public final void disassemble() throws LuaException {
        var sbe = getBlockEntity();
        if (sbe == null)
            throw new LuaException("station must be in loaded chunk");

        if (sbe.isAssembling())
            throw new LuaException("station must not be in assembly mode");

        assertTrainPresent();

        if (!sbe.enterAssemblyMode(null))
            throw new LuaException("could not disassemble train");
    }

    @LuaFunction(mainThread = true)
    public final void setAssemblyMode(boolean assemblyMode) throws LuaException {
        var sbe = getBlockEntity();
        if (sbe == null)
            throw new LuaException("station must be in loaded chunk");

        if (assemblyMode) {
            if (!sbe.enterAssemblyMode(null))
                throw new LuaException("failed to enter assembly mode");
        } else {
            if (!sbe.exitAssemblyMode())
                throw new LuaException("failed to exit assembly mode");
        }
    }

    @LuaFunction(mainThread = true)
    public final void setName(String name) throws LuaException {
        var sbe = getBlockEntity();
        if (sbe == null)
            throw new LuaException("station must be in loaded chunk");

        if (!sbe.updateName(name))
            throw new LuaException("could not set station name");
    }

}
