package dev.khloeleclair.create.additionallogistics.client.screen;

import com.simibubi.create.content.logistics.BigItemStack;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.data.CustomComponents;
import dev.khloeleclair.create.additionallogistics.common.data.SalesHistoryData;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SalesLedgerScreen extends AbstractSimiScreen {

    public final SalesHistoryData Data;
    public final Map<UUID, String> PlayerNames;

    @Nullable
    private Button btnPrevious;
    @Nullable
    private Button btnNext;

    private String query;

    private int page;
    private int pages;
    private int perPage;

    private final List<SalesHistoryData.Sale> filteredSales;

    public SalesLedgerScreen(ItemStack stack, Map<UUID, String> playerNames) {
        super(stack.getHoverName());
        Data = stack.getOrDefault(CustomComponents.SALES_HISTORY, SalesHistoryData.EMPTY);
        PlayerNames = playerNames;

        query = "";
        page = -1;
        perPage = 10;

        filteredSales = new ArrayList<>();
    }

    private void updateFiltered() {
        filteredSales.clear();

        if (! query.isBlank()) {
            var uuids = Data.getPlayers();

            for (var sale : Data.getSales()) {
                boolean matches = false;
                var name = PlayerNames.get(uuids.get(sale.player()));
                if (name != null && name.contains(query))
                    matches = true;

                if (matches)
                    filteredSales.add(sale);
            }
        } else
            filteredSales.addAll(Data.getSales());

        pages = Math.max(1, Math.ceilDiv(filteredSales.size(), perPage));
    }

    @Override
    protected void init() {

        super.init();

        page = 0;
        resizeElements();
        layoutPage();

    }

    private void resizeElements() {
        // If the screen is smaller than 640x360, we need to compact things.
        perPage = 10;

        if (height < 360)
            perPage = Math.clamp((height - 86) / 22, 2, 10);

        pages = Math.max(1, Math.ceilDiv(filteredSales.size(), perPage));
    }

    private void changePage(int p) {
        if (p < 0)
            p = pages - 1;
        if (p >= pages)
            p = 0;

        if (page == p)
            return;

        page = p;
        layoutPage();
    }

    private void hydrateItems(Map<String, BigItemStack> cache, Collection<Map.Entry<String, Integer>> entries) {
        for(var entry : entries) {
            var stack = cache.get(entry.getKey());
            if (stack == null) {
                var item = Data.getItem(entry.getKey());
                if (item == null)
                    continue;

                stack = new BigItemStack(item.getDefaultInstance(), 0);
                cache.put(entry.getKey(), stack);
            }

            stack.count += entry.getValue();
        }
    }

    private void layoutPage() {
        clearWidgets();

        GridLayout layout = new GridLayout();
        layout.defaultCellSetting().padding(2, 2, 2, 0).alignVerticallyMiddle();
        var helper = layout.createRowHelper(4);

        helper.addChild(new StringWidget(title, font).alignCenter(), 3);
        helper.addChild(new StringWidget(CALLang.text("(WIP)").style(ChatFormatting.GRAY).component(), font), 1);
        helper.addChild(new SpacerElement(50, 10), 4);

        // Sum things up

        Map<String, BigItemStack> payments =  new Object2ObjectArrayMap<>();
        Map<String, BigItemStack> sold =  new Object2ObjectArrayMap<>();

        for(var sale : Data.getSales()) {
            hydrateItems(payments, sale.payment().entrySet());
            hydrateItems(sold, sale.purchase().entrySet());
        }

        helper.addChild(new StringWidget(CALLang.translate("sales.sales", CALLang.number(Data.saleCount()).style(ChatFormatting.GOLD)).component(), font), 4);

        helper.addChild(new SpacerElement(50, 10), 4);

        helper.addChild(new StringWidget(CALLang.text("Income:").component(), font), 4);

        for(var stack : payments.values()) {
            helper.addChild(new StringWidget(CALLang.number(stack.count).text("x ").style(ChatFormatting.GRAY).add(stack.stack.getHoverName()).component(), font), 4);
        }

        helper.addChild(new SpacerElement(50, 10), 4);

        helper.addChild(new StringWidget(CALLang.text("Sold Items:").component(), font), 4);

        for(var stack : sold.values()) {
            helper.addChild(new StringWidget(CALLang.number(stack.count).text("x ").style(ChatFormatting.GRAY).add(stack.stack.getHoverName()).component(), font), 4);
        }

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
        layout.visitWidgets(this::addRenderableWidget);

    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }
}
