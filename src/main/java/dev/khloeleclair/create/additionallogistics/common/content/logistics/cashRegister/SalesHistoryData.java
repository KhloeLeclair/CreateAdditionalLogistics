package dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.logistics.BigItemStack;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public record SalesHistoryData(@Nullable Map<String, ResourceLocation> itemMap, @Nullable Map<String, UUID> playerMap, @Nullable List<Sale> saleList) {

    private static final ResourceLocation ID = CreateAdditionalLogistics.asResource("sales_history");

    public static SalesHistoryData getOrEmpty(ItemStack stack) {
        var result = get(stack);
        return result == null ? EMPTY : result;
    }

    @Nullable
    public static SalesHistoryData get(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null && tag.contains(ID.toString(), Tag.TAG_COMPOUND))
            return decode(tag.getCompound(ID.toString()));
        return null;
    }

    @Nullable
    public static SalesHistoryData decode(CompoundTag tag) {
        var result = CODEC.decode(NbtOps.INSTANCE, tag).result();
        return result.map(Pair::getFirst).orElse(null);
    }

    public void save(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null)
            tag = new CompoundTag();

        var result = CODEC.encode(this, NbtOps.INSTANCE, NbtOps.INSTANCE.empty()).result().orElse(null);
        if (result != null)
            tag.put(ID.toString(), result);
        else
            tag.remove(ID.toString());

        stack.setTag(tag);
    }


    public static final SalesHistoryData EMPTY = new SalesHistoryData(null, null, null);

    public static final Codec<SalesHistoryData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("items").forGetter(SalesHistoryData::itemMap),
                    Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC).fieldOf("players").forGetter(SalesHistoryData::playerMap),
                    Sale.CODEC.listOf().fieldOf("sales").forGetter(SalesHistoryData::saleList)
            ).apply(instance, SalesHistoryData::new));

    public SalesHistoryData withBigSale(UUID player, Collection<BigItemStack> payment, Collection<BigItemStack> purchase) {

        List<Sale> workingSales = saleList == null ? new ArrayList<>() : new ArrayList<>(saleList);
        BiMap<String, ResourceLocation> workingItems = itemMap == null ? HashBiMap.create() : HashBiMap.create(itemMap);
        BiMap<String, UUID> workingPlayers = playerMap == null ? HashBiMap.create() : HashBiMap.create(playerMap);

        var result = convertBigItemList(workingItems, payment);
        var other = convertBigItemList(workingItems, purchase);

        String p;
        if (workingPlayers.containsValue(player))
            p = workingPlayers.inverse().get(player);
        else {
            p = (workingPlayers.size() + 1) + "";
            workingPlayers.put(p, player);
        }

        workingSales.add(new Sale(Instant.now().getEpochSecond(), p, result, other));

        return new SalesHistoryData(workingItems, workingPlayers, workingSales);
    }

    public List<Sale> getSales() {
        return saleList == null ? List.of() : saleList;
    }

    public Map<String, ResourceLocation> getItems() {
        return itemMap == null ? Map.of() : itemMap;
    }

    public Map<String, UUID> getPlayers() {
        return playerMap == null ? Map.of() : playerMap;
    }

    @Nullable
    public List<List<ClipboardEntry>> toPurchaseClipboardEntries() {
        if (saleList == null || saleList.isEmpty())
            return null;

        List<ClipboardEntry> entries = new ArrayList<>();
        Map<String, Integer> purchases = new Object2IntArrayMap<>();

        for(var sale : getSales()) {
            for(var entry : sale.purchase().entrySet()) {
                purchases.put(entry.getKey(), purchases.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        for(var entry : purchases.entrySet()) {
            var item = getItem(entry.getKey());
            if (item == null)
                continue;

            entries.add(new ClipboardEntry(false, Component.empty())
                .displayItem(item.getDefaultInstance(), entry.getValue()));
        }

        if (!entries.isEmpty())
            return List.of(entries);
        return null;
    }

    public SalesHistoryData withSale(UUID player, Collection<ItemStack> payment, Collection<ItemStack> purchase) {

        List<Sale> workingSales = saleList == null ? new ArrayList<>() : new ArrayList<>(saleList);
        BiMap<String, ResourceLocation> workingItems = itemMap == null ? HashBiMap.create() : HashBiMap.create(itemMap);
        BiMap<String, UUID> workingPlayers = playerMap == null ? HashBiMap.create() : HashBiMap.create(playerMap);

        var result = convertItemList(workingItems, payment);
        var other = convertItemList(workingItems, purchase);

        String p;
        if (workingPlayers.containsValue(player))
            p = workingPlayers.inverse().get(player);
        else {
            p = (workingPlayers.size() + 1) + "";
            workingPlayers.put(p, player);
        }

        workingSales.add(new Sale(Instant.now().getEpochSecond(), p, result, other));

        return new SalesHistoryData(workingItems, workingPlayers, workingSales);
    }

    @Nullable
    public MutableComponent getTimeRange(ChatFormatting... date_formats) {
        if (saleList == null || saleList.isEmpty())
            return null;

        var start_instant = saleList.get(0).toInstant();
        var end_instant = saleList.get(saleList.size() - 1).toInstant();

        var where = ZoneId.systemDefault();

        var start = start_instant.atZone(where);
        var end = end_instant.atZone(where);

        boolean same_day = end.toLocalDate().equals(start.toLocalDate());

        return CALLang.translate(
                "sales.date-range",
                Component.literal(start.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))).withStyle(date_formats),
                Component.literal(end.format(same_day ? DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))).withStyle(date_formats)
        ).component();
    }

    @Nullable
    public Sale firstSale() {
        return saleList != null && ! saleList.isEmpty() ? saleList.get(0) : null;
    }

    @Nullable
    public Sale lastSale() {
        return saleList != null && ! saleList.isEmpty() ? saleList.get(saleList.size() - 1) : null;
    }

    public int saleCount() {
        return saleList == null ? 0 : saleList.size();
    }

    @Nullable
    public Item getItem(String id) {
        if (itemMap == null)
            return null;

        ResourceLocation location = itemMap.get(id);
        var optional = BuiltInRegistries.ITEM.getOptional(location);
        return optional.orElse(null);
    }

    @Nullable
    private static String memorizeItem(BiMap<String, ResourceLocation> workingItems, @Nullable Item item) {
        var location = item == null ? null : BuiltInRegistries.ITEM.getKey(item);
        if (location == null)
            return null;

        if (workingItems.containsValue(location))
            return workingItems.inverse().get(location);

        String id = (workingItems.size() + 1) + "";
        workingItems.put(id, location);
        return id;
    }

    @NotNull
    private static Map<String, Integer> convertItemList(BiMap<String, ResourceLocation> workingItems, @Nullable Collection<ItemStack> input) {
        if (input == null || input.isEmpty())
            return Map.of();

        var result = new Object2IntArrayMap<String>();
        for(var stack : input) {
            String id = memorizeItem(workingItems, stack.getItem());
            if (id == null)
                continue;
            int amount = result.getOrDefault(id, 0) + stack.getCount();
            result.put(id, amount);
        }

        return result;
    }

    @NotNull
    private static Map<String, Integer> convertBigItemList(BiMap<String, ResourceLocation> workingItems, @Nullable Collection<BigItemStack> input) {
        if (input == null || input.isEmpty())
            return Map.of();

        var result = new Object2IntArrayMap<String>();
        for(var stack : input) {
            String id = memorizeItem(workingItems, stack.stack.getItem());
            if (id == null)
                continue;
            int amount = result.getOrDefault(id, 0) + stack.count;
            result.put(id, amount);
        }

        return result;
    }

    public record Sale(long timestamp, String player, Map<String, Integer> payment, Map<String, Integer> purchase) {
        public static Codec<Sale> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("t").forGetter(Sale::timestamp),
                        Codec.STRING.fieldOf("p").forGetter(Sale::player),
                        Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("i").forGetter(Sale::payment),
                        Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("o").forGetter(Sale::purchase)
                ).apply(instance, Sale::new));

        public Instant toInstant() {
            return Instant.ofEpochSecond(timestamp);
        }

    }

}
