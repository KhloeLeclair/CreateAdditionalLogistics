package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class VerticalBeltInventory {

    final VerticalBeltBlockEntity belt;
    private final List<TransportedItemStack> items;
    final List<TransportedItemStack> toInsert;
    final List<TransportedItemStack> toRemove;
    boolean beltMovementPositive;
    final float SEGMENT_WINDOW = 0.75f;

    @Nullable
    TransportedItemStack lazyClientItem;

    public VerticalBeltInventory(VerticalBeltBlockEntity be) {
        belt = be;
        items = new LinkedList<>();
        toInsert = new LinkedList<>();
        toRemove = new LinkedList<>();
    }

    public void tick() {

    }

    protected TransportedItemStackHandlerBehaviour getTransportedItemStackHandlerAtSegment(int segment) {
        return BlockEntityBehaviour.get(belt.getLevel(), VerticalBeltHelper.getPositionForOffset(belt, segment), TransportedItemStackHandlerBehaviour.TYPE);
    }

    private enum Ending {
        UNRESOLVED(0),
        EJECT(0),
        INSERT(.25f),
        BLOCKED(.45f);

        private float margin;

        Ending(float f) {
            margin = f;
        }
    }

    private Ending resolveEnding() {
        Level world = belt.getLevel();
        BlockPos nextPosition = VerticalBeltHelper.getPositionForOffset(belt, beltMovementPositive ? belt.beltHeight : -1);

        DirectBeltInputBehaviour inputBehaviour =
                BlockEntityBehaviour.get(world, nextPosition, DirectBeltInputBehaviour.TYPE);
        if (inputBehaviour != null)
            return Ending.INSERT;

        if (BlockHelper.hasBlockSolidSide(world.getBlockState(nextPosition), world, nextPosition, beltMovementPositive ? Direction.DOWN : Direction.UP))
            return Ending.BLOCKED;

        return Ending.EJECT;
    }

    public boolean canInsertAt(int segment) {
        return canInsertAtFromSide(segment, Direction.NORTH);
    }

    public boolean canInsertAtFromSide(int segment, Direction side) {
        return false;
    }


    public void addItem(TransportedItemStack newStack) {
        toInsert.add(newStack);
    }

    private void insert(TransportedItemStack newStack) {
        if (items.isEmpty())
            items.add(newStack);
        else {
            int index = 0;
            for(TransportedItemStack stack : items) {
                if (stack.compareTo(newStack) > 0 == beltMovementPositive)
                    break;
                index++;
            }
            items.add(index, newStack);
        }
    }

    @Nullable
    public TransportedItemStack getStackAtOffset(int offset) {
        float min = offset;
        float max = offset + 1;
        for(TransportedItemStack stack : items) {
            if (toRemove.contains(stack))
                continue;
            if (stack.beltPosition > max)
                continue;
            if (stack.beltPosition > min)
                return stack;
        }
        return null;
    }

    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        items.clear();
        nbt.getList("Items", CompoundTag.TAG_COMPOUND)
                .forEach(inbt -> items.add(TransportedItemStack.read((CompoundTag) inbt, registries)));
        if (nbt.contains("LazyItem"))
            lazyClientItem = TransportedItemStack.read(nbt.getCompound("LazyItem"), registries);
        beltMovementPositive = nbt.getBoolean("PositiveOrder");
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsTag = new ListTag();
        items.forEach(stack -> itemsTag.add(stack.serializeNBT(registries)));
        tag.put("Items", itemsTag);
        if (lazyClientItem != null)
            tag.put("LazyItem", lazyClientItem.serializeNBT(registries));
        tag.putBoolean("PositiveOrder", beltMovementPositive);
        return tag;
    }

    public List<TransportedItemStack> getTransportedItems() {
        return items;
    }

    @Nullable
    public TransportedItemStack getLazyClientItem() {
        return lazyClientItem;
    }


}
