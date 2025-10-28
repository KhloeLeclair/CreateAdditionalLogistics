package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.minecraftforge.network.NetworkHooks;

public class CashRegisterBlock extends StockTickerBlock {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static VoxelShape[] SHAPES;

    static {
        SHAPES = new VoxelShape[Iterate.horizontalDirections.length];

        for(int i = 0; i < SHAPES.length; i++) {
            Direction dir = Iterate.horizontalDirections[i];
            SHAPES[i] = buildShape(dir);
        }
    }


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

    public static VoxelShape buildShape(Direction facing) {
        double x1, x2, z1, z2;

        switch(facing) {
            case NORTH:
                x1 = 0;
                z1 = 10/16.0;
                x2 = 1;
                z2 = 1f;
                break;

            case EAST:
                x1 = 0;
                z1 = 0;
                x2 = 6/16.0;
                z2 = 1;
                break;

            case WEST:
                x1 = 10/16.0;
                z1 = 0;
                x2 = 1;
                z2 = 1;
                break;

            case SOUTH:
            default:
                x1 = 0;
                z1 = 0;
                x2 = 1;
                z2 = 6/16.0;
                break;
        }

        return Shapes.or(
                Shapes.box(0, 0, 0, 16/16.0, 6/16.0, 16/16.0),
                Shapes.box(x1, 6/16.0, z1, x2, 1, z2)
        );
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(FACING).get2DDataValue() % SHAPES.length];
    }

    @Override
    public BlockEntityType<? extends StockTickerBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.CASH_REGISTER.get();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CashRegisterBlockEntity cbe)
            cbe.recheckOpen();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player != null && player.getItemInHand(hand).getItem() instanceof LogisticallyLinkedBlockItem)
            return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof CashRegisterBlockEntity be))
            return InteractionResult.PASS;

        if (!be.behaviour.mayInteractMessage(player))
            return InteractionResult.SUCCESS;

        // Unlike the default stock ticker, players can't extract payments just by clicking.
        // Instead, we use a menu. Or they extract items in the world with hoppers, funnels, etc.
        if (player instanceof ServerPlayer sp)
            NetworkHooks.openScreen(sp, be.new CashRegisterMenuProvider(), be.getBlockPos());

        return InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

}
