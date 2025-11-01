package dev.khloeleclair.create.additionallogistics.common.utilities;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.createmod.catnip.data.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RecipeHelper {

    public static RecipeCache getCache() { return SidedCache.getCache(RecipeCache.class); }

    public static void onRecipesUpdated(RecipesUpdatedEvent ignoredEvent) {
        SidedCache.runOnEachCache(RecipeCache.class, RecipeCache::clearCache);
    }

    @Nullable
    public static SimpleCurrency getCompactingCurrency(Item input) {
        return getCache().getCompactingCurrency(input);
    }

    public static class RecipeCache extends SidedCache {

        private final Map<Item, Optional<SimpleCurrency>> compactingCurrencies;

        public RecipeCache(boolean isServer) {
            super(isServer);
            compactingCurrencies = new Object2ObjectOpenHashMap<>();
        }

        public void clearCache() {
            synchronized (compactingCurrencies) {
                compactingCurrencies.clear();
            }
        }

        @Nullable
        public SimpleCurrency getCompactingCurrency(Item item) {
            Optional<SimpleCurrency> cached;
            synchronized (compactingCurrencies) {
                cached = compactingCurrencies.get(item);
                if (cached != null)
                    return cached.orElse(null);
            }

            @Nullable
            SimpleCurrency currency;
            try {
                currency = getCompactingCurrencyImpl(item);
            } catch(Exception ex) {
                CreateAdditionalLogistics.LOGGER.warn("Error while determining automatic compacting currency for item {}", item);
                currency = null;
            }

            cached = Optional.ofNullable(currency);
            synchronized (compactingCurrencies) {
                if (currency == null)
                    compactingCurrencies.put(item, cached);
                else
                    for(var i : currency.getItems())
                        compactingCurrencies.put(i, cached);
            }

            return currency;
        }

        @Nullable
        private SimpleCurrency getCompactingCurrencyImpl(Item item) {
            final var level = getLevel();
            if (level == null)
                return null;

            List<Pair<Item, Integer>> decompression = new ArrayList<>();
            List<Pair<Item, Integer>> compression = new ArrayList<>();

            HashSet<Item> visited = new HashSet<>();
            visited.add(item);

            // Scan down first.
            Item current = item;
            while(true) {
                ItemStack result = getUncompressResult(level, current.getDefaultInstance());
                if (result.isEmpty())
                    break;

                int count = result.getCount();
                // We only support 2x2 and 3x3
                if (count != 4 && count != 9)
                    break;

                // Loop detection.
                if (!visited.add(result.getItem()))
                    break;

                // Detect if there's a reciprocal recipe
                var compressionResult = getCompressionResult(level, result, count == 4 ? 2 : 3);

                Item finalCurrent = current;
                if (compressionResult.stream().noneMatch(x -> x.is(finalCurrent) && x.getCount() == 1))
                    break;

                // We got here, so there's a conversion.
                current = result.getItem();

                decompression.add(Pair.of(current, count));
            }

            // Now, scan up.
            List<Item> frontier = new LinkedList<>();
            frontier.add(item);

            while(!frontier.isEmpty()) {
                current = frontier.remove(0);
                for(int size = 2; size <= 3; size++) {
                    for (var result : getCompressionResult(level, current.getDefaultInstance(), size)) {
                        // If it produced anything we've seen before, or more than 1 item, we don't want it.
                        if (result.isEmpty() || result.getCount() != 1 || !visited.add(result.getItem()))
                            continue;

                        // Check for a reciprocal recipe.
                        var uncompressResult = getUncompressResult(level, result);
                        if (uncompressResult.isEmpty() || uncompressResult.getCount() != (size == 2 ? 4 : 9) || !uncompressResult.is(current))
                            continue;

                        // We have a match.
                        frontier.add(result.getItem());
                        compression.add(Pair.of(result.getItem(), size == 2 ? 4 : 9));
                    }
                }
            }

            if (decompression.isEmpty() && compression.isEmpty())
                return null;

            // Generate a key based on the key of the lowest item.
            @Nullable ResourceKey<Item> key;
            if (decompression.isEmpty())
                key = item.builtInRegistryHolder().key();
            else
                key = decompression.get(decompression.size() - 1).getFirst().builtInRegistryHolder().key();

            ResourceLocation id = key == null ? null : key.location();
            if (id == null)
                return null;

            SimpleCurrency currency = new SimpleCurrency(new ResourceLocation(id.getNamespace(), "generated/" + id.getPath()));
            long value = 1;

            // First, decompression items.
            for(int i = decompression.size() - 1; i >= 0; i--) {
                var entry = decompression.get(i);
                currency.addItem(entry.getFirst(), value);
                value *= entry.getSecond();
            }

            // Now, add the base item
            currency.addItem(item, value);

            // Now, the compression items
            for(var entry : compression) {
                value *= entry.getSecond();
                currency.addItem(entry.getFirst(), value);
            }

            return currency;
        }

        private static CraftingContainer getContainerOf(int width, int height, ItemStack item) {
            var inv = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(Player player, int i) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(Player player) {
                    return false;
                }
            }, width, height);

            for(int slot = 0; slot < inv.getContainerSize(); slot++)
                inv.setItem(slot, item.copyWithCount(1));

            return inv;
        }

        private static List<ItemStack> getCompressionResult(Level level, ItemStack input, int size) {

            CraftingContainer inputGrid = getContainerOf(size, size, input);

            var recipes = safeGetRecipesFor(RecipeType.CRAFTING, inputGrid, level);
            if (recipes.isEmpty())
                return List.of();

            return recipes.stream().map(x -> x.getResultItem(level.registryAccess())).toList();
        }

        private static ItemStack getUncompressResult(Level level, ItemStack input) {
            // We're looking for a recipe that puts the input stack in a 1x1 grid.
            var inputGrid = getContainerOf(1, 1, input);

            var recipes = safeGetRecipesFor(RecipeType.CRAFTING, inputGrid, level);
            if (recipes.size() != 1)
                return ItemStack.EMPTY;

            return recipes.get(0).getResultItem(level.registryAccess());
        }

    }

    public static <C extends Container, T extends Recipe<C>> List<T> safeGetRecipesFor(RecipeType<T> recipeType, C inventory, Level level) {
        try {
            return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
        } catch (Exception e) {
            CreateAdditionalLogistics.LOGGER.error("Error while getting recipe: ", e);
            return Collections.emptyList();
        }
    }

}
