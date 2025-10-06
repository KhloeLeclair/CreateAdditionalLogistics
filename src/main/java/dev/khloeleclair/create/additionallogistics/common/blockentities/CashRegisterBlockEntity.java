package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.blocks.CashRegisterBlock;
import dev.khloeleclair.create.additionallogistics.common.data.CustomComponents;
import dev.khloeleclair.create.additionallogistics.common.data.SalesHistoryData;
import dev.khloeleclair.create.additionallogistics.common.menu.CashRegisterMenu;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CashRegisterBlockEntity extends StockTickerBlockEntity {

    @Nullable
    public AbstractComputerBehaviour computerBehavior;

    private ItemStack ledger;
    private final IItemHandlerModifiable ledgerHandler;
    public final CombinedInvWrapper invWrapper;
    private final ContainerOpenersCounter openersCounter;

    private int saleTicks;

    public CashRegisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ledger = ItemStack.EMPTY;
        saleTicks = 0;

        openersCounter = new ContainerOpenersCounter() {
            @Override
            protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
                CashRegisterBlockEntity.this.playSound(blockState, AllSoundEvents.STOCK_TICKER_TRADE.getMainEvent());
                CashRegisterBlockEntity.this.updateBlockState(blockState, true);
            }

            @Override
            protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
                CashRegisterBlockEntity.this.updateBlockState(blockState, false);
            }

            @Override
            protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int i1) {

            }

            @Override
            protected boolean isOwnContainer(Player player) {
                if (player.containerMenu instanceof CashRegisterMenu menu)
                    return menu.contentHolder == CashRegisterBlockEntity.this;
                return false;
            }
        };

        ledgerHandler = new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (isItemValid(slot, stack))
                    ledger = stack;
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ledger;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // Only allow extraction.
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 1 && amount < 1)
                    return ItemStack.EMPTY;

                var result = ledger;
                if (!simulate)
                    ledger = ItemStack.EMPTY;

                notifyUpdate();
                return result;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? 1 : 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot != 0 || stack.isEmpty() || !stack.is(CALItems.SALES_LEDGER) || stack.getCount() != 1)
                    return false;

                var history = stack.get(CustomComponents.SALES_HISTORY);
                return history == null || history.saleCount() < 1000;
            }
        };

        invWrapper = new CombinedInvWrapper(receivedPayments, ledgerHandler);
    }

    public boolean hadRecentSale() {
        return saleTicks > 0;
    }

    void updateBlockState(BlockState state, boolean open) {
        if (level != null)
            level.setBlock(worldPosition, state.setValue(CashRegisterBlock.OPEN, open), 3);
    }

    void playSound(BlockState state, SoundEvent sound) {
        if (level == null)
            return;

        Vec3i face = state.getValue(CashRegisterBlock.FACING).getNormal();
        double x = worldPosition.getX() + 0.5 + face.getX() / 2.0;
        double y = worldPosition.getY() + 0.5 + face.getY() / 2.0;
        double z = worldPosition.getZ() + 0.5 + face.getZ() / 2.0;
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, 1f);
    }

    public void startOpen(Player player) {
        if (!remove && !player.isSpectator() && level != null)
            this.openersCounter.incrementOpeners(player, level, worldPosition, getBlockState());
    }

    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator() && ledger != null)
            openersCounter.decrementOpeners(player, level, worldPosition, getBlockState());
    }

    public void recheckOpen() {
        if (!remove && level != null)
            openersCounter.recheckOpeners(level, worldPosition, getBlockState());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CALBlockEntityTypes.CASH_REGISTER.get(),
                (be, context) -> context == Direction.DOWN ? be.receivedPayments : context == Direction.UP ? be.ledgerHandler : be.invWrapper
        );

        if (Mods.COMPUTERCRAFT.isLoaded()) {
            event.registerBlockEntity(
                    PeripheralCapability.get(),
                    CALBlockEntityTypes.CASH_REGISTER.get(),
                    (be, context) -> be.computerBehavior.getPeripheralCapability()
            );
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // Delete any inherited behavior from the Stock Ticker, and add our own.
        behaviours.removeIf(x -> x instanceof AbstractComputerBehaviour);
        behaviours.add(computerBehavior = CALComputerCraftProxy.behavior(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (saleTicks > 0)
            saleTicks--;
    }

    @Override
    public void destroy() {
        ItemHelper.dropContents(level, worldPosition, ledgerHandler);
        super.destroy();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (ledger != null && !ledger.isEmpty())
            tag.put("Ledger", ledger.save(registries));
        tag.putInt("SaleTicks", saleTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        ledger = ItemStack.EMPTY;
        if (tag.contains("Ledger", CompoundTag.TAG_COMPOUND))
            ledger = ItemStack.parseOptional(registries, tag.getCompound("Ledger"));
        saleTicks = tag.getInt("SaleTicks");
    }

    public static void recordSale(Level level, BlockPos pos, Player player, ItemStack mainHandItem) {
        if (level.isClientSide || mainHandItem.isEmpty() || !(level.getBlockEntity(pos) instanceof CashRegisterBlockEntity be))
            return;

        // Make sure the player isn't still holding the shopping list, since
        // that would indicate a failed sale.
        if (player.getMainHandItem().equals(mainHandItem))
            return;

        var list = ShoppingListItem.getList(mainHandItem);
        if (list == null)
            return;

        be.recordSale(player, list);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (receivedPayments.isEmpty() && ledger.isEmpty())
            return false;
        if (!behaviour.mayAdministrate(Minecraft.getInstance().player))
            return false;

        if (!ledger.isEmpty() && ledger.get(CustomComponents.SALES_HISTORY) instanceof SalesHistoryData history) {
            CALLang.translate("sales.sales", CALLang.number(history.saleCount()).style(ChatFormatting.GOLD))
                    .style(ChatFormatting.WHITE)
                    .forGoggles(tooltip);

            var cmp = history.getTimeRange(ChatFormatting.AQUA);
            if (cmp != null)
                CALLang.builder().add(cmp.withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);

            if (!receivedPayments.isEmpty())
                tooltip.add(Component.literal(""));
        }

        if (!receivedPayments.isEmpty()) {
            CreateLang.translate("stock_ticker.contains_payments")
                    .style(ChatFormatting.WHITE)
                    .forGoggles(tooltip);

            InventorySummary summary = new InventorySummary();
            for (int i = 0; i < receivedPayments.getSlots(); i++)
                summary.add(receivedPayments.getStackInSlot(i));
            for (BigItemStack entry : summary.getStacksByCount())
                CreateLang.builder()
                        .text(Component.translatable(entry.stack.getDescriptionId())
                                .getString() + " x" + entry.count)
                        .style(ChatFormatting.GREEN)
                        .forGoggles(tooltip);
        }

        return true;
    }

    public void recordSale(Player player, ShoppingListItem.ShoppingList list) {

        var baked = list.bakeEntries(level, null);
        var payment = baked.getSecond();
        var order = baked.getFirst();

        // Get the sales item.
        var history = ledger.isEmpty() ? null : ledger.get(CustomComponents.SALES_HISTORY);
        if (history == null)
            history = SalesHistoryData.EMPTY;

        // Try making a new ledger if the existing one has too many sales.
        if (history.saleCount() >= 1000) {
            ledger = ItemHandlerHelper.insertItem(receivedPayments, ledger, false);
            if (ledger.isEmpty())
                history = SalesHistoryData.EMPTY;
            else
                // But if we can't, abort to avoid growing the NBT too big.
                return;
        }

        history = history.withBigSale(player.getUUID(), payment.getStacks(), order.getStacks());

        if (ledger.isEmpty())
            ledger = CALItems.SALES_LEDGER.asStack();

        ledger.set(CustomComponents.SALES_HISTORY, history);
        saleTicks = 20;
        notifyUpdate();
    }

    public SmartInventory getReceivedPaymentsHandler() {
        return receivedPayments;
    }

    public ItemStack getLedger() {
        return ledger;
    }


    public class CashRegisterMenuProvider implements MenuProvider {

        @Override
        public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return CashRegisterMenu.create(id, inventory, CashRegisterBlockEntity.this);
        }

        @Override
        public Component getDisplayName() {
            return CashRegisterBlockEntity.this.getBlockState().getBlock().getName();
        }
    }


}
