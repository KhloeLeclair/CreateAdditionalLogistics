package dev.khloeleclair.create.additionallogistics.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ICurrency {

    record ExtractionResult(List<ItemStack> remaining, int remainingValue) {}

    ResourceLocation getId();

    Iterable<Item> getItems();

    int getValue(@Nullable Player player, ItemStack stack, int count);

    List<ItemStack> getStacksWithValue(int count);

    ExtractionResult extractValue(@Nullable Player player, ItemStack stack, int toExtract, boolean exact);

    @Nullable
    Component formatValue(int value, TooltipFlag flag);

    interface IAdvancedValueGetter {
        int get(@Nullable Player player, ItemStack stack, int count);
    }

    interface IAdvancedValueExtractor {
        ExtractionResult apply(@Nullable Player player, ItemStack stack, int toExtract, boolean exact);
    }

    interface IValueFormatter {
        @Nullable
        Component get(int value, TooltipFlag tooltipFlag);
    }

    interface ICurrencyBackend {

        void registerFormatter(ResourceLocation id, IValueFormatter formatter);

        void registerCurrency(ResourceLocation id, ICurrency currency);

        ICurrencyBuilder newBuilder(ResourceLocation id);

        @Nullable
        ICurrency get(ResourceLocation id);

        @Nullable
        ICurrency getForItem(Item item);

    }

}
