package dev.khloeleclair.create.additionallogistics.common.content.logistics.packageAccelerator;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PackageAcceleratorBlock extends DirectionalKineticBlock implements IBE<PackageAcceleratorBlockEntity>, ICogWheel {

    public PackageAcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PackagerBlockEntity) {
            if (!level.isClientSide && stack.getItem() instanceof BlockItem bi) {
                var result = bi.place(new BlockPlaceContext(player, hand, stack, hitResult));
                if (result == InteractionResult.CONSUME)
                    return ItemInteractionResult.CONSUME;
                else if (result == InteractionResult.FAIL)
                    return ItemInteractionResult.FAIL;
                else if (result == InteractionResult.SUCCESS)
                    return ItemInteractionResult.SUCCESS;
            }

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VoxelShaper.forDirectional(AllShapes.MILLSTONE, Direction.UP).get(state.getValue(FACING));
    }

    @Override
    public Direction getPreferredFacing(BlockPlaceContext context) {
        final var level = context.getLevel();
        final var pos = context.getClickedPos();

        for(Direction dir : Iterate.directions) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof PackagerBlockEntity)
                return dir.getOpposite();
        }

        return super.getPreferredFacing(context);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Class<PackageAcceleratorBlockEntity> getBlockEntityClass() {
        return PackageAcceleratorBlockEntity.class;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public BlockEntityType<? extends PackageAcceleratorBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.PACKAGE_ACCELERATOR.get();
    }
}
