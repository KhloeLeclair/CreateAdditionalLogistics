package dev.khloeleclair.create.additionallogistics.common;

import com.google.gson.JsonElement;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.function.BiConsumer;

public class DataGen {

    public static void gatherData(GatherDataEvent event) {
        addLanguageRegistrateData();
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
