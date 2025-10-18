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
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataMaps;
import dev.khloeleclair.create.additionallogistics.mixin.IStockTickerBlockEntityAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class CurrencyUtilities {

    public static final Map<ResourceLocation, SimpleCurrency> CURRENCIES = new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceLocation, Function<Integer, Component>> FORMATTERS = new Object2ObjectOpenHashMap<>();

    public static final Map<Item, @Nullable SimpleCurrency> AUTOMATIC_CURRENCIES = new Object2ObjectOpenHashMap<>();

    private static boolean isPopulated;

    public static boolean isConversionEnabled() {
        ensurePopulated();
        return true;
        //return ! CURRENCIES.isEmpty();
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

    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        AUTOMATIC_CURRENCIES.clear();
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

        synchronized (AUTOMATIC_CURRENCIES) {
            if (AUTOMATIC_CURRENCIES.containsKey(item))
                return AUTOMATIC_CURRENCIES.get(item);
        }

        var result = calculateCompressionRecipeCurrency(item);
        synchronized (AUTOMATIC_CURRENCIES) {
            if (result == null)
                AUTOMATIC_CURRENCIES.put(item, result);
            else {
                for(var i : result.getItems())
                    AUTOMATIC_CURRENCIES.put(i, result);
            }
        }

        return result;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Nullable
    private static SimpleCurrency calculateCompressionRecipeCurrency(Item item) {
        var server = CreateAdditionalLogistics.getServer();
        Level level;
        if (server != null)
            level = server.overworld();
        else if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.CLIENT)
            level = getClientLevel();
        else
            return null;

        if (level == null)
            return null;

        List<Pair<Item, Integer>> decompression = new ArrayList<>();
        List<Pair<Item, Integer>> compression = new ArrayList<>();

        HashSet<Item> visited = new HashSet<>();
        visited.add(item);

        // Scan down first.
        Item current = item;
        while(true) {
            ItemStack result = getUncompressResult(level, current.getDefaultInstance());
            if (result.isEmpty())
                break;

            int count = result.getCount();
            // We only support 2x2 and 3x3
            if (count != 4 && count != 9)
                break;

            // Loop detection.
            if (!visited.add(result.getItem()))
                break;

            // Detect if there's a reciprocal recipe
            var compressionResult = getCompressionResult(level, result, count == 4 ? 2 : 3);

            Item finalCurrent = current;
            if (compressionResult.stream().noneMatch(x -> x.is(finalCurrent) && x.getCount() == 1))
                break;

            // We got here, so there's a conversion.
            current = result.getItem();

            decompression.add(Pair.of(current, count));
        }

        // Now, scan up.
        List<Item> frontier = new LinkedList<>();
        frontier.add(item);

        while(!frontier.isEmpty()) {
            current = frontier.removeFirst();
            for(int size = 2; size <= 3; size++) {
                for (var result : getCompressionResult(level, current.getDefaultInstance(), size)) {
                    // If it produced anything we've seen before, or more than 1 item, we don't want it.
                    if (result.isEmpty() || result.getCount() != 1 || !visited.add(result.getItem()))
                        continue;

                    // Check for a reciprocal recipe.
                    var uncompressResult = getUncompressResult(level, result);
                    if (uncompressResult.isEmpty() || uncompressResult.getCount() != (size == 2 ? 4 : 9) || !uncompressResult.is(current))
                        continue;

                    // We have a match.
                    frontier.add(result.getItem());
                    compression.add(Pair.of(result.getItem(), size == 2 ? 4 : 9));
                }
            }
        }

        if (decompression.isEmpty() && compression.isEmpty())
            return null;

        // Generate a key based on the key of the lowest item.
        @Nullable ResourceLocation id;
        if (decompression.isEmpty())
            id = item.builtInRegistryHolder().getKey().location();
        else
            id = decompression.getLast().getFirst().builtInRegistryHolder().getKey().location();

        if (id == null)
            return null;

        SimpleCurrency currency = new SimpleCurrency(CreateAdditionalLogistics.asResource("generated/" + id.getNamespace() + "/" + id.getPath()));
        int value = 1;

        // First, decompression items.
        for(int i = decompression.size() - 1; i >= 0; i--) {
            var entry = decompression.get(i);
            currency.addItem(entry.getFirst(), value);
            value *= entry.getSecond();
        }

        // Now, add the base item
        currency.addItem(item, value);

        // Now, the compression items
        for(var entry : compression) {
            value *= entry.getSecond();
            currency.addItem(entry.getFirst(), value);
        }

        return currency;
    }


    private static List<ItemStack> getCompressionResult(Level level, ItemStack input, int size) {

        List<ItemStack> inputGrid;

        if (size == 1)
            inputGrid = List.of(input.copyWithCount(1));
        else if (size == 2)
            inputGrid = List.of(input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1));
        else if (size == 3)
            inputGrid = List.of(input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1), input.copyWithCount(1));
        else
            return List.of();

        var recipes = safeGetRecipesFor(RecipeType.CRAFTING, CraftingInput.of(size, size, inputGrid), level);
        if (recipes.isEmpty())
            return List.of();

        return recipes.stream().map(x -> x.value().getResultItem(level.registryAccess())).toList();
    }

    private static ItemStack getUncompressResult(Level level, ItemStack input) {
        // We're looking for a recipe that puts the input stack in a 1x1 grid.
        var inputGrid = CraftingInput.of(1, 1, List.of(input.copyWithCount(1)));

        var recipes = safeGetRecipesFor(RecipeType.CRAFTING, inputGrid, level);
        if (recipes.size() != 1)
            return ItemStack.EMPTY;

        return recipes.getFirst().value().getResultItem(level.registryAccess());
    }


    public static <I extends CraftingInput, T extends Recipe<I>> List<RecipeHolder<T>> safeGetRecipesFor(RecipeType<T> recipeType, I inventory, Level level) {
        try {
            return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
        } catch (Exception e) {
            CreateAdditionalLogistics.LOGGER.error("Error while getting recipe: ", e);
            return Collections.emptyList();
        }
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
