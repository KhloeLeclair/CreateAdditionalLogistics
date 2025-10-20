package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.api.currency.ICurrency;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataMaps;
import dev.khloeleclair.create.additionallogistics.common.utilities.RecipeHelper;
import dev.khloeleclair.create.additionallogistics.mixin.IStockTickerBlockEntityAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CurrencyUtilities {

    public static final Map<ResourceLocation, SimpleCurrency> CURRENCIES = new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceLocation, Function<Integer, Component>> FORMATTERS = new Object2ObjectOpenHashMap<>();

    private static boolean isPopulated;

    public static boolean isConversionEnabled(boolean is_cash_register) {
        if (!is_cash_register && !Config.Server.stockTickersConvertToo.get())
            return false;
        if (Config.Server.currencyConversion.get())
            return true;
        ensurePopulated();
        return ! CURRENCIES.isEmpty();
    }

    public static boolean isConversionEnabled() {
        return isConversionEnabled(false);
    }

    @Nullable
    public static Function<Integer, Component> getFormatter(ResourceLocation id) {
        synchronized (FORMATTERS) {
            return FORMATTERS.get(id);
        }
    }

    public static void registerFormatter(ResourceLocation id, Function<Integer, Component> formatter) {
        synchronized (FORMATTERS) {
            FORMATTERS.put(id, formatter);
        }
    }

    public static void onDataMapUpdated(DataMapsUpdatedEvent event) {
        if (event.getRegistryKey() != Registries.ITEM || !isPopulated)
            return;

        CURRENCIES.clear();
        isPopulated = false;
    }

    private static void ensurePopulated() {
        if (isPopulated)
            return;

        isPopulated = true;

        var entries = BuiltInRegistries.ITEM.getDataMap(CALDataMaps.CURRENCY_DATA);
        if (entries == null || entries.isEmpty())
            return;

        synchronized (CURRENCIES) {
            int i = 0;

            for (var entry : entries.entrySet()) {
                var item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
                if (item == null)
                    continue;

                var data = entry.getValue();
                var id = data.id();
                int value = data.value();
                if (id == null) {
                    CreateAdditionalLogistics.LOGGER.warn("Ignoring currency entry for {} with null id", entry.getKey());
                    continue;
                }
                if (value < 1) {
                    CreateAdditionalLogistics.LOGGER.warn("Ignoring currency {} for {} with invalid valud", id, entry.getKey());
                    continue;
                }

                var currency = CURRENCIES.computeIfAbsent(id, SimpleCurrency::new);
                currency.addItem(item, value);
                i++;
            }

            CreateAdditionalLogistics.LOGGER.debug("Added {} items to {} currencies.", i, CURRENCIES.size());
        }

        FORMATTERS.put(ResourceLocation.fromNamespaceAndPath("numismatics", "spur"), val -> {
            int cogs = val / 64;
            int spurs = val % 64;

            return CALLang.number(cogs).text(" cogs, ").text(String.valueOf(spurs)).text("¤").component();
        });

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
        var data = item.builtInRegistryHolder().getData(CALDataMaps.CURRENCY_DATA);
        if (data != null && data.id() != null) {
            synchronized (CURRENCIES) {
                return CURRENCIES.get(data.id());
            }
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
        var payment = splitCost(bakeEntries.getSecond().getStacksByCount());

        Map<ICurrency, Integer> paymentCurrencies = payment.getFirst();
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
        consumed.addAllBigItemStacks(paymentOther);

        for(var entry : paymentCurrencies.entrySet())
            consumed.addAllItemStacks(entry.getKey().getStacksWithValue(entry.getValue()));

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
            tally.addAllBigItemStacks(paymentOther);

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


    public record ExtractValueResult(boolean inventoryFull, int remaining) {
        public static ExtractValueResult of(boolean inventoryFull, int remaining) {
            return new ExtractValueResult(inventoryFull, remaining);
        }

    }

    /// Extract an amount of currency from the player's inventory.
    public static ExtractValueResult extractValueFromPlayer(Player player, ICurrency currency, int value, boolean simulate) {
        if (value <= 0)
            return ExtractValueResult.of(false, 0);

        int remaining = value;
        int emptied_slots = 0;
        InventorySummary to_insert = new InventorySummary();

        // TODO: Refactor this method to always draw the smallest valid currency first,
        // instead of just the first one we find.

        // First, build a list of items to insert.
        for(int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            var result = currency.extractValue(stack, remaining);
            if (result.remainingValue() == remaining)
                continue;

            remaining = result.remainingValue();

            if (simulate) {
                // If we're simulating, count this slot for later and
                // save the remaining items for adding up.
                to_insert.addAllItemStacks(result.remaining());
                emptied_slots++;

            } else if (result.remaining().isEmpty())
                // We're not simulating, there's nothing remaining.
                // Clear this slot.
                player.getInventory().setItem(i, ItemStack.EMPTY);

            else {
                // We're not simulating. There's remaining items.
                // Give the player the remaining items while removing
                // this slot's existing item.
                var stacks = result.remaining();
                player.getInventory().setItem(i, stacks.getFirst());

                if (stacks.size() > 1)
                    for(int j = 1; j < stacks.size(); j++)
                        ItemHandlerHelper.giveItemToPlayer(player, stacks.get(j));
            }
        }

        // If we're simulating, see if we have room to insert any remaining values.
        // We're doing this in a very basic way.
        if (simulate) {
            int occupiedSlots = 0;

            for (BigItemStack entry : to_insert.getStacksByCount())
                occupiedSlots += Mth.ceil(entry.count / (float) entry.stack.getMaxStackSize());

            occupiedSlots -= emptied_slots;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty())
                    occupiedSlots--;
            }

            if (occupiedSlots > 0)
                return ExtractValueResult.of(true, remaining);
        }

        return ExtractValueResult.of(false, remaining);
    }


    public static Pair<Map<ICurrency, Integer>, List<BigItemStack>> splitCost(List<BigItemStack> input) {
        Map<ICurrency, Integer> currency_cost = new Object2IntArrayMap<>();
        List<BigItemStack> other_cost = new ArrayList<>();

        for(var entry : input) {
            var currency = getForItem(entry.stack.getItem());
            if (currency == null)
                other_cost.add(entry);
            else {
                int value = currency.getValue(entry.stack, entry.count);
                currency_cost.put(currency, currency_cost.getOrDefault(currency, 0) + value);
            }

        }

        return Pair.of(currency_cost, other_cost);
    }


    public static void createShoppingListTooltip(ItemStack stack, List<Component> tooltipComponents, @Nullable Couple<InventorySummary> lists) {
        if (lists == null)
            return;

        var costs = splitCost(lists.getSecond().getStacks());
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
                if (currency.hasFormatter())
                    tooltipComponents.add(Component.empty().withStyle(ChatFormatting.YELLOW).append(currency.formatValue(entry.getValue())));
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
        return CreateLang.builder()
                .add(entry.getHoverName().plainCopy())
                .text(" x")
                .text(String.valueOf(entry.getCount()))
                .style(style)
                .component();
    }

    private static Component addTooltipLine(ChatFormatting style, BigItemStack entry) {
        return CreateLang.builder()
                .add(entry.stack.getHoverName().plainCopy())
                .text(" x")
                .text(String.valueOf(entry.count))
                .style(style)
                .component();
    }


}
