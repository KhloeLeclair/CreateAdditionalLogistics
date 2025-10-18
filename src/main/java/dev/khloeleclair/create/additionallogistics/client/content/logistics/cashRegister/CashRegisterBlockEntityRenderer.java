package dev.khloeleclair.create.additionallogistics.client.content.logistics.cashRegister;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CashRegisterBlockEntityRenderer extends SmartBlockEntityRenderer<CashRegisterBlockEntity> {

    public CashRegisterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CashRegisterBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

        var state = blockEntity.getBlockState();
        var dir = state.getValue(CashRegisterBlock.FACING);

        if (blockEntity.hadRecentSale()) {
            SuperByteBuffer sale = CachedBuffers.partialFacing(CALPartialModels.CASH_REGISTER_SALE_NOTICE, state, dir);
            sale.light(light).renderInto(ms, buffer.getBuffer(RenderType.CUTOUT_MIPPED));
        }

        SuperByteBuffer glass = CachedBuffers.partialFacing(CALPartialModels.CASH_REGISTER_GLASS, state, dir);
        glass.light(light).renderInto(ms, buffer.getBuffer(RenderType.TRANSLUCENT));

    }

}
