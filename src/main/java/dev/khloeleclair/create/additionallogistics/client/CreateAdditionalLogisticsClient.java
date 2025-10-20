package dev.khloeleclair.create.additionallogistics.client;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateAdditionalLogistics.MODID, dist = Dist.CLIENT)
public class CreateAdditionalLogisticsClient {

    public CreateAdditionalLogisticsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.

        // We could use the shiny Create mod screen... but it doesn't use i18n keys and has other odd behavior.
        //Supplier<IConfigScreenFactory> configScreen = () -> (mc, previousScreen) -> new BaseConfigScreen(previousScreen, CreateAdditionalLogistics.MODID);
        //container.registerExtensionPoint(IConfigScreenFactory.class, configScreen);

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

    }

}
