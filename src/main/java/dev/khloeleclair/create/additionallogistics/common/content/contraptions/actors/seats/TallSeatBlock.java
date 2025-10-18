package dev.khloeleclair.create.additionallogistics.common.content.contraptions.actors.seats;

import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TallSeatBlock extends AbstractSeatBlock {

    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);
    public static final VoxelShape COLLISION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);
    public static final VoxelShape COLLISION_PLAYER = Block.box(0.0, 0.0, 0.0, 16.0, 9.0, 16.0);

    public TallSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
    }

    @Override
    protected BlockState getColoredState(DyeColor color) {
        return CALBlocks.TALL_SEATS.get(color).getDefaultState();
    }

    @Override
    public Vec3 getSeatPosition(@Nullable Level level, BlockPos pos) {
        return Vec3.upFromBottomCenterOf(pos, 13/16.0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player)
            return COLLISION_PLAYER;
        return COLLISION_SHAPE;
    }

}
