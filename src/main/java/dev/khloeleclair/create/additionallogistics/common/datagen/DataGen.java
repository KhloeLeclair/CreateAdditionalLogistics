package dev.khloeleclair.create.additionallogistics.common.datagen;

import com.google.gson.JsonElement;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DataGen {

    public static void gatherData(GatherDataEvent event) {
        addLanguageRegistrateData();

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        CreateAdditionalLogistics.REGISTRATE.get().addDataGenerator(ProviderType.BLOCK_TAGS, provider -> {
            provider.addTag(CALTags.CALBlockTags.BASIC_SHAFTS.tag).add(AllBlocks.SHAFT.getKey());
        });

        CreateAdditionalLogistics.REGISTRATE.get().addDataGenerator(ProviderType.ITEM_TAGS, provider -> {
            provider.addTag(CALTags.CALItemTags.BASIC_SHAFTS.tag).add(AllBlocks.SHAFT.asItem().builtInRegistryHolder().getKey());
        });

        if (event.includeServer())
            CALRecipeProvider.registerAllProcessing(generator, output, lookupProvider);
    }

    private static void addLanguageRegistrateData() {
        CreateAdditionalLogistics.REGISTRATE.get().addDataGenerator(ProviderType.LANG, provider -> {
            provideDefaultLang("config", provider::add);
            provideDefaultLang("interface", provider::add);
            provideDefaultLang("tooltip", provider::add);
        });
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + CreateAdditionalLogistics.MODID + "/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null)
            throw new IllegalStateException(String.format("Could not find default language file: %s", path));

        for (var entry : jsonElement.getAsJsonObject().entrySet())
            consumer.accept(entry.getKey(), entry.getValue().getAsString());
    }

}
