package dev.khloeleclair.create.additionallogistics.common.content.trains.networkObserver;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NetworkObserverBlock extends Block implements IBE<NetworkObserverBlockEntity>, IWrenchable {

    public NetworkObserverBlock(Properties properties) {
        super(properties);

    }

    @Override
    public Class<NetworkObserverBlockEntity> getBlockEntityClass() {
        return NetworkObserverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NetworkObserverBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.NETWORK_OBSERVER.get();
    }
}
