package dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy;

import dev.khloeleclair.create.additionallogistics.client.widgets.BlockPreviewWidget;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.EncasedFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPackets;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FlexibleShaftScreen extends AbstractSimiScreen {

    private final BlockPos pos;
    private final Level level;
    private final Player player;

    private BlockPreviewWidget widget;

    private final Component label;

    public FlexibleShaftScreen(BlockPos pos) {
        super();
        this.pos = pos;

        player = Minecraft.getInstance().player;
        level = Minecraft.getInstance().level;

        label = level.getBlockState(pos).getBlock().getName();
    }

    @Override
    protected void init() {
        super.init();

        GridLayout layout = new GridLayout();

        layout.defaultCellSetting().padding(2).alignVerticallyMiddle().alignHorizontallyCenter();
        var helper = layout.createRowHelper(1);

        helper.addChild(new StringWidget(label, Minecraft.getInstance().font).alignCenter());

        widget = new BlockPreviewWidget(0, 0, 600, 300, pos);

        widget.canSelectDirection(dir -> {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FlexibleShaftBlock))
                return false;

            if (state.getBlock() instanceof EncasedFlexibleShaftBlock fsb)
                return !fsb.connectsTo(level, pos, state, dir);

            var prop = FlexibleShaftBlock.SIDES[dir.ordinal()];
            return ! state.getValue(prop);
        });

        widget.onClick((dir, button) -> {
            byte existing;
            if (level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb)
                existing = fsb.getSide(dir);
            else
                existing = 0;

            byte target;
            if (button == 0) {
                target = switch (existing) {
                    case 1 -> -1;
                    case -1 -> 0;
                    default -> 1;
                };
            } else {
                target = switch (existing) {
                    case 1 -> 0;
                    case -1 -> 1;
                    default -> -1;
                };
            }

            CALPackets.ConfigureFlexibleShaft.of(pos, dir, target).send();
        });

        helper.addChild(widget);

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);

        layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        super.onClose();

        CALPackets.FinishedConfiguringFlexibleShaft.of(pos).send();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (widget.visible && widget.isMouseOver(mouseX, mouseY))
            return widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (widget.visible && widget.isMouseOver(mouseX, mouseY))
            return widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {



    }
}
