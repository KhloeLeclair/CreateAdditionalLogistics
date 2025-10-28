package dev.khloeleclair.create.additionallogistics.common.datagen;

import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazySimpleKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.cog.EncasedLazyCogWheelBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.cog.LazyCogWheelBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.EncasedFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.AbstractFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft.EncasedLazyShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft.LazyShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;

public class CALBlockStateGen {

    public static <B extends EncasedFlexibleShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedFlexibleShaft(
            String casing,
            DyeColor color,
            @Nullable Supplier<CTSpriteShiftEntry> casingShift
    ) {
        return encasedFlexibleShaft(
                () -> CALBlocks.DYED_FLEXIBLE_SHAFTS.get(color),
                casing,
                ResourceLocation.withDefaultNamespace("block/" + color.getSerializedName() + "_concrete"),
                casingShift
        );
    }

    public static <B extends EncasedFlexibleShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedFlexibleShaft(
            String casing,
            @Nullable Supplier<CTSpriteShiftEntry> casingShift
    ) {
        return encasedFlexibleShaft(CALBlocks.FLEXIBLE_SHAFT::get, casing, null, casingShift);
    }

    public static <B extends EncasedFlexibleShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedFlexibleShaft(
        Supplier<ItemLike> drop,
        String casing,
        @Nullable ResourceLocation coreTexture,
        @Nullable Supplier<CTSpriteShiftEntry> casingShift
    ) {

        return b -> {
            b.loot((p, lb) -> p.dropOther(lb, drop.get()));
            b.blockstate(encasedFlexibleShaftBlockState(casing, coreTexture));

            if (casingShift != null) {
                b.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())));
                b.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
                        (s, f) -> s.getValue(AbstractFlexibleShaftBlock.SIDES[f.ordinal()]))));
            }

            return b;
        };
    }

    private static <B extends EncasedFlexibleShaftBlock> NonNullBiConsumer<DataGenContext<Block, B>, RegistrateBlockstateProvider> encasedFlexibleShaftBlockState(
            String casing,
            @Nullable ResourceLocation coreTexture
    ) {
        return (c, p) -> {
            String path = "block/encased_flexible_shaft";

            ModelFile model;
            if (coreTexture == null)
                model = p.models().getExistingFile(p.modLoc(path + "/base"));
            else
                model = p.models()
                        .withExistingParent(c.getName(), p.modLoc(path + "/base"))
                        .texture("particle", coreTexture)
                        .texture("core", coreTexture);

            MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
            builder
                    .part()
                    .modelFile(model)
                    .addModel()
                    .end();

            for(Direction dir : Iterate.directions) {
                int index = dir.ordinal();
                BooleanProperty property = AbstractFlexibleShaftBlock.SIDES[index];

                ModelFile m = p.models()
                        .withExistingParent("encased_flexible_shaft_" + casing + "_" + dir.getSerializedName(), p.modLoc(path + "/block_" + dir.getSerializedName()))
                        .texture("frame", Create.asResource("block/" + casing));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(property, true)
                        .end();
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, FlexibleShaftBlock>, RegistrateBlockstateProvider> flexibleShaft(@Nullable String name, @Nullable ResourceLocation texture) {
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
                BooleanProperty property = AbstractFlexibleShaftBlock.SIDES[index];

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

    public static NonNullBiConsumer<DataGenContext<Block, LazyCogWheelBlock>, RegistrateBlockstateProvider> lazyCog() {
        return (c, p) -> {
            String path = "block/lazy_cog";

            ModelFile model = p.models()
                    .getExistingFile(p.modLoc(path + "/base"));

            MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
            builder
                    .part()
                    .modelFile(model)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.Z)
                    .end();

            builder
                    .part()
                    .modelFile(model)
                    .rotationY(90)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.X)
                    .end();

            builder
                    .part()
                    .modelFile(model)
                    .rotationX(90)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.Y)
                    .end();

            for(Direction dir : Iterate.directions) {
                BooleanProperty prop = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? AbstractLazySimpleKineticBlock.POSITIVE
                        : AbstractLazySimpleKineticBlock.NEGATIVE;

                ModelFile m = p.models()
                        .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc("block/flexible_shaft/block_" + dir.getSerializedName()))
                        .texture("frame", p.modLoc("block/lazy_shaft/frame"));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(AbstractLazySimpleKineticBlock.AXIS, dir.getAxis())
                        .condition(prop, true)
                        .end();
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Block, LazyShaftBlock>, RegistrateBlockstateProvider> lazyShaftBlockState() {
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
                    .useOr()
                    .condition(AbstractLazySimpleKineticBlock.NEGATIVE, false)
                    .condition(AbstractLazySimpleKineticBlock.POSITIVE, false)
                    .end();

            ModelFile connection = p.models().getExistingFile(p.modLoc(lazyPath + "/connection"));

            builder
                    .part()
                    .modelFile(connection)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.Z)
                    .condition(AbstractLazySimpleKineticBlock.POSITIVE, true)
                    .condition(AbstractLazySimpleKineticBlock.NEGATIVE, true)
                    .end();

            builder
                    .part()
                    .modelFile(connection)
                    .rotationY(90)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.X)
                    .condition(AbstractLazySimpleKineticBlock.POSITIVE, true)
                    .condition(AbstractLazySimpleKineticBlock.NEGATIVE, true)
                    .end();

            builder
                    .part()
                    .modelFile(connection)
                    .rotationX(90)
                    .addModel()
                    .condition(AbstractLazySimpleKineticBlock.AXIS, Direction.Axis.Y)
                    .condition(AbstractLazySimpleKineticBlock.POSITIVE, true)
                    .condition(AbstractLazySimpleKineticBlock.NEGATIVE, true)
                    .end();

            for(Direction dir : Iterate.directions) {
                BooleanProperty prop = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? AbstractLazySimpleKineticBlock.POSITIVE
                        : AbstractLazySimpleKineticBlock.NEGATIVE;

                BooleanProperty otherProp = dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                        ? AbstractLazySimpleKineticBlock.POSITIVE
                        : AbstractLazySimpleKineticBlock.NEGATIVE;

                ModelFile m = p.models()
                        .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc(path + "/block_" + dir.getSerializedName()))
                        .texture("frame", p.modLoc(lazyPath + "/frame"));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(AbstractLazySimpleKineticBlock.AXIS, dir.getAxis())
                        .condition(prop, true)
                        .condition(otherProp, false)
                        .end();
            }
        };
    }

    private static <B extends Block, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b,
                                                                       Supplier<ItemLike> drop) {
        return b.initialProperties(SharedProperties::stone)
                .properties(BlockBehaviour.Properties::noOcclusion)
                .loot((p, lb) -> p.dropOther(lb, drop.get()));
    }

    public static <B extends EncasedLazyShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyShaft(
            String casing, Supplier<CTSpriteShiftEntry> casingShift
    ) {
        return encasedLazyShaft(casing, Create.asResource("block/" + casing + "_casing"), Create.asResource("block/" + casing + "_gearbox"), casingShift);
    }

    public static <B extends EncasedLazyShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyShaft(
            String casing, ResourceLocation openingLocation, Supplier<CTSpriteShiftEntry> casingShift
    ) {
        return encasedLazyShaft(casing, Create.asResource("block/" + casing + "_casing"), openingLocation, casingShift);
    }

    public static <B extends EncasedLazyShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyShaft(
            String casing, ResourceLocation casingLocation, ResourceLocation openingLocation, Supplier<CTSpriteShiftEntry> casingShift
    ) {
        return builder -> encasedBase(builder, CALBlocks.LAZY_SHAFT::get)
                .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get())))
                .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
                        (s, f) -> f.getAxis() != s.getValue(AbstractLazySimpleKineticBlock.AXIS))))
                .blockstate((c, p) -> axisBlock(c, p, blockState -> p.models()
                                .withExistingParent("block/encased_lazy_shaft/block_" + casing, Create.asResource("block/encased_shaft/block"))
                                .texture("casing", casingLocation)
                                .texture("opening", openingLocation)
                        , true))
                .item()
                .model((c,p) ->
                        p.withExistingParent("block/encased_lazy_shaft/item_" + casing, Create.asResource("block/encased_shaft/item"))
                                .texture("casing", casingLocation)
                                .texture("opening", openingLocation))
                .build();
    }

    public static <B extends EncasedLazyShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> layeredEncasedLazyShaft(
            String casing, ResourceLocation openingLocation, Supplier<CTSpriteShiftEntry> casingShift, Supplier<CTSpriteShiftEntry> casingShift2
    ) {
        return builder -> encasedBase(builder, CALBlocks.LAZY_SHAFT::get)
                .onRegister(CreateRegistrate.connectedTextures(() -> new HorizontalCTBehaviour(casingShift.get(), casingShift2.get())))
                .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
                        (s, f) -> f.getAxis() != s.getValue(AbstractLazySimpleKineticBlock.AXIS))))
                .blockstate((c, p) -> axisBlock(c, p, blockState -> p.models()
                                .withExistingParent("block/encased_lazy_shaft/block_" + casing, p.modLoc("block/lazy_shaft/layered_encased"))
                                .texture("side", casingShift.get().getOriginalResourceLocation())
                                .texture("end", casingShift2.get().getOriginalResourceLocation())
                                .texture("opening", openingLocation)
                        , true))
                .item()
                .model((c,p) ->
                        p.withExistingParent("block/encased_lazy_shaft/item_" + casing, p.modLoc("block/lazy_shaft/layered_encased"))
                                .texture("side", casingShift.get().getOriginalResourceLocation())
                                .texture("end", casingShift2.get().getOriginalResourceLocation())
                                .texture("opening", openingLocation))
                .build();
    }

    public static <B extends EncasedLazyShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyShaftNotConnected(
            String casing, ResourceLocation casingLocation, ResourceLocation openingLocation
    ) {
        return builder -> encasedBase(builder, CALBlocks.LAZY_SHAFT::get)
                .blockstate((c, p) -> axisBlock(c, p, blockState -> p.models()
                                .withExistingParent("block/encased_lazy_shaft/block_" + casing, Create.asResource("block/encased_shaft/block"))
                                .texture("casing", casingLocation)
                                .texture("opening", openingLocation)
                        , true))
                .item()
                .model((c,p) ->
                        p.withExistingParent("block/encased_lazy_shaft/item_" + casing, Create.asResource("block/encased_shaft/item"))
                                .texture("casing", casingLocation)
                                .texture("opening", openingLocation))
                .build();

    }

    public static <B extends EncasedLazyCogWheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyCogwheel(
            String casing, @Nullable Supplier<CTSpriteShiftEntry> casingShift) {
        return b -> encasedLazyCogwheelBase(b, casing, casingShift, CALBlocks.LAZY_COGWHEEL::get, false);
    }

    public static <B extends EncasedLazyCogWheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLazyLargeCogwheel(
            String casing, @Nullable Supplier<CTSpriteShiftEntry> casingShift) {
        return b -> encasedLazyCogwheelBase(b, casing, casingShift, CALBlocks.LAZY_LARGE_COGWHEEL::get, true);
    }

    @SuppressWarnings("removal") // addLayer use is copied from Create
    private static <B extends EncasedLazyCogWheelBlock, P> BlockBuilder<B, P> encasedLazyCogwheelBase(BlockBuilder<B, P> b,
                                                                                              String casing, @Nullable Supplier<CTSpriteShiftEntry> casingShift, Supplier<ItemLike> drop, boolean large) {
        boolean side_is_create = casing.equals("andesite") || casing.equals("brass");
        String encasedSuffix = "_encased_cogwheel_side" + (large ? "_connected" : "");
        String blockFolder = large ? "encased_large_cogwheel" : "encased_cogwheel";
        ResourceLocation wood;
        ResourceLocation gearbox;
        String casing_postfix;

        if (casing.equals("brass")) {
            wood = strippedLog("dark_oak");
            casing_postfix = "_casing";
            gearbox = Create.asResource("block/brass_gearbox");
        } else if (casing.equals("copper")) {
            wood = CreateAdditionalLogistics.asResource("block/copper_gearbox");
            casing_postfix = "_casing";
            gearbox = CreateAdditionalLogistics.asResource("block/copper_gearbox");
        } else if (casing.equals("industrial_iron")) {
            wood = Create.asResource("block/industrial_iron_block");
            casing_postfix = "_block";
            gearbox = CreateAdditionalLogistics.asResource("block/industrial_iron_gearbox");
        } else if (casing.equals("weathered_iron")) {
            wood = Create.asResource("block/weathered_iron_block");
            casing_postfix = "_block";
            gearbox = CreateAdditionalLogistics.asResource("block/weathered_iron_gearbox");
        } else {
            wood = strippedLog("spruce");
            casing_postfix = "_casing";
            gearbox = Create.asResource("block/gearbox");
        }

        return encasedBase(b, drop).addLayer(() -> RenderType::cutoutMipped)
                .onRegister(thing -> {
                    if (casingShift != null)
                        CreateRegistrate.casingConnectivity((block, cc) -> cc.make(block, casingShift.get(),
                                (s, f) -> f.getAxis() == s.getValue(AbstractLazySimpleKineticBlock.AXIS)
                                        && s.getValue(f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? AbstractLazySimpleKineticBlock.POSITIVE
                                        : AbstractLazySimpleKineticBlock.NEGATIVE))).accept(thing);
                })
                .blockstate((c, p) -> axisBlock(c, p, blockState -> {
                    String suffix = (blockState.getValue(AbstractLazySimpleKineticBlock.POSITIVE) ? "" : "_top")
                            + (blockState.getValue(AbstractLazySimpleKineticBlock.NEGATIVE) ? "" : "_bottom");
                    String modelName = c.getName() + suffix;
                    return p.models()
                            .withExistingParent(modelName, Create.asResource("block/" + blockFolder + "/block" + suffix))
                            .texture("casing", Create.asResource("block/" + casing + casing_postfix))
                            .texture("particle", Create.asResource("block/" + casing + casing_postfix))
                            .texture("4", gearbox)
                            .texture("1", wood)
                            .texture("side", side_is_create ? Create.asResource("block/" + casing + encasedSuffix) : CreateAdditionalLogistics.asResource("block/" + casing + encasedSuffix));
                }, false));
    }

    private static ResourceLocation strippedLog(String wood) {
        return ResourceLocation.withDefaultNamespace("block/stripped_" + wood + "_log_top");
    }


}
