package com.superfarmer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SuperFarmerEntity extends Villager {
    private int workCooldown = 0;
    private BlockPos targetCrop;
    private BlockPos targetHopper;
    private ItemStack carriedProduce = ItemStack.EMPTY;

    public SuperFarmerEntity(EntityType<? extends SuperFarmerEntity> entityType, Level level) {
        super(entityType, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FARMER_HOE));
    }

    /** Disable the normal villager Brain so this entity never uses composters. */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        // Our own farming/delivery logic below controls this mob.
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FARMER_HOE));
        }

        // Show harvested produce in the off hand while the farmer is delivering it.
        if (!carriedProduce.isEmpty()) {
            setItemSlot(EquipmentSlot.OFFHAND, carriedProduce.copy());
        } else if (!getOffhandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        if (workCooldown > 0) workCooldown--;

        // DELIVERY MODE: once crops are harvested, walk them to a hopper first.
        if (!carriedProduce.isEmpty()) {
            tickDelivery();
            return;
        }

        // FARMING MODE: walk to mature crops and harvest them.
        if (targetCrop != null) {
            BlockState state = level().getBlockState(targetCrop);
            if (!isWantedMatureCrop(state)) {
                targetCrop = null;
            } else {
                double distance = distanceToSqr(targetCrop.getX() + 0.5, targetCrop.getY() + 0.5, targetCrop.getZ() + 0.5);
                if (distance <= 4.0) {
                    harvest(targetCrop, state);
                    targetCrop = null;
                    workCooldown = 10;
                } else if (tickCount % 5 == 0) {
                    getNavigation().moveTo(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5, 1.15);
                }
                return;
            }
        }

        if (workCooldown == 0 && tickCount % 10 == 0) {
            targetCrop = findNearestCrop(16);
            if (targetCrop != null) {
                getNavigation().moveTo(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5, 1.15);
            }
        }
    }

    private void tickDelivery() {
        if (targetHopper == null || !isUsableHopper(targetHopper, carriedProduce)) {
            targetHopper = findNearestUsableHopper(24, carriedProduce);
            getNavigation().stop();
        }

        // No hopper with room yet: keep the produce instead of composting/dropping it.
        if (targetHopper == null) {
            return;
        }

        double distance = distanceToSqr(
                targetHopper.getX() + 0.5,
                targetHopper.getY() + 0.5,
                targetHopper.getZ() + 0.5
        );

        if (distance <= 4.0) {
            BlockEntity blockEntity = level().getBlockEntity(targetHopper);
            if (blockEntity instanceof Container container) {
                insertIntoContainer(container, carriedProduce);
                blockEntity.setChanged();
            }

            if (carriedProduce.isEmpty()) {
                targetHopper = null;
                getNavigation().stop();
                workCooldown = 10;
            } else {
                // This hopper filled while we were walking to it; find another.
                targetHopper = null;
            }
            return;
        }

        if (tickCount % 5 == 0) {
            getNavigation().moveTo(
                    targetHopper.getX() + 0.5,
                    targetHopper.getY(),
                    targetHopper.getZ() + 0.5,
                    1.2
            );
        }
    }

    private BlockPos findNearestCrop(int radius) {
        BlockPos origin = blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level().getBlockState(pos);
                    if (isWantedMatureCrop(state)) {
                        double distance = pos.distSqr(origin);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = pos.immutable();
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isWantedMatureCrop(BlockState state) {
        if (state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)) {
            return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
        }
        return false;
    }

    private void harvest(BlockPos pos, BlockState state) {
        if (!isWantedMatureCrop(state)) return;

        ItemStack harvested;
        if (state.is(Blocks.CARROTS)) {
            harvested = new ItemStack(net.minecraft.world.item.Items.CARROT, 3);
        } else {
            harvested = new ItemStack(net.minecraft.world.item.Items.POTATO, 3);
        }

        // Replant instantly by resetting the crop to age 0.
        level().setBlock(pos, state.getBlock().defaultBlockState(), 3);

        // Carry the produce physically to a hopper.
        carriedProduce = harvested;
        targetHopper = findNearestUsableHopper(24, carriedProduce);
        if (targetHopper != null) {
            getNavigation().moveTo(
                    targetHopper.getX() + 0.5,
                    targetHopper.getY(),
                    targetHopper.getZ() + 0.5,
                    1.2
            );
        }
    }

    private BlockPos findNearestUsableHopper(int radius, ItemStack stack) {
        BlockPos origin = blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!isUsableHopper(pos, stack)) continue;

                    double distance = pos.distSqr(origin);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = pos.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isUsableHopper(BlockPos pos, ItemStack stack) {
        if (!level().getBlockState(pos).is(Blocks.HOPPER)) return false;
        BlockEntity blockEntity = level().getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) return false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < Math.min(existing.getMaxStackSize(), container.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private void insertIntoContainer(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (existing.isEmpty()) {
                int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), container.getMaxStackSize()));
                ItemStack inserted = stack.copy();
                inserted.setCount(moved);
                container.setItem(slot, inserted);
                stack.shrink(moved);
                continue;
            }

            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int max = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
                int room = max - existing.getCount();
                if (room <= 0) continue;

                int moved = Math.min(room, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                container.setItem(slot, existing);
            }
        }
    }
}
