package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import dev.khloeleclair.create.additionallogistics.api.currency.ICurrency;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimpleCurrency implements ICurrency {

    private final ResourceLocation id;
    private final Map<Item, Integer> items;

    private final List<Item> items_by_value;

    public SimpleCurrency(ResourceLocation id) {
        this.id = id;

        // Currencies will likely have very few items, so use an array map
        // for memory efficiency.
        items = new Object2IntArrayMap<>();
        items_by_value = new ArrayList<>();
    }

    public void addItem(Item item, int value) {
        Objects.requireNonNull(item);
        if (value <= 0)
            throw new IllegalArgumentException("value must be greater than 0");

        items.put(item, value);
        items_by_value.add(item);
        items_by_value.sort((a,b) -> -items.get(a).compareTo(items.get(b)));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Iterable<Item> getItems() {
        return items.keySet();
    }

    @Override
    public int getValue(ItemStack stack) {
        final var item = stack.getItem();
        final int value = items.getOrDefault(item, 0);

        return value * stack.getCount();
    }

    @Override
    public int getValue(ItemStack stack, int count) {
        final var item = stack.getItem();
        final int value = items.getOrDefault(item, 0);

        return value * count;
    }

    public List<ItemStack> getStacksWithValue(int value) {
        int remaining = value;
        List<ItemStack> result = new ArrayList<>();

        for(var item : items_by_value) {
            final var item_value = items.get(item);
            if (item_value > remaining)
                continue;

            int item_amount = Math.floorDiv(remaining, item_value);
            remaining -= (item_amount * item_value);

            int max_stack = item.getDefaultMaxStackSize();
            while (item_amount > 0) {
                int count = Math.min(max_stack, item_amount);
                item_amount -= count;
                result.add(new ItemStack(item, count));
            }
        }

        return result;
    }

    @Override
    public ExtractionResult extractValue(ItemStack stack, int toExtract) {
        int value = getValue(stack);

        // Do nothing if there's no input stack or no value.
        if (toExtract <= 0 || value <= 0 || stack.isEmpty())
            return new ExtractionResult(List.of(stack), toExtract);

        // If we're extracting the whole stack, then we can return an empty replacement.
        if (toExtract >= value)
            return new ExtractionResult(List.of(), toExtract - value);

        // We aren't doing that. Can the remaining value be expressed as a single
        // stack of this item?
        int remaining = value - toExtract;
        final int item_value = items.get(stack.getItem());

        int amount = remaining / item_value;
        int remainder = remaining % item_value;

        if (remainder == 0 && amount <= stack.getMaxStackSize())
            return new ExtractionResult(List.of(stack.copyWithCount(amount)), 0);

        // We need multiple stacks, so calculate the optimal stacks
        return new ExtractionResult(getStacksWithValue(value - toExtract), 0);
    }

    @Override
    public boolean hasFormatter() {
        return CurrencyUtilities.getFormatter(id) != null;
    }

    @Override
    public Component formatValue(int value, TooltipFlag flag) {
        var formatter = CurrencyUtilities.getFormatter(id);
        if (formatter != null)
            return formatter.apply(value, flag);

        return CALLang.number(value).component();
    }

}
