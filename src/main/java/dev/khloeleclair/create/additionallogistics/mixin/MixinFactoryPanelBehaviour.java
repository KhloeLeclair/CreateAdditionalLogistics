package dev.khloeleclair.create.additionallogistics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.IPromiseLimit;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelBehaviour.class)
public class MixinFactoryPanelBehaviour extends FilteringBehaviour implements IPromiseLimit {

    private static final String PROMISE_LIMIT_KEY = "CAL$PromiseLimit";

    private int promiseLimit = -1;

    public MixinFactoryPanelBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
        super(be, slot);
    }

    public boolean hasPromiseLimit() {
        return promiseLimit >= 0 && Config.Common.enablePromiseLimits.get();
    }

    public int getPromiseLimit() {
        return promiseLimit;
    }

    public void setPromiseLimit(int value) {
        if (value < 0)
            value = -1;
        promiseLimit = value;
    }

    private FactoryPanelBehaviour FPB() {
        // This is safe, because Mixins.
        return (FactoryPanelBehaviour) (Object) this;
    }

    private void writePromiseLimit(CompoundTag nbt) {
        var fpb = FPB();
        if (!fpb.active)
            return;

        String tagName = CreateLang.asId(fpb.slot.name());
        var tag = nbt.getCompound(tagName);

        tag.putInt(PROMISE_LIMIT_KEY, promiseLimit);

        nbt.put(tagName, tag);
    }

    @Inject(
            method = "tryRestock",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lorg/joml/Math;clamp(III)I"
            ),
            cancellable = true
    )
    private void CAL$onTryRestock(CallbackInfo ci, @Local(ordinal = 2) int promised, @Local(ordinal = 5) LocalIntRef amountToOrder) {
        if (!hasPromiseLimit())
            return;

        int limit = getPromiseLimit();
        int amount = amountToOrder.get();
        if (promised + amount > limit)
            amount = limit - promised;

        if (amount <= 0)
            ci.cancel();
        else
            amountToOrder.set(amount);
    }


    @Inject(
            method = "tickRequests",
            at = @At(
                    value = "INVOKE",
                    target = "resetTimer",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void CAL$inTickRequests(CallbackInfo ci) {
        if (!hasPromiseLimit())
            return;

        var fpb = FPB();

        int limit = getPromiseLimit();

        // If we aren't a restocker, the promise value should be multiplied by the output amount.
        if (!fpb.panelBE().restocker)
            limit = limit * fpb.recipeOutput;

        if (limit <= 0 || fpb.getPromised() >= limit)
            ci.cancel();
    }

    @Inject(
            method = "writeSafe",
            at = @At("RETURN")
    )
    private void CAL$onWriteSafe(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci) {
        writePromiseLimit(nbt);
    }

    @Inject(
            method = "write",
            at = @At("RETURN")
    )
    private void CAL$onWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        writePromiseLimit(nbt);
    }

    @Inject(
            method = "read",
            at = @At("RETURN")
    )
    private void CAL$onRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        var fpb = FPB();
        if (!fpb.active)
            return;

        var tag = nbt.getCompound(CreateLang.asId(fpb.slot.name()));
        if (tag.contains(PROMISE_LIMIT_KEY, CompoundTag.TAG_INT))
            this.setPromiseLimit(tag.getInt(PROMISE_LIMIT_KEY));
        else
            this.setPromiseLimit(-1);
    }

}
