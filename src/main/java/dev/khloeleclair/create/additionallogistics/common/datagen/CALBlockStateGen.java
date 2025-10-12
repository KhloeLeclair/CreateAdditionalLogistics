package dev.khloeleclair.create.additionallogistics.common.datagen;

import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.khloeleclair.create.additionallogistics.common.blocks.*;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.Z)
                    .end();

            builder
                    .part()
                    .modelFile(model)
                    .rotationY(90)
                    .addModel()
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.X)
                    .end();

            builder
                    .part()
                    .modelFile(model)
                    .rotationX(90)
                    .addModel()
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.Y)
                    .end();

            for(Direction dir : Iterate.directions) {
                BooleanProperty prop = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? AbstractLazyShaftBlock.POSITIVE
                        : AbstractLazyShaftBlock.NEGATIVE;

                ModelFile m = p.models()
                        .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc("block/flexible_shaft/block_" + dir.getSerializedName()))
                        .texture("frame", p.modLoc("block/lazy_shaft/frame"));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(AbstractLazyShaftBlock.AXIS, dir.getAxis())
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
                    .condition(AbstractLazyShaftBlock.NEGATIVE, false)
                    .condition(AbstractLazyShaftBlock.POSITIVE, false)
                    .end();

            ModelFile connection = p.models().getExistingFile(p.modLoc(lazyPath + "/connection"));

            builder
                    .part()
                    .modelFile(connection)
                    .addModel()
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.Z)
                    .condition(AbstractLazyShaftBlock.POSITIVE, true)
                    .condition(AbstractLazyShaftBlock.NEGATIVE, true)
                    .end();

            builder
                    .part()
                    .modelFile(connection)
                    .rotationY(90)
                    .addModel()
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.X)
                    .condition(AbstractLazyShaftBlock.POSITIVE, true)
                    .condition(AbstractLazyShaftBlock.NEGATIVE, true)
                    .end();

            builder
                    .part()
                    .modelFile(connection)
                    .rotationX(90)
                    .addModel()
                    .condition(AbstractLazyShaftBlock.AXIS, Direction.Axis.Y)
                    .condition(AbstractLazyShaftBlock.POSITIVE, true)
                    .condition(AbstractLazyShaftBlock.NEGATIVE, true)
                    .end();

            for(Direction dir : Iterate.directions) {
                BooleanProperty prop = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                        ? AbstractLazyShaftBlock.POSITIVE
                        : AbstractLazyShaftBlock.NEGATIVE;

                BooleanProperty otherProp = dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                        ? AbstractLazyShaftBlock.POSITIVE
                        : AbstractLazyShaftBlock.NEGATIVE;

                ModelFile m = p.models()
                        .withExistingParent(c.getName() + "_" + dir.getSerializedName(), p.modLoc(path + "/block_" + dir.getSerializedName()))
                        .texture("frame", p.modLoc(lazyPath + "/frame"));

                builder
                        .part()
                        .modelFile(m)
                        .addModel()
                        .condition(AbstractLazyShaftBlock.AXIS, dir.getAxis())
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
                        (s, f) -> f.getAxis() != s.getValue(AbstractLazyShaftBlock.AXIS))))
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

}
