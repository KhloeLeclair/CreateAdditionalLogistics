package dev.khloeleclair.create.additionallogistics.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Currency {

    @Nullable
    private static ICurrency.ICurrencyBackend BACKEND;

    @ApiStatus.Internal
    public static void setBackend(ICurrency.ICurrencyBackend backend) {
        BACKEND = backend;
    }

    public static void registerFormatter(ResourceLocation id, ICurrency.IValueFormatter formatter) {
        Objects.requireNonNull(BACKEND);
        BACKEND.registerFormatter(id, formatter);
    }

    public static void registerCurrency(ResourceLocation id, ICurrency currency) {
        Objects.requireNonNull(BACKEND);
        BACKEND.registerCurrency(id, currency);
    }

    public static ICurrencyBuilder builder(ResourceLocation id) {
        Objects.requireNonNull(BACKEND);
        return BACKEND.newBuilder(id);
    }

    @Nullable
    static ICurrency get(ResourceLocation id) {
        if (BACKEND == null)
            return null;
        return BACKEND.get(id);
    }

    @Nullable
    static ICurrency getForItem(Item item) {
        if (BACKEND == null)
            return null;
        return BACKEND.getForItem(item);
    }

}
