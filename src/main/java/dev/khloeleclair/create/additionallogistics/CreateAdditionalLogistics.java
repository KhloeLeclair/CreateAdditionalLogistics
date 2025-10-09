package dev.khloeleclair.create.additionallogistics;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.blockentities.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.data.CustomComponents;
import dev.khloeleclair.create.additionallogistics.common.datagen.DataGen;
import dev.khloeleclair.create.additionallogistics.common.network.CustomPackets;
import dev.khloeleclair.create.additionallogistics.common.registries.*;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
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
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }


    public CreateAdditionalLogistics(IEventBus modEventBus, ModContainer modContainer) {

        // Register things.
        REGISTRATE.get().registerEventListeners(modEventBus);

        CALTags.init();
        CALBlocks.register();
        CALItems.register();
        CALMenuTypes.register();
        CALEntityTypes.register();
        CALBlockEntityTypes.register();
        CALStress.register();
        CALPartialModels.register();

        CALComputerCraftProxy.register();

        CustomComponents.register(modEventBus);
        modEventBus.addListener(CustomPackets::register);
        //modEventBus.addListener(this::populateCreativeTabs);

        modEventBus.addListener(EventPriority.HIGHEST, DataGen::gatherData);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(AbstractLowEntityKineticBlockEntity::onTick);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        Config.register(modContainer);
    }

    private static ItemStack addToTab(BuildCreativeModeTabContentsEvent event, ItemStack item, @Nullable ItemStack after, CreativeModeTab.TabVisibility visibility) {
        if (after != null && !after.isEmpty() && event.getTab().contains(after))
            event.insertAfter(after, item, visibility);
        else
            event.accept(item, visibility);

        return item;
    }

    private void populateCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == AllCreativeModeTabs.BASE_CREATIVE_TAB.get()) {

            addToTab(event, CALBlocks.CASH_REGISTER.asStack(), AllBlocks.STOCK_TICKER.asStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            var editor = addToTab(event, CALBlocks.PACKAGE_EDITOR.asStack(), AllBlocks.REPACKAGER.asStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            addToTab(event, CALBlocks.PACKAGE_ACCELERATOR.asStack(), editor, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        } else if (event.getTab() == AllCreativeModeTabs.PALETTES_CREATIVE_TAB.get()) {

            var after = AllBlocks.SEATS.get(DyeColor.BLACK).asStack();
            for(DyeColor color : DyeColor.values())
                after = addToTab(event, CALBlocks.SHORT_SEATS.get(color).asStack(), after, color == DyeColor.RED ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);

            for(DyeColor color : DyeColor.values())
                after = addToTab(event, CALBlocks.TALL_SEATS.get(color).asStack(), after, color == DyeColor.RED ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }


    @SubscribeEvent
    private void onServerStart(ServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    private void onServerStopped(ServerStoppedEvent event) {
        server = null;
    }

    @SubscribeEvent
    private void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (! Config.Common.protectStockKeeperSeats.get())
            return;

        final var pos = event.getPos();
        final var level = event.getLevel();

        if (level.getBlockState(pos).getBlock() instanceof SeatBlock) {
            var seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
            if (!seats.isEmpty()) {
                var passengers = seats.getFirst().getPassengers();
                if (! passengers.isEmpty()) {
                    var passenger = passengers.getFirst();
                    var ticker = StockTickerInteractionHandler.getStockTickerPosition(passenger);
                    if (ticker != null)
                        event.setCanceled(true);
                }
            }
        }
    }


}
