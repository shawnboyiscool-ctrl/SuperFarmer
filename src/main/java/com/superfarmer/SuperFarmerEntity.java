package com.superfarmer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SuperFarmerEntity extends Villager {
    private int workCooldown = 0;
    private BlockPos targetCrop;

    public SuperFarmerEntity(EntityType<? extends SuperFarmerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (workCooldown > 0) workCooldown--;

        if (targetCrop != null) {
            BlockState state = level().getBlockState(targetCrop);
            if (!isWantedMatureCrop(state)) {
                targetCrop = null;
            } else {
                double distance = distanceToSqr(targetCrop.getX() + 0.5, targetCrop.getY() + 0.5, targetCrop.getZ() + 0.5);
                if (distance <= 3.0) {
                    harvest(targetCrop, state);
                    targetCrop = null;
                    workCooldown = 20;
                } else if (tickCount % 10 == 0) {
                    getNavigation().moveTo(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5, 1.15);
                }
                return;
            }
        }

        if (workCooldown == 0 && tickCount % 20 == 0) {
            targetCrop = findNearestCrop(10);
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
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level().getBlockState(pos);
                    if (isWantedMatureCrop(state)) {
                        double distance = pos.distSqr(origin);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearest = pos;
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

        // Drop the normal vanilla crop loot. Hoppers nearby can collect these drops normally.
        net.minecraft.world.level.block.Block.dropResources(state, level(), pos);

        // Replant the crop immediately.
        level().setBlock(pos, state.getBlock().defaultBlockState(), 2);
    }
}
