package dev.khloeleclair.create.additionallogistics.common.utilities;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.BlueprintOverlayShopContext;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.api.Currency;
import dev.khloeleclair.create.additionallogistics.api.ICurrency;
import dev.khloeleclair.create.additionallogistics.api.ICurrencyBuilder;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.mixin.IStockTickerBlockEntityAccessor;
import dev.khloeleclair.create.additionallogistics.mixin.client.IBlueprintOverlayRendererAccessor;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CurrencyUtilities {

    public static final Map<ResourceLocation, ICurrency> CURRENCIES = new Object2ObjectOpenHashMap<>();
    public static final Map<Item, ResourceLocation> API_CURRENCIES = new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceLocation, ICurrency.IValueFormatter> FORMATTERS = new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceLocation, ICurrency.IValueFormatter> BUILTIN_FORMATTERS = new Object2ObjectOpenHashMap<>();

    private static boolean isPopulated;

    static {
        // Numismatics Support
        BUILTIN_FORMATTERS.put(ResourceLocation.tryBuild("numismatics", "coins"), (value, ctx) -> {
            /*if (ctx.hasShiftDown())
                return null;*/

            long cogs = value / 64;
            int spurs = (int) (value % 64);

            var result = Component.literal(String.valueOf(spurs)).append("¤");

            if (cogs > 0) {
                // Yes this is a terrible way to handle pluralization.
                // This is also the way Numismatics itself handles it, so...
                var new_result = CALLang.number(cogs).text(" ").text(Component.translatable("item.numismatics.cog").getString().toLowerCase(Locale.ROOT) + (cogs == 1 ? "" : "s"));
                if (spurs == 0)
                    return new_result.component();

                result = new_result.text(", ").add(result).component();
            }

            return result;
        });
    }

    public static boolean isConversionEnabled(boolean is_cash_register) {
        if (!is_cash_register && !Config.Server.currencyStockTicker.get())
            return false;
        if (Config.Server.currencyCompression.get())
            return true;
        ensurePopulated();
        return ! CURRENCIES.isEmpty();
    }

    @Nullable
    public static ICurrency.IValueFormatter getFormatter(ResourceLocation id) {
        synchronized (FORMATTERS) {
            if (FORMATTERS.containsKey(id))
                return FORMATTERS.get(id);
        }

        synchronized (BUILTIN_FORMATTERS) {
            return BUILTIN_FORMATTERS.get(id);
        }
    }

    public static void registerCurrency(ResourceLocation id, ICurrency currency) {
        synchronized (CURRENCIES) {
            CURRENCIES.put(id, currency);
        }
        synchronized (API_CURRENCIES) {
            for(var item : currency.getItems())
                API_CURRENCIES.put(item, currency.getId());
        }
    }

    public static void registerFormatter(ResourceLocation id, ICurrency.IValueFormatter formatter) {
        synchronized (FORMATTERS) {
            FORMATTERS.put(id, formatter);
        }
    }

    /*public static void onDataMapUpdated(DataMapsUpdatedEvent event) {
        if (event.getRegistryKey() != Registries.ITEM || !isPopulated)
            return;

        // Remove any currency not involved with the API.
        synchronized (CURRENCIES) {
            if (API_CURRENCIES.isEmpty())
                CURRENCIES.clear();
            else
                CURRENCIES.keySet().removeIf(x -> ! API_CURRENCIES.containsValue(x));
        }

        isPopulated = false;
    }*/

    private static void ensurePopulated() {
        if (isPopulated)
            return;

        isPopulated = true;

        // TODO: Alternative for data maps on 1.20?
    }

    @Nullable
    public static ICurrency get(ResourceLocation id) {
        ensurePopulated();
        synchronized (CURRENCIES) {
            return CURRENCIES.get(id);
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static ICurrency getForItem(Item item) {
        ensurePopulated();
        ResourceLocation id;

        synchronized (API_CURRENCIES) {
            id = API_CURRENCIES.get(item);
        }

        if (id != null)
            synchronized (CURRENCIES) {
                return CURRENCIES.get(id);
            }

        return RecipeHelper.getCompactingCurrency(item);
    }

    public static void interactWithShop(Player player, Level level, BlockPos targetPos, ItemStack mainHandItem) {
        if (level.isClientSide || !(level.getBlockEntity(targetPos) instanceof StockTickerBlockEntity tickerBE))
            return;

        ShoppingListItem.ShoppingList list = ShoppingListItem.getList(mainHandItem);
        if (list == null)
            return;

        if (!tickerBE.behaviour.freqId.equals(list.shopNetwork())) {
            AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
            CreateLang.translate("stock_keeper.wrong_network")
                    .style(ChatFormatting.RED)
                    .sendStatus(player);
            return;
        }

        Couple<InventorySummary> bakeEntries = list.bakeEntries(level, null);
        var payment = splitCost(player, bakeEntries.getSecond().getStacksByCount());

        Map<ICurrency, Long> paymentCurrencies = payment.getFirst();
        List<BigItemStack> paymentOther = payment.getSecond();

        InventorySummary orderEntries = bakeEntries.getFirst();
        PackageOrder order = new PackageOrder(orderEntries.getStacksByCount());

        // Must be up-to-date
        tickerBE.getAccurateSummary();

        // Check stock levels
        InventorySummary recentSummary = tickerBE.getRecentSummary();
        for (BigItemStack entry : order.stacks()) {
            if (recentSummary.getCountOf(entry.stack) >= entry.count)
                continue;

            AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
            CreateLang.translate("stock_keeper.stock_level_too_low")
                    .style(ChatFormatting.RED)
                    .sendStatus(player);
            return;
        }

        // Determine which items from the player will be consumed as payment.
        // This is necessary for checking that the cash register has room.
        InventorySummary consumed = new InventorySummary();
        paymentOther.forEach(consumed::add);

        for(var entry : paymentCurrencies.entrySet())
            entry.getKey().getStacksWithValue(entry.getValue()).forEach(consumed::add);

        // Check space in stock ticker
        int occupiedSlots = 0;

        for (BigItemStack entry : consumed.getStacksByCount())
            occupiedSlots += Mth.ceil(entry.count / (float) entry.stack.getMaxStackSize());

        var receivedPayments = ((IStockTickerBlockEntityAccessor) tickerBE).getReceivedPayments();

        for (int i = 0; i < receivedPayments.getSlots(); i++)
            if (receivedPayments.getStackInSlot(i)
                    .isEmpty())
                occupiedSlots--;

        if (occupiedSlots > 0) {
            AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
            CreateLang.translate("stock_keeper.cash_register_full")
                    .style(ChatFormatting.RED)
                    .sendStatus(player);
            return;
        }

        // Transfer payment to stock ticker
        for (boolean simulate : Iterate.trueAndFalse) {
            // Currencies
            List<ItemStack> toTransfer = new ArrayList<>();

            for(var entry : paymentCurrencies.entrySet()) {
                var result = extractValueFromPlayer(player, entry.getKey(), entry.getValue(), simulate);
                if (simulate) {
                    if (result.remaining > 0) {
                        youreTooPoor(level, player);
                        return;

                    } else if (result.inventoryFull) {
                        AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                        CALLang.translate("stock_keeper.player_inventory_full")
                                .style(ChatFormatting.RED)
                                .sendStatus(player);
                        return;
                    }

                } else
                    toTransfer.addAll(entry.getKey().getStacksWithValue(entry.getValue()));
            }

            // Other Payments
            InventorySummary tally = new InventorySummary();
            paymentOther.forEach(tally::add);

            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack item = player.getInventory()
                        .getItem(i);
                if (item.isEmpty())
                    continue;
                int countOf = tally.getCountOf(item);
                if (countOf == 0)
                    continue;
                int toRemove = Math.min(item.getCount(), countOf);
                tally.add(item, -toRemove);

                if (simulate)
                    continue;

                int newStackSize = item.getCount() - toRemove;
                player.getInventory()
                        .setItem(i, newStackSize == 0 ? ItemStack.EMPTY : item.copyWithCount(newStackSize));
                toTransfer.add(item.copyWithCount(toRemove));
            }

            if (simulate && tally.getTotalCount() != 0) {
                AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                CreateLang.translate("stock_keeper.too_broke")
                        .style(ChatFormatting.RED)
                        .sendStatus(player);
                return;
            }

            if (simulate)
                continue;

            toTransfer.forEach(s -> ItemHandlerHelper.insertItemStacked(receivedPayments, s, false));
        }

        // We have a sale.
        if (tickerBE instanceof CashRegisterBlockEntity register)
            register.recordSale(player, list);

        tickerBE.broadcastPackageRequest(LogisticallyLinkedBehaviour.RequestType.PLAYER, order, null, ShoppingListItem.getAddress(mainHandItem));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!order.isEmpty())
            AllSoundEvents.STOCK_TICKER_TRADE.playOnServer(level, tickerBE.getBlockPos());
    }

    private static void youreTooPoor(Level level, Player player) {
        AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
        CreateLang.translate("stock_keeper.too_broke")
                .style(ChatFormatting.RED)
                .sendStatus(player);
    }


    public record ExtractValueResult(boolean inventoryFull, long remaining) {
        public static ExtractValueResult of(boolean inventoryFull, long remaining) {
            return new ExtractValueResult(inventoryFull, remaining);
        }

    }

    /// Extract an amount of currency from the player's inventory.
    public static ExtractValueResult extractValueFromPlayer(Player player, ICurrency currency, long value, boolean simulate) {
        return extractValueFrom(player, player.level(), player.blockPosition(), new PlayerInvWrapper(player.getInventory()), currency, value, simulate);
    }

    public static ExtractValueResult extractValueFromBlock(@Nullable Player player, BlockEntity be, IItemHandlerModifiable itemHandler, ICurrency currency, long value, boolean simulate) {
        return extractValueFrom(player, be.getLevel(), be.getBlockPos(), itemHandler, currency, value, simulate);
    }

    public static ExtractValueResult extractValueFrom(@Nullable Player player, Level level, BlockPos pos, IItemHandlerModifiable itemHandler, ICurrency currency, long value, boolean simulate) {
        if (value <= 0)
            return ExtractValueResult.of(false, 0);

        long remaining = value;
        int emptied_slots = 0;
        InventorySummary to_insert = new InventorySummary();

        // First, build a list of items to insert.
        boolean[] modes;
        if (simulate)
            modes = new boolean[]{false};
        else
            modes = Iterate.trueAndFalse;

        for(boolean exact : modes) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                var result = currency.extractValue(player, stack, remaining, exact);
                if (result.remainingValue() == remaining)
                    continue;

                remaining = result.remainingValue();

                if (simulate) {
                    // If we're simulating, count this slot for later and
                    // save the remaining items for adding up.
                    result.remaining().forEach(to_insert::add);
                    emptied_slots++;

                } else if (result.remaining().isEmpty())
                    // We're not simulating, there's nothing remaining.
                    // Clear this slot.
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);

                else {
                    // We're not simulating. There's remaining items.
                    // Give the player the remaining items while removing
                    // this slot's existing item.
                    var stacks = result.remaining();
                    itemHandler.setStackInSlot(slot, stacks.get(0));

                    if (stacks.size() > 1)
                        for (int j = 1; j < stacks.size(); j++) {
                            var remainder = ItemHandlerHelper.insertItem(itemHandler, stacks.get(j), false);
                            if (!remainder.isEmpty() && !level.isClientSide) {
                                ItemEntity entity = new ItemEntity(level, pos.getX(), pos.getY() + 0.5F, pos.getZ(), remainder);
                                entity.setPickUpDelay(40);
                                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.0, 1.0, 0.0));
                                level.addFreshEntity(entity);
                            }
                        }
                }

                if (remaining <= 0)
                    break;
            }

            if (remaining <= 0)
                break;
        }

        // If we're simulating, see if we have room to insert any remaining values.
        // We're doing this in a very basic way.
        if (simulate) {
            int occupiedSlots = 0;

            for (BigItemStack entry : to_insert.getStacksByCount())
                occupiedSlots += Mth.ceil(entry.count / (float) entry.stack.getMaxStackSize());

            occupiedSlots -= emptied_slots;
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.isEmpty())
                    occupiedSlots--;
            }

            if (occupiedSlots > 0)
                return ExtractValueResult.of(true, remaining);
        }

        return ExtractValueResult.of(false, remaining);
    }


    public static Pair<Map<ICurrency, Long>, List<BigItemStack>> splitCost(Player player, List<BigItemStack> input) {
        Map<ICurrency, Long> currency_cost = new Object2LongArrayMap<>();
        List<BigItemStack> other_cost = new ArrayList<>();

        for(var entry : input) {
            var currency = getForItem(entry.stack.getItem());
            if (currency == null)
                other_cost.add(entry);
            else {
                long value = currency.getValue(player, entry.stack, entry.count);
                currency_cost.put(currency, currency_cost.getOrDefault(currency, 0L) + value);
            }

        }

        return Pair.of(currency_cost, other_cost);
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderShoppingList(@Nullable ShoppingListItem.ShoppingList list) {
        if (list == null || IBlueprintOverlayRendererAccessor.CAL$getActive())
            return;

        var lists = list.bakeEntries(Minecraft.getInstance().level, null);
        var costs = splitCost(Minecraft.getInstance().player, lists.getSecond().getStacks());

        var currency_cost = costs.getFirst();
        var other_cost = costs.getSecond();
        var purchased = lists.getFirst();

        if (currency_cost == null || other_cost == null || purchased == null)
            return;

        IBlueprintOverlayRendererAccessor.callPrepareCustomOverlay();
        IBlueprintOverlayRendererAccessor.CAL$setNoOutput(false);
        IBlueprintOverlayRendererAccessor.CAL$setShopContext(new BlueprintOverlayShopContext(true, 1, 0));

        var player = Minecraft.getInstance().player;

        var ingredients = IBlueprintOverlayRendererAccessor.CAL$getIngredients();
        var results = IBlueprintOverlayRendererAccessor.CAL$getResults();

        for(var entry : currency_cost.entrySet()) {
            var currency = entry.getKey();
            var value = entry.getValue();
            var result = CurrencyUtilities.extractValueFromPlayer(player, currency, value, true);

            boolean can_afford = !result.inventoryFull() && result.remaining() <= 0;

            for(var item : currency.getStacksWithValue(value))
                ingredients.add(Pair.of(item, can_afford));
        }

        for(var entry : other_cost) {
            boolean can_afford = IBlueprintOverlayRendererAccessor.callCanAfford(player, entry);
            ingredients.add(Pair.of(entry.stack.copyWithCount(entry.count), can_afford));
        }

        for(BigItemStack entry : purchased.getStacksByCount())
            results.add(entry.stack.copyWithCount(entry.count));

    }


    public static void createShoppingListTooltip(Player player, ItemStack stack, List<Component> tooltipComponents, TooltipFlag tooltipFlag, @Nullable Couple<InventorySummary> lists) {
        if (lists == null)
            return;

        var costs = splitCost(player, lists.getSecond().getStacks());
        var currency_cost = costs.getFirst();
        var other_cost = costs.getSecond();

        var purchased = lists.getFirst();
        for(var entry : purchased.getStacks()) {
            tooltipComponents.add(addTooltipLine(ChatFormatting.GRAY, entry));
        }

        if (!currency_cost.isEmpty() || ! other_cost.isEmpty()) {
            CreateLang.translate("table_cloth.total_cost").style(ChatFormatting.GOLD).addTo(tooltipComponents);
            for(var entry : currency_cost.entrySet()) {
                var currency = entry.getKey();
                Component cmp;
                try {
                    cmp = currency.formatValue(entry.getValue(), tooltipFlag);
                } catch(Exception ex) {
                    CreateAdditionalLogistics.LOGGER.error("Error running currency formatter for {}", currency.getId(), ex);
                    cmp = null;
                }

                if (cmp != null)
                    tooltipComponents.add(Component.empty().withStyle(ChatFormatting.YELLOW).append(cmp));
                else {
                    for(var item : currency.getStacksWithValue(entry.getValue()))
                        tooltipComponents.add(addTooltipLine(ChatFormatting.YELLOW, item));
                }
            }
            for(var entry : other_cost)
                tooltipComponents.add(addTooltipLine(ChatFormatting.YELLOW, entry));
        }

    }

    private static Component addTooltipLine(ChatFormatting style, ItemStack entry) {
        var result = CreateLang.builder()
                .add(entry.getHoverName())
                .text(" x")
                .text(String.valueOf(entry.getCount()))
                .style(style)
                .component();

        var rarity = entry.getRarity();
        if (rarity == Rarity.EPIC || rarity == Rarity.RARE)
            return result.withStyle(entry.getRarity().getStyleModifier());

        return result;
    }

    private static Component addTooltipLine(ChatFormatting style, BigItemStack entry) {
        return CreateLang.builder()
                .add(entry.stack.getHoverName().plainCopy())
                .text(" x")
                .text(String.valueOf(entry.count))
                .style(style)
                .component();
    }

    public static class CurrencyBackend implements ICurrency.ICurrencyBackend {

        public static CurrencyBackend INSTANCE = new CurrencyBackend();

        @Override
        public void registerFormatter(ResourceLocation id, ICurrency.IValueFormatter formatter) {
            CurrencyUtilities.registerFormatter(id, formatter);
        }

        @Override
        public void registerCurrency(ResourceLocation id, ICurrency currency) {
            CurrencyUtilities.registerCurrency(id, currency);
        }

        @Override
        public ICurrencyBuilder newBuilder(ResourceLocation id) {
            return new SimpleCurrency.CurrencyBuilder(id, this);
        }

        @Override
        public @Nullable ICurrency get(ResourceLocation id) {
            return CurrencyUtilities.get(id);
        }

        @Override
        public @Nullable ICurrency getForItem(Item item) {
            return CurrencyUtilities.getForItem(item);
        }

    }

    public static void init() {
        Currency.setBackend(CurrencyBackend.INSTANCE);
    }

}
