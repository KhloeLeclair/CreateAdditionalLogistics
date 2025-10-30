package dev.khloeleclair.create.additionallogistics;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.datagen.DataGen;
import dev.khloeleclair.create.additionallogistics.common.registries.*;
import dev.khloeleclair.create.additionallogistics.common.utilities.CurrencyUtilities;
import dev.khloeleclair.create.additionallogistics.common.utilities.RecipeHelper;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateAdditionalLogistics.MODID)
public class CreateAdditionalLogistics {

    public static final String MODID = "createadditionallogistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private static MinecraftServer server;

    // Registrate
    public static final NonNullSupplier<CreateRegistrate> REGISTRATE = NonNullSupplier.lazy(() -> CreateRegistrate.create(MODID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            )
    );

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }


    public CreateAdditionalLogistics() {
        this(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public CreateAdditionalLogistics(IEventBus modEventBus) {

        // Register things.
        REGISTRATE.get().registerEventListeners(modEventBus);

        CurrencyUtilities.init();

        CALTags.init();
        CALBlocks.register();
        CALItems.register();
        CALMenuTypes.register();
        CALEntityTypes.register();
        CALBlockEntityTypes.register();
        CALStress.register();
        CALPartialModels.register();

        CALComputerCraftProxy.register();

        CALPackets.register();

        modEventBus.addListener(EventPriority.HIGHEST, DataGen::gatherData);

        MinecraftForge.EVENT_BUS.register(this);
        //MinecraftForge.EVENT_BUS.addListener(CurrencyUtilities::onDataMapUpdated);
        MinecraftForge.EVENT_BUS.addListener(RecipeHelper::onRecipesUpdated);
        MinecraftForge.EVENT_BUS.addListener(AbstractLowEntityKineticBlockEntity::onTick);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        Config.register();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        server = null;
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (! Config.Common.protectStockKeeperSeats.get())
            return;

        final var pos = event.getPos();
        final var level = event.getLevel();

        if (level.getBlockState(pos).getBlock() instanceof SeatBlock) {
            var seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
            if (!seats.isEmpty()) {
                var passengers = seats.get(0).getPassengers();
                if (! passengers.isEmpty()) {
                    var passenger = passengers.get(0);
                    var ticker = StockTickerInteractionHandler.getStockTickerPosition(passenger);
                    if (ticker != null)
                        event.setCanceled(true);
                }
            }
        }
    }


}
