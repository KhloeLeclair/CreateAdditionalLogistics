package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.crate.BottomlessItemHandler;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.PatternReplacement;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import it.unimi.dsi.fastutil.Pair;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

public class PackageEditorBlockEntity extends PackagerBlockEntity implements IHaveHoveringInformation {

    @Nullable
    public AbstractComputerBehaviour computerBehavior;

    protected record ReplacementSettings(int maxStarHeight, int maxRepetitions, boolean allowBackrefs) {
        static ReplacementSettings get() {
            return new ReplacementSettings(
                    Config.Common.maxStarHeight.get(),
                    Config.Common.maxRepetitions.get(),
                    Config.Common.allowBackrefs.get()
            );
        }
    }

    protected boolean hasComputerReplacements;

    @Nullable
    protected BlockPos replacementsSource;
    @Nullable
    protected List<PatternReplacement> _replacements;
    @Nullable
    protected ReplacementSettings _replacementSettings;

    protected boolean hasReplacements;
    @Nullable
    protected String parseError;

    public PackageEditorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        hasComputerReplacements = false;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CALBlockEntityTypes.PACKAGE_EDITOR.get(),
                (be, context) -> be.inventory
        );

        if (Mods.COMPUTERCRAFT.isLoaded()) {
            event.registerBlockEntity(
                    PeripheralCapability.get(),
                    CALBlockEntityTypes.PACKAGE_EDITOR.get(),
                    (be, context) -> be.computerBehavior.getPeripheralCapability()
            );
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // Delete any inherited behavior from the Stock Ticker, and add our own.
        behaviours.removeIf(x -> x instanceof AbstractComputerBehaviour);
        behaviours.add(computerBehavior = CALComputerCraftProxy.behavior(this));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (parseError != null && ! parseError.isEmpty())
            tag.putString("ParseError", parseError);
        tag.putBoolean("HasReplacements", hasReplacements);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        parseError = tag.contains("ParseError", CompoundTag.TAG_STRING) ? tag.getString("ParseError") : null;
        hasReplacements = tag.getBoolean("HasReplacements");
    }

    public String applyRules(String address) {
        var replacements = getReplacements();
        if (!replacements.isEmpty()) {
            for (var replacement : replacements) {
                var pair = replacement.replace(address);
                address = pair.getFirst();
                if (replacement.stop() && pair.getSecond())
                    break;
            }
        }
        return address;
    }

    public void readdressPackage(ItemStack box) {
        var replacements = getReplacements();

        if (!replacements.isEmpty()) {
            String address = PackageItem.getAddress(box);
            address = applyRules(address);
            if (address.isEmpty())
                PackageItem.clearAddress(box);
            else
                PackageItem.addAddress(box, address);
        }
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean supertip = IHaveHoveringInformation.super.addToTooltip(tooltip, isPlayerSneaking);

        if (!hasReplacements) {
            CALLang.translate("package_editor.no_address.title").style(ChatFormatting.GOLD).forGoggles(tooltip);
            var cut = TooltipHelper.cutTextComponent(CALLang.translate("package_editor.no_address").component(), FontHelper.Palette.GRAY_AND_WHITE);
            for(Component cmp : cut)
                CALLang.builder().add(cmp).forGoggles(tooltip);

            return true;
        }

        if (parseError == null)
            return supertip;

        CALLang.translate("package_editor.invalid_pattern.title").style(ChatFormatting.GOLD).forGoggles(tooltip);
        var cut = TooltipHelper.cutTextComponent(CALLang.translate("package_editor.invalid_pattern").component(), FontHelper.Palette.GRAY_AND_WHITE);
        for(Component cmp : cut)
            CreateLang.builder().add(cmp).forGoggles(tooltip);

        Iterator<String> it = parseError.lines().iterator();
        boolean first = true;
        while(it.hasNext()) {
            String line = it.next();

            cut = TooltipHelper.cutStringTextComponent(line, first ? FontHelper.Palette.ofColors(ChatFormatting.WHITE, ChatFormatting.GOLD) : FontHelper.Palette.RED);
            first = false;
            for(Component cmp : cut)
                CreateLang.builder().add(cmp).forGoggles(tooltip);
        }

        return true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (animationTicks > 0 || redstonePowered)
            return false;

        // Don't work if we have bad replacements.
        getReplacements();
        if (parseError != null)
            return false;

        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || targetInv instanceof PackagerItemHandler)
            return false;

        boolean targetIsCreativeCrate = targetInv instanceof BottomlessItemHandler;
        boolean anySpace = false;

        if (simulate)
            box = box.copy();

        readdressPackage(box);

        for(int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack remainder = targetInv.insertItem(slot, box, simulate);
            if (!remainder.isEmpty())
                continue;
            anySpace = true;
            break;
        }

        if (!targetIsCreativeCrate && !anySpace)
            return false;
        if (simulate)
            return true;

        previouslyUnwrapped = box;
        animationInward = true;
        animationTicks = CYCLE;
        notifyUpdate();
        return true;
    }

    @Override
    public void recheckIfLinksPresent() {
    }

    @Override
    public boolean redstoneModeActive() {
        return true;
    }

    public List<PatternReplacement> getReplacements() {
        if (hasComputerReplacements) {
            if (_replacements == null)
                return List.of();
            return _replacements;
        }

        if (_replacements == null || !Objects.equals(_replacementSettings, ReplacementSettings.get()))
            updateReplacements();
        return _replacements;
    }

    public void maybeUpdateReplacements(BlockPos neighbor) {
        var nbe = level == null ? null : level.getBlockEntity(neighbor);
        var is_relevant = nbe instanceof SignBlockEntity || nbe instanceof ClipboardBlockEntity;

        if (is_relevant || neighbor.equals(replacementsSource) || _replacements == null)
            updateReplacements();
    }

    public void setComputerReplacements(@Nullable List<PatternReplacement> replacements) {
        if (replacements == null) {
            if (hasComputerReplacements) {
                hasComputerReplacements = false;
                updateReplacements();
            }
        } else {
            parseError = null;
            hasReplacements = true;
            hasComputerReplacements = true;
            replacementsSource = null;
            _replacements = replacements;
            sendData();
        }
    }

    public void updateReplacements() {
        if (hasComputerReplacements)
            return;

        String lastError = parseError;
        boolean hadReplacements = hasReplacements;

        // TODO: Refactor this to result in an error if there are multiple sources.

        for(Direction side : Iterate.directions) {
            var pos = worldPosition.relative(side);
            var result = getSignOrClipboard(pos);
            if (result == null)
                continue;

            var ex = result.second();
            if (ex != null) {
                replacementsSource = pos;
                _replacements = List.of();
                _replacementSettings = ReplacementSettings.get();
                parseError = ex.getMessage();
                hasReplacements = true;
                if (!Objects.equals(parseError, lastError) || hadReplacements != hasReplacements)
                    sendData();
                return;
            }

            var entries = result.first();
            if (entries == null || entries.isEmpty())
                continue;

            replacementsSource = pos;
            _replacements = entries;
            parseError = null;
            hasReplacements = true;
            if (lastError != null || hadReplacements != hasReplacements)
                sendData();
            return;
        }

        replacementsSource = null;
        _replacements = List.of();
        _replacementSettings = ReplacementSettings.get();
        parseError = null;
        hasReplacements = false;
        if (lastError != null || hadReplacements != hasReplacements)
            sendData();
    }

    @Override
    protected void updateSignAddress() {
        signBasedAddress = "";
    }

    protected boolean isSignOrClipboardAttached(BlockState state, BlockPos pos) {
        if (state.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)) {
            var face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
            if (face == AttachFace.CEILING)
                return pos.relative(Direction.UP).equals(worldPosition);
            if (face == AttachFace.FLOOR)
                return pos.relative(Direction.DOWN).equals(worldPosition);
        }

        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            var opposite = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            return pos.relative(opposite).equals(worldPosition);
        }

        // shrug
        return true;
    }

    @Nullable
    protected Pair<@Nullable List<PatternReplacement>, @Nullable PatternSyntaxException> getSignOrClipboard(BlockPos pos) {
        var entity = level == null ? null : level.getBlockEntity(pos);
        if (entity instanceof SignBlockEntity sign) {
            List<PatternReplacement> result = new ArrayList<>(1);
            for(boolean front : Iterate.trueAndFalse) {
                SignText text = sign.getText(front);
                var messages = text.getMessages(false);
                for(int i = 0; i < messages.length; i += 2) {
                    String regex = messages[i].getString();
                    String replacement = (i + 1 < messages.length) ? messages[i + 1].getString() : "";

                    if (!regex.isEmpty()) {
                        PatternReplacement pattern;
                        try {
                            pattern = PatternReplacement.of(regex, replacement);
                        } catch (PatternSyntaxException ex) {
                            return Pair.of(null, ex);
                        }

                        result.add(pattern);
                    }
                }
            }

            if (!result.isEmpty())
                return Pair.of(result, null);
        }

        if (entity instanceof ClipboardBlockEntity cb && isSignOrClipboardAttached(cb.getBlockState(), pos)) {
            var pages = cb.dataContainer.get(AllDataComponents.CLIPBOARD_PAGES);
            List<PatternReplacement> result = new ArrayList<>();
            if (pages != null) {
                for (var page : pages) {
                    if (page != null && !page.isEmpty()) {
                        for(int i = 0; i < page.size(); i += 2) {
                            var regexEntry = page.get(i);
                            var replacementEntry = (i + 1 < page.size()) ? page.get(i + 1) : null;
                            String regex = regexEntry == null ? null : regexEntry.text.getString();
                            String replacement = replacementEntry == null ? "" : replacementEntry.text.getString();
                            boolean insensitive = regexEntry != null && regexEntry.checked;
                            boolean stop = replacementEntry != null && replacementEntry.checked;

                            if (regex != null && !regex.isEmpty()) {
                                PatternReplacement pattern;
                                try {
                                    pattern = PatternReplacement.of(regex, replacement, stop, insensitive);
                                } catch (PatternSyntaxException ex) {
                                    return Pair.of(null, ex);
                                }

                                result.add(pattern);
                            }
                        }
                    }
                }
            }

            if (!result.isEmpty())
                return Pair.of(result, null);
        }

        return null;
    }

    public void attemptToSend(List<PackagingRequest> queuedRequests) {
        if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0)
            return;
        if (!queuedExitingPackages.isEmpty())
            return;

        // Don't work if we have bad replacements.
        getReplacements();
        if (parseError != null)
            return;

        IItemHandler targetInv = targetInventory.getInventory();
        if (targetInv == null || (targetInv instanceof PackagerItemHandler))
            return;

        attemptToFindPackage(targetInv);
        if (heldBox.isEmpty())
            return;

        readdressPackage(heldBox);
    }

    protected void attemptToFindPackage(IItemHandler targetInv) {
        for(int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack extracted = targetInv.extractItem(slot, 1, true);
            if (extracted.isEmpty() || !PackageItem.isPackage(extracted))
                continue;

            targetInv.extractItem(slot, 1, false);
            heldBox = extracted.copy();
            animationInward = false;
            animationTicks = CYCLE;
            notifyUpdate();
            return;
        }
    }
}
