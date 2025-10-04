package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.blockentities.PackageEditorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class PackageEditorBlock extends PackagerBlock {

    public PackageEditorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.PACKAGE_EDITOR.get();
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        if (level.isClientSide())
            return;
        if (level.getBlockEntity(pos) instanceof PackageEditorBlockEntity pe)
            pe.maybeUpdateReplacements(neighbor);
    }

}
