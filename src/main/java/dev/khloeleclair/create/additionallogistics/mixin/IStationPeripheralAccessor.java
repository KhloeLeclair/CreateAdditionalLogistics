package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StationPeripheral;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StationPeripheral.class)
public interface IStationPeripheralAccessor {

    @Invoker(remap = false)
    static CreateLuaTable callFromCompoundTag(CompoundTag tag) throws LuaException {
        throw new AssertionError();
    }

    @Invoker(remap = false)
    static CompoundTag callToCompoundTag(CreateLuaTable table) throws LuaException {
        throw new AssertionError();
    }

}
