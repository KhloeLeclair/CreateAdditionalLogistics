package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesLedgerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;

public class CALItems {

    private static final CreateRegistrate REGISTRATE = CreateAdditionalLogistics.REGISTRATE.get();

    static {
        REGISTRATE.defaultCreativeTab(CreativeModeTabs.SEARCH);
    }

    // Sales Ledger
    public static final ItemEntry<SalesLedgerItem> SALES_LEDGER =
            REGISTRATE.item("sales_ledger", SalesLedgerItem::new)
                    .properties(p -> p.stacksTo(1))
                    .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "createadditionallogistics.sales_ledger"))
                    .register();

    public static void register() { }

}
