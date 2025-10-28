package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VerticalBeltBlockEntity extends KineticBlockEntity {

    public int beltHeight;
    public int index;
    public Direction lastInsert;
    public boolean covered;

    @Nullable
    protected BlockPos controller;
    @Nullable
    protected VerticalBeltInventory inventory;
    @Nullable
    protected IItemHandler itemHandler;

    public VersionedInventoryTrackerBehaviour invVersionTracker;

    @Nullable
    public CompoundTag trackerUpdateTag;

    public VerticalBeltBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);

        controller = BlockPos.ZERO;
        itemHandler = null;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        /*event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CALBlockEntityTypes.VERTICAL_BELT.get(),
                (be, context) -> {
                    if (!be.isRemoved() && be.itemHandler == null)
                        be.initializeItemHandler();
                    return be.itemHandler;
                }
        );*/
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
    }

    public boolean isController() {
        return controller != null && controller.equals(worldPosition);
    }

    public void setController(BlockPos pos) {
        controller = pos;
    }

    public BlockPos getController() {
        return controller == null ? worldPosition : controller;
    }

    @Nullable
    public VerticalBeltBlockEntity getControllerBE() {
        if (controller != null && level != null && level.isLoaded(controller) && level.getBlockEntity(controller) instanceof VerticalBeltBlockEntity be)
            return be;
        return null;
    }

    public float getBeltMovementSpeed() {
        return getSpeed() / 480f;
    }


    @Override
    public void tick() {
        // Initialize vertical belt
        if (beltHeight == 0)
            AbstractVerticalBeltBlock.initBelt(level, worldPosition);

        super.tick();

        if (!(level.getBlockState(worldPosition).getBlock() instanceof VerticalBeltBlock))
            return;

        initializeItemHandler();

        // Move Items
        if (!isController())
            return;

        invalidateRenderBoundingBox();

        getInventory().tick();

        // Unlike normal belts, we don't do entities, so we're done here.
    }

    @Override
    public float calculateStressApplied() {
        if (!isController())
            return 0;
        return super.calculateStressApplied();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        var result = super.createRenderBoundingBox();
        // The controller renders the whole thing, but we only ever need to expand up.
        if (isController())
            result = result.setMaxY(result.getMaxPosition().y + beltHeight + 1);
        return result;
    }

    protected void initializeItemHandler() {
        if (level.isClientSide || itemHandler != null)
            return;
        if (beltHeight == 0 || controller == null)
            return;
        if (!level.isLoaded(controller) || !(level.getBlockEntity(controller) instanceof VerticalBeltBlockEntity vbbe))
            return;

        var inventory = vbbe.getInventory();
        if (inventory == null)
            return;

        itemHandler = new ItemHandlerVerticalBeltSegment(inventory, index);
        invalidateCapabilities();
    }

    public VerticalBeltInventory getInventory() {
        if (!isController()) {
            var controllerBE = getControllerBE();
            if (controllerBE != null)
                return controllerBE.getInventory();
            return null;
        }

        if (inventory == null)
            inventory = new VerticalBeltInventory(this);

        return inventory;
    }

    public void invalidateItemHandler() {
        invalidateCapabilities();
        itemHandler = null;
    }

}
