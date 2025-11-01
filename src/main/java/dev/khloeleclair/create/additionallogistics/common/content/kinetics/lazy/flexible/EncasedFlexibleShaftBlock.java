package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class EncasedFlexibleShaftBlock extends AbstractFlexibleShaftBlock implements SpecialBlockItemRequirement, EncasedBlock {

    // Encased Flexible Shafts use the sides properties differently than the base block.
    // If a side is true, that means there's no shaft on that side.

    public static final VoxelShape ENCASED_SHAPE = Block.box(1, 1, 1, 15, 15, 15);

    public static final VoxelShape[] ENCASED_SIDE_SHAPES = {
            Block.box(0, 0, 0, 16, 1, 16), // Bottom
            Block.box(0, 15, 0, 16, 16, 16), // Top

            Block.box(0, 0, 0, 16, 16, 1),
            Block.box(0, 0, 15, 16, 16, 16),

            Block.box(0, 0, 0, 1, 16, 16),
            Block.box(15, 0, 0, 16, 16, 16)
    };

    private static final Map<Byte, VoxelShape> ENCASED_SHAPE_CACHE = new Byte2ObjectOpenHashMap<>();

    protected VoxelShape getShapeWithSides(byte key) {
        VoxelShape cached;

        synchronized (ENCASED_SHAPE_CACHE) {
            cached = ENCASED_SHAPE_CACHE.get(key);
        }

        if (cached == null) {
            cached = ENCASED_SHAPE;
            for(Direction dir : Iterate.directions) {
                int index = dir.ordinal();
                if ((key & (1 << index)) != 0)
                    cached = Shapes.joinUnoptimized(cached, ENCASED_SIDE_SHAPES[index], BooleanOp.OR);
            }

            cached = cached.optimize();
            synchronized (ENCASED_SHAPE_CACHE) {
                ENCASED_SHAPE_CACHE.put(key, cached);
            }
        }

        return cached;
    }

    private final Supplier<Block> casing;

    public EncasedFlexibleShaftBlock(Properties properties, Supplier<Block> casing) {
        this(properties, null, casing);
    }

    public EncasedFlexibleShaftBlock(Properties properties, @Nullable DyeColor color, Supplier<Block> casing) {
        super(properties, color);
        this.casing = casing;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        var prop = SIDES[direction.ordinal()];

        // Do we connect to whatever the new neighbor state is?
        boolean connects = connectsTo(level, pos, state, direction, neighborPos, level.getBlockState(neighborPos));

        // If the side is false, we need to update the tile entity.
        if (connects && !state.getValue(prop)) {
            state = state.setValue(prop, true);

            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb)
                fsb.setSide(direction, (byte)0);
        }

        // We can't make any assumptions about connections because of how we use our sides, so always
        // mark dirty.
        if (level instanceof ServerLevel sl)
            AbstractLowEntityKineticBlockEntity.markDirty(sl, pos);

        return state;
    }

    protected BlockEntry<FlexibleShaftBlock> getUnencased() {
        if (color == null)
            return CALBlocks.FLEXIBLE_SHAFT;
        return CALBlocks.DYED_FLEXIBLE_SHAFTS.get(color);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        final var level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        level.levelEvent(2001, context.getClickedPos(), Block.getId(state));
        KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                getConnectedState(context.getLevel(), context.getClickedPos(), getUnencased().getDefaultState().setValue(ACTIVE, state.getValue(ACTIVE))));

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult result)
            return !state.getValue(SIDES[result.getDirection().ordinal()])
                ? getUnencased().asStack()
                : getCasing().asItem().getDefaultInstance();

        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        return ItemRequirement.of(getUnencased().getDefaultState(), blockEntity);
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
        BlockState newState = defaultBlockState().setValue(ACTIVE, state.getValue(ACTIVE));
        @Nullable FlexibleShaftBlockEntity be;
        if (level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb)
            be = fsb;
        else
            be = null;

        for(Direction dir : Iterate.directions) {
            newState = newState.setValue(SIDES[dir.ordinal()], be == null || be.getSide(dir) == 0);
        }

        KineticBlockEntity.switchToBlockState(level, pos, newState);
    }
}
