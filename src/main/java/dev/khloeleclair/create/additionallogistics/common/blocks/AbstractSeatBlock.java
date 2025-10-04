package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.khloeleclair.create.additionallogistics.common.entities.CustomSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public abstract class AbstractSeatBlock extends SeatBlock {

    public AbstractSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
    }

    protected abstract BlockState getColoredState(DyeColor color);

    protected abstract Vec3 getSeatPosition(Level level, BlockPos pos);

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        DyeColor color = DyeColor.getColor(stack);
        if (color != null && color != this.color) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            BlockState newState = BlockHelper.copyProperties(state, getColoredState(color));
            level.setBlockAndUpdate(pos, newState);
            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public void handleSitDown(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide)
            return;

        SeatEntity seat = new CustomSeatEntity(level, pos);
        var seatPos = getSeatPosition(level, pos);
        seat.setPos(seatPos.x, seatPos.y, seatPos.z);

        level.addFreshEntity(seat);
        entity.startRiding(seat, true);
        if (entity instanceof TamableAnimal ta)
            ta.setInSittingPose(true);
    }

}
