package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.*;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.logistics.packager.PackagerGenerator;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.blocks.*;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.TagGen.*;

public class CALBlocks {

    private static final CreateRegistrate REGISTRATE = CreateAdditionalLogistics.REGISTRATE.get();

    static {
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey());
    }

    // Lazy Shafts
    private static NonNullBiConsumer<DataGenContext<Block, LazyShaftBlock>, RegistrateBlockstateProvider> lazyShaftBlockState() {
        return (c, p) -> {
            String path = "block/flexible_shaft";
            String lazyPath = "block/" + c.getName();

            ModelFile model = p.models()
                    .withExistingParent(c.getName(), p.modLoc(path + "/base"))
                    .texture("frame", p.modLoc(lazyPath + "/frame"))
                    .texture("particle", p.modLoc(lazyPath + "/core"))
                    .texture("core", p.modLoc(lazyPath + "/core"));

            MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
            builder
                    .part()
                    .modelFile(model)
                    .addModel()
                    .end();

            for(Direction dir : Iterate.directions) {
                BooleanProperty prop = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? LazyShaftBlock.POSITIVE
                        : LazyShaftBlock.NEGATIVE;

                ModelFile m = p.models()
                                .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc(path + "/block_" + dir.getSerializedName()))
                                .texture("frame", p.modLoc(lazyPath + "/frame"));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(LazyShaftBlock.AXIS, dir.getAxis())
                        .condition(prop, true)
                        .end();
            }
        };
    }

    public static final BlockEntry<LazyShaftBlock> LAZY_SHAFT =
            REGISTRATE.block("lazy_shaft", LazyShaftBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.PODZOL))
                    .transform(axeOrPickaxe())
                    .blockstate(lazyShaftBlockState())
                    .tag(CALTags.CALBlockTags.BASIC_SHAFTS.tag)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .recipe((c, p) -> {
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 4)
                                .pattern("SS")
                                .pattern("SS")
                                .define('S', AllBlocks.SHAFT)
                                .unlockedBy("has_shaft", RegistrateRecipeProvider.has(AllBlocks.SHAFT))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName()));
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllBlocks.SHAFT, 4)
                                .requires(c.get())
                                .unlockedBy("has_lazy_shaft", RegistrateRecipeProvider.has(c.get()))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/shaft_from_lazy"));
                    })
                    .item()
                    .tag(CALTags.CALItemTags.BASIC_SHAFTS.tag)
                    .transform(ModelGen.customItemModel())
                    .register();

    // Flexible Shafts
    private static NonNullBiConsumer<DataGenContext<Block, FlexibleShaftBlock>, RegistrateBlockstateProvider> pipeBlockState(@Nullable String name, @Nullable ResourceLocation texture) {
        return (c, p) -> {
            String path = "block/" + (name == null ? c.getName() : name);

            ModelFile model;
            if (texture == null)
                model = p.models().getExistingFile(p.modLoc(path + "/base"));
            else
                model = p.models()
                        .withExistingParent(c.getName(), p.modLoc(path + "/base"))
                        .texture("particle", texture)
                        .texture("core", texture);

            MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
            builder
                .part()
                .modelFile(model)
                .addModel()
                .end();

            for(Direction dir : Iterate.directions) {
                int index = dir.ordinal();
                BooleanProperty property = FlexibleShaftBlock.SIDES[index];

                ModelFile m;
                //if (texture == null)
                    m = p.models().getExistingFile(p.modLoc(path + "/block_" + dir.getSerializedName()));
                /*else
                    m = p.models()
                                .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc(path + "/block_" + dir.getSerializedName()))
                                .texture("particle", texture)
                                .texture("core", texture);*/

                builder
                    .part()
                    .modelFile(m)
                    .addModel()
                    .condition(property, true)
                    .end();
            }
        };
    }

    public static final BlockEntry<FlexibleShaftBlock> FLEXIBLE_SHAFT =
            REGISTRATE.block("flexible_shaft", FlexibleShaftBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(MapColor.PODZOL))
                    .transform(axeOrPickaxe())
                    .lang("Flexible Lazy Shaft")
                    .blockstate(pipeBlockState(null, null))
                    .tag(CALTags.CALBlockTags.FLEXIBLE_SHAFTS.tag)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .recipe((c, p) -> {
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 4)
                                .pattern(" S ")
                                .pattern("CBC")
                                .pattern(" S ")
                                .define('S', CALTags.CALItemTags.BASIC_SHAFTS.tag)
                                .define('C', AllBlocks.COGWHEEL)
                                .define('B', AllBlocks.BRASS_CASING)
                                .unlockedBy("has_brass_casing", RegistrateRecipeProvider.has(AllBlocks.BRASS_CASING))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName()));

                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
                                .requires(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag)
                                .unlockedBy("has_flexible_shaft", RegistrateRecipeProvider.has(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName() + "_remove_dye"));
                    })
                    .item()
                    .tag(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag)
                    .transform(ModelGen.customItemModel())
                    .register();


    public static final DyedBlockList<FlexibleShaftBlock> DYED_FLEXIBLE_SHAFTS = new DyedBlockList<>(color -> {
        String colorName = color.getSerializedName();
        return REGISTRATE.block(colorName + "_flexible_shaft", p -> new FlexibleShaftBlock(p, color))
                .initialProperties(SharedProperties::stone)
                .properties(p -> p.mapColor(color))
                .transform(axeOrPickaxe())
                .lang(RegistrateLangProvider.toEnglishName(colorName + "_flexible_lazy_shaft"))
                .blockstate(pipeBlockState("flexible_shaft", ResourceLocation.withDefaultNamespace("block/" + color.getSerializedName() + "_concrete")))
                .tag(CALTags.CALBlockTags.FLEXIBLE_SHAFTS.tag)
                .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                .recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
                        .requires(color.getTag())
                        .requires(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag)
                        .unlockedBy("has_flexible_shaft", RegistrateRecipeProvider.has(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag))
                        .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName() + "_from_other_flexible_shaft")))
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.createadditionallogistics.flexible_shaft"))
                .item()
                .tag(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag)
                .tab(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey(), (c,p) -> p.accept(c.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY))
                .build()
                .register();
    });

    // Cash Register
    public static final BlockEntry<CashRegisterBlock> CASH_REGISTER =
            REGISTRATE.block("cash_register", CashRegisterBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.sound(SoundType.METAL))
                    .transform(TagGen.axeOrPickaxe())
                    .blockstate((c,p) -> {
                        p.horizontalBlock(c.get(), state -> state.getValue(CashRegisterBlock.OPEN)
                                ? p.models().getExistingFile(p.modLoc("block/cash_register/block_open"))
                                : p.models().getExistingFile(p.modLoc("block/cash_register/block"))
                        , 0);
                    })
                    .tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                    .recipe((c,p) -> {
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, c.get())
                                .requires(c.get())
                                .unlockedBy("has_cash_register", RegistrateRecipeProvider.has(c.get()))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/logistics/" + c.getName() + "_clear_data"));

                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                                .pattern(" G ")
                                .pattern("BLO")
                                .pattern(" C ")
                                .define('G', Tags.Items.GLASS_BLOCKS)
                                .define('B', AllItems.BRASS_SHEET.get())
                                .define('O', Items.BOOK)
                                .define('C', Tags.Items.CHESTS)
                                .define('L', AllBlocks.STOCK_LINK)
                                .unlockedBy("has_link", RegistrateRecipeProvider.has(AllBlocks.STOCK_LINK))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/logistics/" + c.getName()));
                    })
                    .item(LogisticallyLinkedBlockItem::new)
                    .model((c,p) -> p.withExistingParent(c.getName(), p.modLoc("block/cash_register/item")))
                    .build()
                    .register();

    // Package Accelerator
    public static final BlockEntry<PackageAcceleratorBlock> PACKAGE_ACCELERATOR =
            REGISTRATE.block("package_accelerator", PackageAcceleratorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.mapColor(DyeColor.GRAY))
                    .blockstate((c,p) -> {
                        p.directionalBlock(c.get(), p.models().getExistingFile(p.modLoc("block/package_accelerator/block")));
                    })
                    .transform(axeOrPickaxe())
                    .tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                    .recipe((c,p) -> {
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                                .pattern("GMD")
                                .pattern(" B ")
                                .pattern(" C ")
                                .define('M', AllItems.PRECISION_MECHANISM)
                                .define('G', AllItems.SUPER_GLUE)
                                .define('B', AllBlocks.BRASS_CASING)
                                .define('D', AllBlocks.CARDBOARD_BLOCK)
                                .define('C', AllBlocks.COGWHEEL)
                                .unlockedBy("has_packager", RegistrateRecipeProvider.has(AllBlocks.PACKAGER))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/logistics/" + c.getName()));
                    })
                    .item()
                    .model((c,p) -> p.withExistingParent(c.getName(), p.modLoc("block/package_accelerator/item")))
                    .build()
                    .register();

    // Package Editor
    public static final BlockEntry<PackageEditorBlock> PACKAGE_EDITOR =
            REGISTRATE.block("package_editor", PackageEditorBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p
                            .noOcclusion()
                            .isRedstoneConductor((a,b,c) -> false)
                            .mapColor(MapColor.TERRACOTTA_CYAN)
                            .sound(SoundType.NETHERITE_BLOCK)
                    )
                    .transform(pickaxeOnly())
                    .blockstate(new PackagerGenerator()::generate)
                    .tag(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                    .recipe((c, p) -> {
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get())
                                .pattern(" I ")
                                .pattern("ICI")
                                .pattern("RIR")
                                .define('I', Items.IRON_INGOT)
                                .define('C', AllBlocks.CLIPBOARD)
                                .define('R', Items.REDSTONE)
                                .unlockedBy("has_packager", RegistrateRecipeProvider.has(AllBlocks.PACKAGER))
                                .save(p, CreateAdditionalLogistics.asResource("crafting/logistics/" + c.getName()));
                    })
                    .item()
                    .model((c,p) -> p.withExistingParent(c.getName(), p.modLoc("block/package_editor/item")))
                    .build()
                    .register();

    static {
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey());
    }

    // Short Seats
    public static final DyedBlockList<ShortSeatBlock> SHORT_SEATS = new DyedBlockList<>(color -> {
        String colorName = color.getSerializedName();
        SeatMovementBehaviour movementBehaviour = new SeatMovementBehaviour();
        SeatInteractionBehaviour interactionBehaviour = new SeatInteractionBehaviour();
        return REGISTRATE.block(colorName + "_short_seat", p -> new ShortSeatBlock(p, color))
                .initialProperties(SharedProperties::wooden)
                .properties(p -> p.mapColor(color))
                .transform(axeOnly())
                .onRegister(movementBehaviour(movementBehaviour))
                .onRegister(interactionBehaviour(interactionBehaviour))
                .transform(displaySource(AllDisplaySources.ENTITY_NAME))
                .blockstate((c, p) -> {
                    p.simpleBlock(c.get(), p.models().getExistingFile(p.modLoc("block/short_seat/" + colorName)));
                })
                /*.blockstate((c, p) -> {
                    p.simpleBlock(c.get(), p.models()
                            .withExistingParent(colorName + "_tall_seat", p.modLoc("block/short_seat/base"))
                            .texture("1", Create.asResource("block/seat/top_" + colorName))
                            .texture("2", Create.asResource("block/seat/side_" + colorName)));
                })*/
                .recipe((c, p) -> {
                    ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                            .requires(DyeHelper.getWoolOfDye(color))
                            .requires(ItemTags.WOODEN_PRESSURE_PLATES)
                            .unlockedBy("has_wool", RegistrateRecipeProvider.has(ItemTags.WOOL))
                            .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName()));
                    ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                            .requires(color.getTag())
                            .requires(CALTags.CALItemTags.SHORT_SEATS.tag)
                            .unlockedBy("has_short_seat", RegistrateRecipeProvider.has(CALTags.CALItemTags.SHORT_SEATS.tag))
                            .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName() + "_from_other_seat"));
                })
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.seat"))
                .tag(CALTags.CALBlockTags.SHORT_SEATS.tag)
                .item()
                .tag(CALTags.CALItemTags.SHORT_SEATS.tag)
                .tab(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(), (c,p) -> p.accept(c.get(), color == DyeColor.RED ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY))
                .build()
                .register();
    });

    // Tall Seats
    public static final DyedBlockList<TallSeatBlock> TALL_SEATS = new DyedBlockList<>(color -> {
        String colorName = color.getSerializedName();
        SeatMovementBehaviour movementBehaviour = new SeatMovementBehaviour();
        SeatInteractionBehaviour interactionBehaviour = new SeatInteractionBehaviour();
        return REGISTRATE.block(colorName + "_tall_seat", p -> new TallSeatBlock(p, color))
                .initialProperties(SharedProperties::wooden)
                .properties(p -> p.mapColor(color))
                .transform(axeOnly())
                .onRegister(movementBehaviour(movementBehaviour))
                .onRegister(interactionBehaviour(interactionBehaviour))
                .transform(displaySource(AllDisplaySources.ENTITY_NAME))
                .blockstate((c, p) -> {
                    p.simpleBlock(c.get(), p.models().getExistingFile(p.modLoc("block/tall_seat/" + colorName)));
                })
                /*.blockstate((c, p) -> {
                    p.simpleBlock(c.get(), p.models()
                            .withExistingParent(colorName + "_tall_seat", p.modLoc("block/tall_seat/base"))
                            .texture("1", Create.asResource("block/seat/top_" + colorName))
                            .texture("2", Create.asResource("block/seat/side_" + colorName)));
                })*/
                .recipe((c, p) -> {
                    ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                            .requires(DyeHelper.getWoolOfDye(color))
                            .requires(ItemTags.PLANKS)
                            .unlockedBy("has_wool", RegistrateRecipeProvider.has(ItemTags.WOOL))
                            .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName()));
                    ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, c.get())
                            .requires(color.getTag())
                            .requires(CALTags.CALItemTags.TALL_SEATS.tag)
                            .unlockedBy("has_tall_seat", RegistrateRecipeProvider.has(CALTags.CALItemTags.TALL_SEATS.tag))
                            .save(p, CreateAdditionalLogistics.asResource("crafting/kinetics/" + c.getName() + "_from_other_seat"));
                })
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.seat"))
                .tag(CALTags.CALBlockTags.TALL_SEATS.tag)
                .item()
                .tag(CALTags.CALItemTags.TALL_SEATS.tag)
                .tab(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(), (c,p) -> p.accept(c.get(), color == DyeColor.RED ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY))
                .build()
                .register();
    });

    public static void register() { }

}
