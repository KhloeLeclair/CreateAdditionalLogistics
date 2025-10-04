package dev.khloeleclair.create.additionallogistics.client.screen;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import dev.khloeleclair.create.additionallogistics.client.registries.CALGuiTextures;
import dev.khloeleclair.create.additionallogistics.common.blocks.CashRegisterBlock;
import dev.khloeleclair.create.additionallogistics.common.menu.CashRegisterMenu;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CashRegisterScreen extends AbstractSimiContainerScreen<CashRegisterMenu> {

    private final CALGuiTextures background;
    private final BlockState icon;

    private List<Rect2i> extraAreas = Collections.emptyList();

    public CashRegisterScreen(CashRegisterMenu container, Inventory inv, Component title) {
        super(container, inv, title);

        background = CALGuiTextures.CASH_REGISTER_BG;
        icon = container.contentHolder.getBlockState().setValue(CashRegisterBlock.FACING, Direction.WEST).setValue(CashRegisterBlock.OPEN, true);
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), background.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();

        int x = getGuiLeft();
        int y = getGuiTop();

        containerTick();

        extraAreas = ImmutableList.of(new Rect2i(x + background.getWidth(), y + background.getHeight() - 50, 70, 60));

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {

        int x = getGuiLeft();
        int y = getGuiTop();

        background.render(guiGraphics, x, y);

        var title = getTitle();

        guiGraphics.drawString(font, title, Math.round(x + background.getWidth() / 2f - font.width(title) / 2f), y + 6, 0x442000, false);

        GuiGameElement.of(icon).<GuiGameElement.GuiRenderBuilder>at(x + background.getWidth() + 16, y + background.getHeight() - 16, 0)
                .scale(32)
                .rotate(30, 45, 0)
                .lighting(ILightingSettings.DEFAULT_3D)
                .render(guiGraphics);

        int invX = leftPos + 30;
        int invY = topPos + 8 + imageHeight - AllGuiTextures.PLAYER_INVENTORY.getHeight();
        renderPlayerInventory(guiGraphics, invX, invY);

    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }
}
