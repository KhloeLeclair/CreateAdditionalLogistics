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

    record ExtractionResult(List<ItemStack> remaining, long remainingValue) {}

    ResourceLocation getId();

    Iterable<Item> getItems();

    long getValue(@Nullable Player player, ItemStack stack, int count);

    List<ItemStack> getStacksWithValue(long value);

    ExtractionResult extractValue(@Nullable Player player, ItemStack stack, long toExtract, boolean exact);

    @Nullable
    Component formatValue(long value, TooltipFlag flag);

    interface IAdvancedValueGetter {
        long get(@Nullable Player player, ItemStack stack, int count);
    }

    interface IAdvancedValueExtractor {
        ExtractionResult apply(@Nullable Player player, ItemStack stack, long toExtract, boolean exact);
    }

    interface IValueFormatter {
        @Nullable
        Component get(long value, TooltipFlag tooltipFlag);
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
