package dev.khloeleclair.create.additionallogistics.api;

import net.minecraft.world.item.Item;

public interface ICurrencyBuilder {

    /// Add a simple item to this currency.
    ICurrencyBuilder add(Item item, int value);

    /// Add a complex item to this currency.
    ICurrencyBuilder add(Item item, ICurrency.IAdvancedValueGetter getValue, ICurrency.IAdvancedValueExtractor extractValue);

    /// Add a custom formatter for this currency.
    ICurrencyBuilder formatter(ICurrency.IValueFormatter formatter);

    /// Build this currency and register it.
    ICurrency build();

}
