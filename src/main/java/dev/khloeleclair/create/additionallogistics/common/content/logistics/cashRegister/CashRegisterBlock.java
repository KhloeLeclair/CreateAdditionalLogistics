package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CashRegisterBlock extends StockTickerBlock {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static final MapCodec<CashRegisterBlock> CODEC = simpleCodec(CashRegisterBlock::new);

    public static VoxelShaper SHAPES = VoxelShaper.forHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(0, 4, 10, 16, 13, 16),
            Block.box(0, 13, 12, 16, 16, 15),

            // Front
            Block.box(2, 4, 2, 16, 5, 10),
            Block.box(2, 5, 4, 16, 7, 10),
            Block.box(2, 7, 6, 16, 9, 10),
            Block.box(2, 9, 8, 16, 11, 10)
    ), Direction.NORTH);

    public CashRegisterBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(OPEN);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    public BlockEntityType<? extends StockTickerBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.CASH_REGISTER.get();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CashRegisterBlockEntity cbe)
            cbe.recheckOpen();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof LogisticallyLinkedBlockItem)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!(level.getBlockEntity(pos) instanceof CashRegisterBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (! be.behaviour.mayInteractMessage(player))
            return ItemInteractionResult.SUCCESS;

        // Unlike the default stock ticker, players can't extract payments just by clicking.
        // Instead, we use a menu. Or they extract items in the world with hoppers, funnels, etc.
        if (player instanceof ServerPlayer sp)
            sp.openMenu(be.new CashRegisterMenuProvider(), be.getBlockPos());

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
