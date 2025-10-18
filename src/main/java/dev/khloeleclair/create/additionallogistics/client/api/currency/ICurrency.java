package dev.khloeleclair.create.additionallogistics.client.api.currency;

import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CurrencyUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface ICurrency {

    record ExtractionResult(List<ItemStack> remaining, int remainingValue) {}

    static void registerFormatter(ResourceLocation id, Function<Integer, Component> formatter) {
        CurrencyUtilities.registerFormatter(id, formatter);
    }

    @Nullable
    static ICurrency get(ResourceLocation id) {
        return CurrencyUtilities.get(id);
    }

    @Nullable
    static ICurrency getForItem(Item item) {
        return CurrencyUtilities.getForItem(item);
    }


    ResourceLocation getId();

    Iterable<Item> getItems();

    int getValue(ItemStack stack, int count);

    int getValue(ItemStack stack);

    List<ItemStack> getStacksWithValue(int count);

    ExtractionResult extractValue(ItemStack stack, int toExtract);

    boolean hasFormatter();

    Component formatValue(int value);

}
