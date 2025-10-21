package dev.khloeleclair.create.additionallogistics.api.currency;

import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CurrencyUtilities;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SimpleCurrency;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class CurrencyBuilder {

    private final SimpleCurrency currency;
    @Nullable
    private BiFunction<Integer, TooltipFlag, Component> formatter;

    protected CurrencyBuilder(ResourceLocation id) {
        this.currency = new SimpleCurrency(id);
    }

    public CurrencyBuilder add(Item item, int value) {
        if (item.equals(Items.AIR))
            throw new IllegalArgumentException("item cannot be air");
        if (value < 1)
            throw new IllegalArgumentException("value must be at least 1");

        this.currency.addItem(item, value);
        return this;
    }

    public CurrencyBuilder formatter(BiFunction<Integer, TooltipFlag, Component> formatter) {
        this.formatter = formatter;
        return this;
    }

    public void register() {
        CurrencyUtilities.registerCurrency(currency.getId(), currency);
        if (formatter != null)
            CurrencyUtilities.registerFormatter(currency.getId(), formatter);
    }

}
