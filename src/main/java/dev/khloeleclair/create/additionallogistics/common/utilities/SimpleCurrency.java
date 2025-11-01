package dev.khloeleclair.create.additionallogistics.common.utilities;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import dev.khloeleclair.create.additionallogistics.api.ICurrency;
import dev.khloeleclair.create.additionallogistics.api.ICurrencyBuilder;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimpleCurrency implements ICurrency {

    private final ResourceLocation id;
    private final Map<Item, Long> items;
    private final Map<Item, Pair<IAdvancedValueGetter, IAdvancedValueExtractor>> advancedItems;

    private final List<Item> items_by_value;

    public SimpleCurrency(ResourceLocation id) {
        this.id = id;

        // Currencies will likely have very few items, so we just use array maps.
        items = new Object2LongArrayMap<>();
        advancedItems = new Object2ObjectArrayMap<>();
        items_by_value = new ArrayList<>();
    }

    public void addItem(Item item, long value) {
        Objects.requireNonNull(item);
        if (value <= 0)
            throw new IllegalArgumentException("value must be greater than 0");

        if (advancedItems.containsKey(item))
            throw new IllegalArgumentException("item " + item.getDescriptionId() + " has advanced definition");

        items.put(item, value);
        items_by_value.add(item);
        items_by_value.sort((a,b) -> -items.get(a).compareTo(items.get(b)));
    }

    public void addItem(Item item, IAdvancedValueGetter getValue, IAdvancedValueExtractor extractValue) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(getValue);
        Objects.requireNonNull(extractValue);

        if (items.containsKey(item)) {
            items.remove(item);
            items_by_value.remove(item);
        }

        advancedItems.put(item, Pair.of(getValue, extractValue));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Iterable<Item> getItems() {
        return Iterables.concat(items.keySet(), advancedItems.keySet());
    }

    public void validate() {
        if (items.isEmpty())
            throw new IllegalStateException("currency must have at least one non-advanced item");
    }

    @Override
    public long getValue(@Nullable Player player, ItemStack stack, int count) {
        final var item = stack.getItem();
        final var advanced = advancedItems.get(item);
        if (advanced != null)
            return advanced.first().get(player, stack, count);

        final long value = items.getOrDefault(item, 0L);

        return value * count;
    }

    public List<ItemStack> getStacksWithValue(long value) {
        long remaining = value;
        List<ItemStack> result = new ArrayList<>();

        for(var item : items_by_value) {
            final long item_value = items.get(item);
            if (item_value > remaining)
                continue;

            long item_amount = remaining / item_value;
            remaining -= (item_amount * item_value);

            int max_stack = item.getMaxStackSize();
            while (item_amount > 0) {
                int count = Math.min(max_stack, item_amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) item_amount);
                item_amount -= count;
                result.add(new ItemStack(item, count));
            }
        }

        return result;
    }

    @Override
    public ExtractionResult extractValue(@Nullable Player player, ItemStack stack, long toExtract, boolean exact) {
        final var item = stack.getItem();
        final var adv = advancedItems.get(item);
        if (adv != null)
            return adv.second().apply(player, stack, toExtract, exact);

        final int count = stack.getCount();
        long value = getValue(player, stack, count);

        // Do nothing if there's no input stack or no value.
        if (toExtract <= 0 || value <= 0 || stack.isEmpty())
            return new ExtractionResult(List.of(stack), toExtract);

        // If we're extracting the whole stack, then we can return an empty replacement.
        if (toExtract >= value)
            return new ExtractionResult(List.of(), toExtract - value);

        // We aren't doing that. What we do now depends on if this is in exact mode or not.
        final long item_value = items.get(stack.getItem());

        if (exact) {
            // We're in exact mode, so we can only deal with this specific currency item
            // and don't want to make change.
            long desired = toExtract / item_value;
            int amount = Math.min(count, Ints.saturatedCast(desired));
            long remaining = toExtract - (amount * item_value);

            if (amount == count)
                return new ExtractionResult(List.of(), remaining);
            else if (amount == 0)
                return new ExtractionResult(List.of(stack), remaining);
            else
                return new ExtractionResult(List.of(stack.copyWithCount(count - amount)), remaining);
        }

        // We aren't in exact mode, so instead we extract as much value as we can
        // and return the leftovers as change.
        long remaining = value - toExtract;

        return new ExtractionResult(getStacksWithValue(remaining), 0);
    }

    @Override
    @Nullable
    public Component formatValue(long value, TooltipFlag flag) {
        var formatter = CurrencyUtilities.getFormatter(id);
        if (formatter != null)
            return formatter.get(value, flag);

        return null;
    }


    public static class CurrencyBuilder implements ICurrencyBuilder {
        private final SimpleCurrency currency;
        private final ICurrencyBackend backend;
        @Nullable
        private IValueFormatter formatter;

        public CurrencyBuilder(ResourceLocation id, ICurrencyBackend backend) {
            this.backend = backend;
            this.currency = new SimpleCurrency(id);
        }

        public CurrencyBuilder add(Item item, int value) {
            return this.add(item, (long) value);
        }

        public CurrencyBuilder add(Item item, long value) {
            if (item.equals(Items.AIR))
                throw new IllegalArgumentException("item cannot be air");
            if (value < 1)
                throw new IllegalArgumentException("value must be at least 1");

            this.currency.addItem(item, value);
            return this;
        }

        public ICurrencyBuilder add(Item item, ICurrency.IAdvancedValueGetter getValue, ICurrency.IAdvancedValueExtractor extractValue) {
            Objects.requireNonNull(item);
            Objects.requireNonNull(getValue);
            Objects.requireNonNull(extractValue);

            if (item.equals(Items.AIR))
                throw new IllegalArgumentException("item cannot be air");

            this.currency.addItem(item, getValue, extractValue);
            return this;
        }

        public CurrencyBuilder formatter(IValueFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public ICurrency build() {
            currency.validate();
            backend.registerCurrency(currency.getId(), currency);
            if (formatter != null)
                backend.registerFormatter(currency.getId(), formatter);
            return currency;
        }
    }

}
