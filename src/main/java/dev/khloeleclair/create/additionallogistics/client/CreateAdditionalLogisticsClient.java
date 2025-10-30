package dev.khloeleclair.create.additionallogistics.client;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.Config;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

//@Mod(value = CreateAdditionalLogistics.MODID, dist = Dist.CLIENT)
public class CreateAdditionalLogisticsClient {

    public CreateAdditionalLogisticsClient() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);

        var factory = new ConfigScreenHandler.ConfigScreenFactory((mc, previousScreen) -> new BaseConfigScreen(previousScreen, CreateAdditionalLogistics.MODID));
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> factory);
    }

    @SubscribeEvent
    public void loadComplete(final FMLLoadCompleteEvent event) {
        Config.registerClient();
    }

}
