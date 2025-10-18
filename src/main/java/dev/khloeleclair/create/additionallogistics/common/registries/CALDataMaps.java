package dev.khloeleclair.create.additionallogistics.common.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public class CALDataMaps {

    public record CurrencyData(ResourceLocation id, int value) {
        public static final Codec<CurrencyData> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(CurrencyData::id),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("value").forGetter(CurrencyData::value)
        ).apply(i, CurrencyData::new));
    }

    public static final DataMapType<Item, CurrencyData> CURRENCY_DATA = DataMapType.builder(
            CreateAdditionalLogistics.asResource("currency"),
            Registries.ITEM,
            CurrencyData.CODEC
    ).synced(
            CurrencyData.CODEC,
            false
    ).build();

    public static void register(IEventBus modBus) {
        modBus.addListener(CALDataMaps::registerTypes);
    }

    private static void registerTypes(RegisterDataMapTypesEvent event) {
        event.register(CURRENCY_DATA);
    }

}
