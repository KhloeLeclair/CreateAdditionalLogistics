package dev.khloeleclair.create.additionallogistics.common.utilities;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import dev.khloeleclair.create.additionallogistics.api.ICurrency;

import java.util.List;
import java.util.Map;

public interface ICustomBlueprintOverlayRenderer {

    void CAL$displayShoppingList(Map<ICurrency, Integer> currency_cost, List<BigItemStack> other_cost, InventorySummary purchased);

}
