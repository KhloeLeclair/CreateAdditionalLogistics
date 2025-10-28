package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetworkMonitorBlock extends Block implements IBE<NetworkMonitorBlockEntity>, IWrenchable {

    public static VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 11, 16),
            Block.box(2, 11, 2, 14, 14, 14)
    );

    public NetworkMonitorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<NetworkMonitorBlockEntity> getBlockEntityClass() {
        return NetworkMonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetworkMonitorBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.NETWORK_MONITOR.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }
}
