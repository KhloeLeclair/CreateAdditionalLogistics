package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.AllShapes;
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

public class ShortSeatBlock extends AbstractSeatBlock {

    public static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public ShortSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
    }

    @Override
    protected BlockState getColoredState(DyeColor color) {
        return CALBlocks.SHORT_SEATS.get(color).getDefaultState();
    }

    @Override
    protected Vec3 getSeatPosition(Level level, BlockPos pos) {
        return Vec3.upFromBottomCenterOf(pos, 0.125);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Our collision shape is too short to work properly, so pretend to be taller
        // when dealing with falling entities for the purpose of claiming a passenger.
        if (ctx instanceof EntityCollisionContext ecc &&
                !(ecc.getEntity() instanceof Player) &&
                ecc.isAbove(SHAPE, pos, false)
        ) {
            return AllShapes.SEAT_COLLISION;
        }

        return SHAPE;
    }

}
