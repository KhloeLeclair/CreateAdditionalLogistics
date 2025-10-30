package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.tableCloth.BlueprintOverlayShopContext;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BlueprintOverlayRenderer.class)
public interface IBlueprintOverlayRendererAccessor {

    @Accessor(value = "active", remap = false)
    static boolean CAL$getActive() { throw new AssertionError(); }

    @Accessor(value = "active", remap = false)
    static void CAL$setActive(boolean active) { throw new AssertionError(); }

    @Accessor(value = "noOutput", remap = false)
    static boolean CAL$getNoOutput() { throw new AssertionError(); }

    @Accessor(value = "noOutput", remap = false)
    static void CAL$setNoOutput(boolean value) { throw new AssertionError(); }

    @Accessor(value = "results", remap = false)
    static List<ItemStack> CAL$getResults() { throw new AssertionError(); }

    @Accessor(value = "ingredients", remap = false)
    static List<Pair<ItemStack, Boolean>> CAL$getIngredients() { throw new AssertionError(); }

    @Accessor(value = "shopContext", remap = false)
    static void CAL$setShopContext(BlueprintOverlayShopContext context) { throw new AssertionError(); }

    @Invoker(remap = false)
    static void callPrepareCustomOverlay() { throw new AssertionError(); }

    @Invoker(remap = false)
    static boolean callCanAfford(Player player, BigItemStack entry) { throw new AssertionError(); }

}
