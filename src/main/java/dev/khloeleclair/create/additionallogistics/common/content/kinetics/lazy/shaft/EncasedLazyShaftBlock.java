package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class EncasedLazyShaftBlock extends AbstractLazyShaftBlock implements SpecialBlockItemRequirement, EncasedBlock {

    private final Supplier<Block> casing;

    public EncasedLazyShaftBlock(Properties properties, Supplier<Block> casing) {
        super(properties);
        this.casing = casing;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        final var level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        level.levelEvent(2001, context.getClickedPos(), Block.getId(state));
        KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                copyValuesForCasing(state, CALBlocks.LAZY_SHAFT.getDefaultState()));

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult result)
            return result.getDirection().getAxis() == state.getValue(AXIS)
                    ? CALBlocks.LAZY_SHAFT.asStack()
                    : getCasing().asItem().getDefaultInstance();

        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        return ItemRequirement.of(CALBlocks.LAZY_SHAFT.getDefaultState(), blockEntity);
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
        KineticBlockEntity.switchToBlockState(level, pos, copyValuesForCasing(state, defaultBlockState()));
    }
}
