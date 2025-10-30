package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SalesLedgerItem extends Item {

    public SalesLedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        var data = SalesHistoryData.get(stack);
        if (data == null)
            return;

        tooltipComponents.add(
                CALLang.translate(
                        "sales.entries",
                        CALLang.number(data.saleCount()).style(ChatFormatting.GOLD)
                ).style(ChatFormatting.GRAY).component()
        );

        var cmp = data.getTimeRange(ChatFormatting.AQUA);
        if (cmp != null)
            tooltipComponents.add(cmp.withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 1000;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return getDamage(stack) != 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public int getDamage(ItemStack stack) {
        var history = SalesHistoryData.get(stack);
        return history == null ? 0 : history.saleCount();
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null)
            return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!level.isClientSide && player instanceof ServerPlayer sp)
                CALPackets.OpenSalesLedgerScreen.create(heldItem).ifPresent(p -> p.send(sp));
            return InteractionResultHolder.success(heldItem);
        }

        return InteractionResultHolder.pass(heldItem);
    }
}
