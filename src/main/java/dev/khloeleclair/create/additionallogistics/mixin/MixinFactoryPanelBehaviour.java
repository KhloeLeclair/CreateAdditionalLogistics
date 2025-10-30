package dev.khloeleclair.create.additionallogistics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.utilities.IPromiseLimit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelBehaviour.class)
public abstract class MixinFactoryPanelBehaviour extends FilteringBehaviour implements IPromiseLimit {

    private static final String CAL_PROMISE_LIMIT_KEY = "CAL$PromiseLimit";
    private static final String CAL_ADDITIONAL_STOCK_KEY = "CAL$Stock$Add";
    private static final String CAL_REMAINING_ADDITIONAL_KEY = "CAL$Stock$Rem";

    @Shadow(remap = false)
    public boolean satisfied;
    @Shadow(remap = false)
    private int lastReportedLevelInStorage;

    @Unique
    private int CAL$promiseLimit = -1;
    @Unique
    private int CAL$AdditionalStock = 0;
    @Unique
    private int CAL$RemainingAdditional = 0;

    public MixinFactoryPanelBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be, slot);
    }

    public boolean hasCALPromiseLimit() {
        return CAL$promiseLimit >= 0 && Config.Common.enablePromiseLimits.get();
    }

    public int getCALPromiseLimit() {
        return CAL$promiseLimit;
    }

    public void setCALPromiseLimit(int value) {
        if (value < 0)
            value = -1;
        CAL$promiseLimit = value;
    }

    public int getCALAdditionalStock() { return CAL$AdditionalStock; }

    public boolean hasCALAdditionalStock() { return CAL$AdditionalStock > 0 && Config.Common.enableAdditionalStock.get(); }

    public void setCALAdditionalStock(int value) {
        if (value < 0)
            value = 0;

        if (value == CAL$AdditionalStock)
            return;

        CAL$AdditionalStock = value;

        if (CAL$RemainingAdditional > value)
            CAL$RemainingAdditional = value;
    }

    @Unique
    private FactoryPanelBehaviour FPB() {
        // This is safe, because Mixins.
        return (FactoryPanelBehaviour) (Object) this;
    }

    @Unique
    private void CAL$writeData(CompoundTag nbt) {
        var fpb = FPB();
        if (!fpb.active)
            return;

        String tagName = CreateLang.asId(fpb.slot.name());
        var tag = nbt.getCompound(tagName);

        tag.putInt(CAL_PROMISE_LIMIT_KEY, CAL$promiseLimit);

        tag.putInt(CAL_ADDITIONAL_STOCK_KEY, CAL$AdditionalStock);
        tag.putInt(CAL_REMAINING_ADDITIONAL_KEY, CAL$RemainingAdditional);

        nbt.put(tagName, tag);
    }

    @ModifyVariable(
            method = "tickStorageMonitor",
            at = @At("STORE"),
            name = "inStorage",
            remap = false
    )
    private int CAL$tickStorageMonitor$inStorage(int value) {
        // If the number of items has gone down in the last tick, we should
        // subtract a value from RemainingAdditional to ensure we don't keep
        // requesting more and more items above the base amount.
        if (CAL$RemainingAdditional > 0 && lastReportedLevelInStorage > value) {
            int difference = lastReportedLevelInStorage - value;
            CAL$RemainingAdditional -= difference;
            if (CAL$RemainingAdditional < 0)
                CAL$RemainingAdditional = 0;
        }

        return value;
    }

    @Inject(
            method = "tickStorageMonitor",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onTickStorageMonitor(CallbackInfo ci) {
        if (!satisfied && CAL$RemainingAdditional <= 0 && FPB().panelBE().restocker && hasCALAdditionalStock()) {
            // We're freshly unsatisfied. Set the RemainingAdditional value so we
            // can start using it to determine how much to order.
            CAL$RemainingAdditional = CAL$AdditionalStock;

        } else if (satisfied) {
            // We're satisfied, so clear state.
            CAL$RemainingAdditional = 0;
        }
    }

    @ModifyVariable(
            method = "tickStorageMonitor",
            at = @At("LOAD"),
            name = "demand",
            remap = false
    )
    private int CAL$tickStorageMonitor$getDemand(int original) {
        if (CAL$RemainingAdditional > 0 && original > 0)
            return original + CAL$RemainingAdditional;
        return original;
    }

    @ModifyVariable(
            method = "tryRestock",
            at = @At("LOAD"),
            name = "demand",
            remap = false
    )
    private int CAL$tryRestock$getDemand(int original) {
        if (CAL$RemainingAdditional > 0 && original > 0)
            return original + CAL$RemainingAdditional;
        return original;
    }

    @Inject(
            method = "tryRestock",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lorg/joml/Math;clamp(III)I",
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private void CAL$onTryRestock(
            CallbackInfo ci,
            @Local(ordinal = 1) int inStorage,
            @Local(ordinal = 2) int promised,
            @Local(ordinal = 4) int demand,
            @Local(ordinal = 5) LocalIntRef amountToOrder
    ) {
        if (!hasCALPromiseLimit())
            return;

        // Promise Limit Processing
        int limit = getCALPromiseLimit();
        int amount = amountToOrder.get();
        if (promised + amount > limit)
            amount = limit - promised;

        if (amount <= 0) {
            ci.cancel();
            return;
        } else
            amountToOrder.set(amount);
    }

    @Inject(
            method = "tickRequests",
            at = @At(
                    value = "INVOKE",
                    target = "resetTimer",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private void CAL$inTickRequests(CallbackInfo ci) {
        if (!hasCALPromiseLimit())
            return;

        var fpb = FPB();

        int limit = getCALPromiseLimit();

        // If we aren't a restocker, the promise value should be multiplied by the output amount.
        if (!fpb.panelBE().restocker)
            limit = limit * fpb.recipeOutput;

        if (limit <= 0 || fpb.getPromised() >= limit)
            ci.cancel();
    }

    @Inject(
            method = "writeSafe",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onWriteSafe(CompoundTag nbt, CallbackInfo ci) {
        CAL$writeData(nbt);
    }

    @Inject(
            method = "write",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onWrite(CompoundTag nbt, boolean clientPacket, CallbackInfo ci) {
        CAL$writeData(nbt);
    }

    @Inject(
            method = "read",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onRead(CompoundTag nbt, boolean clientPacket, CallbackInfo ci) {
        var fpb = FPB();
        if (!fpb.active)
            return;

        var tag = nbt.getCompound(CreateLang.asId(fpb.slot.name()));

        if (tag.contains(CAL_PROMISE_LIMIT_KEY, CompoundTag.TAG_INT))
            this.setCALPromiseLimit(tag.getInt(CAL_PROMISE_LIMIT_KEY));
        else
            this.setCALPromiseLimit(-1);

        CAL$AdditionalStock = Mth.clamp(tag.getInt(CAL_ADDITIONAL_STOCK_KEY), 0, 64 * 100 * 20);
        CAL$RemainingAdditional = tag.getInt(CAL_REMAINING_ADDITIONAL_KEY);
    }

}
