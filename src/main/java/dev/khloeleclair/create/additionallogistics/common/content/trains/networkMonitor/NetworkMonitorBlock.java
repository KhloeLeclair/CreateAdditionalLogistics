package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkMonitorBlock extends Block implements IBE<NetworkMonitorBlockEntity>, IWrenchable {

    public NetworkMonitorBlock(Properties properties) {
        super(properties);
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }
}
