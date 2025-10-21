package dev.khloeleclair.create.additionallogistics.common.datagen;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataMaps;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

public class CALDataMapProvider extends DataMapProvider {

    public CALDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    public static CALDataMaps.CurrencyData get(String id, int value) {
        return new CALDataMaps.CurrencyData(CreateAdditionalLogistics.asResource(id), value);
    }

    public static CALDataMaps.CurrencyData get(String namespace, String id, int value) {
        return new CALDataMaps.CurrencyData(ResourceLocation.fromNamespaceAndPath(namespace, id), value);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        super.gather(provider);

        addNumismaticsCoins(provider);
    }

    private static final String NUMISMATICS = "numismatics";

    private void addNumismaticsCoins(HolderLookup.Provider provider) {

        var builder = this.builder(CALDataMaps.CURRENCY_DATA)
                .conditions(new ModLoadedCondition(NUMISMATICS));

        var coins = new Pair[]{
                Pair.of("spur", 1),
                Pair.of("bevel", 8),
                Pair.of("sprocket", 16),
                Pair.of("cog", 64),
                Pair.of("crown", 512),
                Pair.of("sun", 4096)
        };

        for(var entry : coins) {
            var key = (String) entry.first();
            var value = (int) entry.second();

            builder.add(ResourceLocation.fromNamespaceAndPath(NUMISMATICS, key), get(NUMISMATICS, "coins", value), false);
        }

        builder.build();
    }
}
