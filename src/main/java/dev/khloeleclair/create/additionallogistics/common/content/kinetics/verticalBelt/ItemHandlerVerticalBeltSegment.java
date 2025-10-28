package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemHandlerVerticalBeltSegment implements IItemHandler {

    private final VerticalBeltInventory beltInventory;
    int offset;

    public ItemHandlerVerticalBeltSegment(VerticalBeltInventory beltInventory, int offset) {
        this.beltInventory = beltInventory;
        this.offset = offset;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        TransportedItemStack stack = beltInventory.getStackAtOffset(offset);
        if (stack == null)
            return ItemStack.EMPTY;
        return stack.stack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (beltInventory.canInsertAt(offset)) {
            ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
            if (!simulate) {
                TransportedItemStack newStack = new TransportedItemStack(stack);
                newStack.insertedAt = offset;
                newStack.beltPosition = offset + 0.5f + (beltInventory.beltMovementPositive ? -1 : 1) / 16f;
                newStack.prevBeltPosition = newStack.beltPosition;
                beltInventory.addItem(newStack);
                beltInventory.belt.setChanged();
                beltInventory.belt.sendData();
            }
            return remainder;
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        TransportedItemStack transported = beltInventory.getStackAtOffset(offset);
        if (transported == null)
            return ItemStack.EMPTY;

        amount = Math.min(amount, transported.stack.getCount());
        ItemStack extracted = simulate ? transported.stack.copy()
                .split(amount) : transported.stack.split(amount);
        if (!simulate) {
            if (transported.stack.isEmpty())
                beltInventory.toRemove.add(transported);
            else
                beltInventory.belt.notifyUpdate();
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Math.min(getStackInSlot(slot).getOrDefault(DataComponents.MAX_STACK_SIZE, 64), 64);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
