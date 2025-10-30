package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.ComputerUtil;
import dev.khloeleclair.create.additionallogistics.mixin.IStationPeripheralAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LuaTrainObject implements LuaComparable {

    protected final Train train;

    public static LuaTrainObject of(Train train, boolean writeable) {
        boolean allow_inventory = Config.Server.trainInventoryAccess.get();

        if (writeable) {
            return allow_inventory ? new LuaWriteableInventoryTrainObject(train) : new LuaWriteableTrainObject(train);
        }

        return allow_inventory ? new LuaInventoryTrainObject(train) : new LuaTrainObject(train);
    }

    protected LuaTrainObject(Train train) {
        this.train = train;
    }

    protected boolean isWriteable() {
        return false;
    }


    @Nullable
    protected ResourceKey<Level> getDimension() {
        for(var carriage : train.carriages) {
            var dimensions = carriage.getPresentDimensions();
            if (dimensions.isEmpty())
                continue;

            return dimensions.get(0);
        }

        return null;
    }

    @Nullable
    protected Level getLevel() {
        var server = CreateAdditionalLogistics.getServer();
        if (server == null)
            return null;

        for(var carriage : train.carriages) {
            var dimensions = carriage.getPresentDimensions();
            if (dimensions.isEmpty())
                continue;

            var level = server.getLevel(dimensions.get(0));
            if (level != null)
                return level;
        }

        return null;
    }

    @LuaFunction(mainThread = true)
    public final String getId() {
        return train.id.toString();
    }

    @LuaFunction(mainThread = true)
    public final boolean isDerailed() {
        return train.derailed;
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final LuaStationObject getStation() {
        var station = train.getCurrentStation();
        if (station == null)
            return null;

        return LuaStationObject.of(station, isWriteable());
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed() {
        return train.speed;
    }

    @LuaFunction(mainThread = true)
    public final double getTargetSpeed() {
        return train.targetSpeed;
    }

    @LuaFunction(mainThread = true)
    public final double getThrottle() {
        return train.throttle;
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getPosition() {
        var dimension = getDimension();
        Optional<BlockPos> pos = dimension == null ? Optional.empty() : train.getPositionInDimension(dimension);
        if (pos.isEmpty())
            return null;

        return ComputerUtil.getPosition(pos.get(), dimension);
    }

    @LuaFunction(mainThread = true)
    public final String getName() {
        return train.name.getString();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Integer> listPackageDestinations() {
        Map<String, Integer> result = new HashMap<>();

        for(var carriage : train.carriages) {
            var inventory = carriage.storage.getAllItems();
            if (inventory == null)
                continue;

            for(int slot = 0; slot < inventory.getSlots(); slot++) {
                var stack = inventory.getStackInSlot(slot);
                if (!PackageItem.isPackage(stack))
                    continue;

                var address = PackageItem.getAddress(stack);
                result.put(address, result.getOrDefault(address, 0) + 1);
            }
        }

        return result;
    }

    protected Integer sizeItemsInternal() {
        int i = 0;
        for(var carriage : train.carriages) {
            var inventory = carriage.storage.getAllItems();
            if (inventory != null)
                i += inventory.getSlots();
        }
        return i;
    }

    protected Map<?, ?> getItemDetailInternal(int slot) throws LuaException {
        int i = slot - 1;
        int total = 0;

        for(var carriage : train.carriages) {
            var inventory = carriage.storage.getAllItems();
            if (inventory == null)
                continue;

            total += inventory.getSlots();
            if (i >= inventory.getSlots()) {
                i -= inventory.getSlots();
                continue;

            } else if (i < 0)
                break;

            var stack = inventory.getStackInSlot(i);
            return VanillaDetailRegistries.ITEM_STACK.getDetails(stack);
        }

        throw new LuaException("Slot " + slot + " out of range, available slots between " + 1 + " and " + total);
    }

    protected Integer getItemLimitInternal(int slot) throws LuaException {
        int i = slot - 1;
        int total = 0;

        for(var carriage : train.carriages) {
            var inventory = carriage.storage.getAllItems();
            if (inventory == null)
                continue;

            total += inventory.getSlots();
            if (i >= inventory.getSlots()) {
                i -= inventory.getSlots();
                continue;

            } else if (i < 0)
                break;

            return inventory.getSlotLimit(i);
        }

        throw new LuaException("Slot " + slot + " out of range, available slots between " + 1 + " and " + total);
    }

    protected Map<Integer, Object> listItemsInternal(boolean skip_non_packages) {
        Map<Integer, Object> result = new HashMap<>();
        int i = 0;

        for(var carriage : train.carriages) {
            var inventory = carriage.storage.getAllItems();
            if (inventory == null)
                continue;

            for(int slot = 0; slot < inventory.getSlots(); slot++) {
                var stack = inventory.getStackInSlot(slot);
                i++;

                if (stack.isEmpty() || (skip_non_packages && !PackageItem.isPackage(stack)))
                    result.put(i, null);
                else
                    result.put(i, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
            }
        }

        return result;
    }

    protected Map<Integer, Object> tanksInternal() {
        Map<Integer, Object> result = new HashMap<>();
        int i = 0;

        for(var carriage : train.carriages) {
            var fluids = carriage.storage.getFluids();
            if (fluids == null)
                continue;

            for(int tank = 0; tank < fluids.getTanks(); tank++) {
                i++;
                var fluid = fluids.getFluidInTank(tank);
                if (fluid == null || fluid.isEmpty()) {
                    result.put(i, null);
                    continue;
                }

                Map<String, Object> tankData = new HashMap<>();

                tankData.put("capacity", fluids.getTankCapacity(tank));
                tankData.put("amount", fluid.getAmount());

                var holder = fluid.getFluid().builtInRegistryHolder();
                String key = (holder == null || holder.key() == null) ? null : holder.key().location().toString();

                tankData.put("name", key);

                result.put(i, tankData);
            }
        }

        return result;
    }


    @LuaFunction(mainThread = true)
    public final boolean hasSchedule() {
        return train.runtime.getSchedule() != null;
    }

    @Nullable
    @LuaFunction(mainThread = true)
    public final CreateLuaTable getSchedule() throws LuaException {
        Schedule schedule = train.runtime.getSchedule();
        if (schedule == null)
            return null;

        var level = getLevel();
        if (level == null)
            throw new LuaException("train in unknown dimension");

        return IStationPeripheralAccessor.callFromCompoundTag(schedule.write());
    }

    public Map<?, ?> getTableRepresentation() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", train.id);
        return result;
    }

}
