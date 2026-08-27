package com.superfarmer;

import net.minecraft.core.BlockPos;
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

    public SuperFarmerEntity(EntityType<? extends SuperFarmerEntity> entityType, Level level) {
        super(entityType, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FARMER_HOE));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FARMER_HOE));
        }

        if (workCooldown > 0) workCooldown--;

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

        level().setBlock(pos, state.getBlock().defaultBlockState(), 3);

        if (!insertIntoNearbyHopper(harvested.copy())) {
            net.minecraft.world.level.block.Block.popResource(level(), pos.above(), harvested);
        }
    }

    private boolean insertIntoNearbyHopper(ItemStack stack) {
        BlockPos origin = blockPosition();

        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level().getBlockState(pos).is(Blocks.HOPPER)) continue;

                    BlockEntity blockEntity = level().getBlockEntity(pos);
                    if (!(blockEntity instanceof Container container)) continue;

                    if (insertIntoContainer(container, stack)) {
                        blockEntity.setChanged();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean insertIntoContainer(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (existing.isEmpty()) {
                container.setItem(slot, stack.copy());
                stack.setCount(0);
                return true;
            }

            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int room = existing.getMaxStackSize() - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                container.setItem(slot, existing);

                if (stack.isEmpty()) return true;
            }
        }
        return stack.isEmpty();
    }
}
