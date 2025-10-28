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

    @Accessor("active")
    static boolean CAL$getActive() { throw new AssertionError(); }

    @Accessor("active")
    static void CAL$setActive(boolean active) { throw new AssertionError(); }

    @Accessor("noOutput")
    static boolean CAL$getNoOutput() { throw new AssertionError(); }

    @Accessor("noOutput")
    static void CAL$setNoOutput(boolean value) { throw new AssertionError(); }

    @Accessor("results")
    static List<ItemStack> CAL$getResults() { throw new AssertionError(); }

    @Accessor("ingredients")
    static List<Pair<ItemStack, Boolean>> CAL$getIngredients() { throw new AssertionError(); }

    @Accessor("shopContext")
    static void CAL$setShopContext(BlueprintOverlayShopContext context) { throw new AssertionError(); }

    @Invoker
    static void callPrepareCustomOverlay() { throw new AssertionError(); }

    @Invoker
    static boolean callCanAfford(Player player, BigItemStack entry) { throw new AssertionError(); }

}
