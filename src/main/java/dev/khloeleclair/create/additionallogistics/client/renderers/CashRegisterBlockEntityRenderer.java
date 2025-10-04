package dev.khloeleclair.create.additionallogistics.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.khloeleclair.create.additionallogistics.common.blockentities.CashRegisterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CashRegisterBlockEntityRenderer extends SmartBlockEntityRenderer<CashRegisterBlockEntity> {

    public CashRegisterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CashRegisterBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {


    }

}
