package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.AllPackets;
import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.station.TrainEditPacket;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.mixin.IStationPeripheralAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

public class LuaWriteableTrainObject extends LuaTrainObject {

    protected LuaWriteableTrainObject(Train train) {
        super(train);
    }

    @Override
    protected boolean isWriteable() {
        return true;
    }

    @LuaFunction(mainThread = true)
    public final void setThrottle(double value) {
        train.throttle = Mth.clamp(value, 1 / 18f, 1);
    }

    @LuaFunction(mainThread = true)
    public final void setName(String name) {
        if (name.equals(train.name.getString()))
            return;

        train.name = Component.literal(name);
        AllPackets.getChannel().send(PacketDistributor.ALL.noArg(), new TrainEditPacket.TrainEditReturnPacket(train.id, name, train.icon.getId(), train.mapColorIndex));
    }

    @LuaFunction(mainThread = true)
    public final void setSchedule(IArguments arguments) throws LuaException {
        var level = getLevel();
        if (level == null)
            throw new LuaException("train in unknown dimension");

        Schedule schedule = Schedule.fromTag(IStationPeripheralAccessor.callToCompoundTag(new CreateLuaTable(arguments.getTable(0))));
        if (schedule.entries.isEmpty())
            throw new LuaException("Schedule must have at least one entry");

        boolean autoSchedule = train.runtime.getSchedule() == null || train.runtime.isAutoSchedule;
        train.runtime.setSchedule(schedule, autoSchedule);
    }

}
