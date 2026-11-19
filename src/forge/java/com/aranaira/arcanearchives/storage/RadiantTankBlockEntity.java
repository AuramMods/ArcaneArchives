package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public final class RadiantTankBlockEntity extends PortableStorageBlockEntity {
    public static final int BASE_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    private final FluidTank tank = new FluidTank(BASE_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    public RadiantTankBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.RADIANT_TANK_ENTITY.get(), pos, state, FluidHandlerItemStack.FLUID_NBT_KEY, true);
    }

    public FluidTank tank() {
        return tank;
    }

    @Override
    int storedAmount() {
        return tank.getFluidAmount();
    }

    @Override
    int capacityWithMultiplier(int multiplier) {
        return BASE_CAPACITY * multiplier;
    }

    @Override
    void applyCapacity() {
        tank.setCapacity(capacityWithMultiplier(upgrades().multiplier()));
    }

    @Override
    CompoundTag saveContents() {
        return tank.writeToNBT(new CompoundTag());
    }

    @Override
    void loadContents(CompoundTag tag) {
        tank.readFromNBT(tag);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidHandler = LazyOptional.of(() -> tank);
    }
}
