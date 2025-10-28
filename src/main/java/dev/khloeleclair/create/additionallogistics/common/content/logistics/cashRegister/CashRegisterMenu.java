package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import dev.khloeleclair.create.additionallogistics.common.registries.CALMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class CashRegisterMenu extends MenuBase<CashRegisterBlockEntity> {

    public CashRegisterMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public CashRegisterMenu(MenuType<?> type, int id, Inventory inv, CashRegisterBlockEntity be) {
        super(type, id, inv, be);
        be.startOpen(player);
    }

    public static AbstractContainerMenu create(int id, Inventory inv, CashRegisterBlockEntity be) {
        return new CashRegisterMenu(CALMenuTypes.CASH_REGISTER.get(), id, inv, be);
    }

    @Override
    protected CashRegisterBlockEntity createOnClient(FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        final var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof CashRegisterBlockEntity be)
            return be;
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot clickedSlot = getSlot(index);
        if (!clickedSlot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = clickedSlot.getItem();
        if (clickedSlot.container == playerInventory && stack.is(CALItems.SALES_LEDGER.get())) {
            int idx = contentHolder.invWrapper.getSlots() - 1;
            if (contentHolder.invWrapper.getStackInSlot(idx).isEmpty() && moveItemStackTo(stack, idx, idx+1, false)) {
                return ItemStack.EMPTY;
            }
        }

        int size = contentHolder.invWrapper.getSlots();
        boolean success;
        if (index < size) {
            success = !moveItemStackTo(stack, size, slots.size(), false);
        } else
            success = !moveItemStackTo(stack, 0, size, false);

        return success ? ItemStack.EMPTY : stack;
    }

    @Override
    protected void initAndReadInventory(CashRegisterBlockEntity contentHolder) { }

    @Override
    protected void addSlots() {
        var inventory = contentHolder.invWrapper;

        int x = 27;
        int y = 26;

        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(inventory, row * 9 + col, x + col * 18, y + row * 18));
            }
        }

        addSlot(new SlotItemHandler(inventory, inventory.getSlots() - 1, 27, 94));

        addPlayerSlots(38, 150);
    }

    @Override
    protected void saveData(CashRegisterBlockEntity contentHolder) { }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        if (!playerIn.level().isClientSide)
            contentHolder.stopOpen(playerIn);
    }

    @Override
    public boolean stillValid(Player player) {
        return !contentHolder.isRemoved();
    }
}
