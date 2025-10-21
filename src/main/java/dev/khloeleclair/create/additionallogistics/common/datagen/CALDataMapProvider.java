package dev.khloeleclair.create.additionallogistics.common.datagen;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataMaps;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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

        var builder = this.builder(CALDataMaps.CURRENCY_DATA);

        var numismatics = new Pair[]{
                Pair.of("spur", 1),
                Pair.of("bevel", 8),
                Pair.of("sprocket", 16),
                Pair.of("cog", 64),
                Pair.of("crown", 512),
                Pair.of("sun", 4096)
        };

        for(var entry : numismatics) {
            var key = (String) entry.first();
            var value = (int) entry.second();

            builder.add(ResourceLocation.fromNamespaceAndPath("numismatics", key), get("numismatics", "coins", value), false);
        }

                /*.add(CALTags.CALItemTags.NUGGETS_DIAMOND.tag, get("diamond", 1), false)
                .add(Tags.Items.GEMS_DIAMOND, get("diamond", 9), false)
                .add(Tags.Items.STORAGE_BLOCKS_DIAMOND, get("diamond", 81), false)

                .add(CALTags.CALItemTags.NUGGETS_EMERALD.tag, get("emerald", 1), false)
                .add(Tags.Items.GEMS_EMERALD, get("emerald", 9), false)
                .add(Tags.Items.STORAGE_BLOCKS_EMERALD, get("emerald", 81), false)*/

        builder.build();

    }
}
