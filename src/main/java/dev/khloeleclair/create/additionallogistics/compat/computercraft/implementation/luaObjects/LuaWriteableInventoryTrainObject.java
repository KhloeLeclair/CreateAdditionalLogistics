package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.trains.entity.Train;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.Map;
import java.util.Optional;

public class LuaWriteableInventoryTrainObject extends LuaWriteableTrainObject {

    protected LuaWriteableInventoryTrainObject(Train train) {
        super(train);
    }

    @LuaFunction(mainThread = true)
    public final Integer size() {
        return sizeItemsInternal();
    }

    @LuaFunction(mainThread = true)
    public final Integer getItemLimit(int slot) throws LuaException {
        return getItemLimitInternal(slot);
    }

    @LuaFunction(mainThread = true)
    public final Map<?, ?> getItemDetail(int slot) throws LuaException {
        return getItemDetailInternal(slot);
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Object> list(Optional<Boolean> only_packages) {
        boolean skip_non_packages = only_packages.orElse(false);
        return listItemsInternal(skip_non_packages);
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Object> tanks() {
        return tanksInternal();
    }

}
