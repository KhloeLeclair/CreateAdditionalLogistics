package dev.khloeleclair.create.additionallogistics.common.datagen;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataMaps;
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

        /*this.builder(CALDataMaps.CURRENCY_DATA)
                .add(CALTags.CALItemTags.NUGGETS_DIAMOND.tag, get("diamond", 1), false)
                .add(Tags.Items.GEMS_DIAMOND, get("diamond", 9), false)
                .add(Tags.Items.STORAGE_BLOCKS_DIAMOND, get("diamond", 81), false)

                .add(CALTags.CALItemTags.NUGGETS_EMERALD.tag, get("emerald", 1), false)
                .add(Tags.Items.GEMS_EMERALD, get("emerald", 9), false)
                .add(Tags.Items.STORAGE_BLOCKS_EMERALD, get("emerald", 81), false)

                .build();*/

    }
}
