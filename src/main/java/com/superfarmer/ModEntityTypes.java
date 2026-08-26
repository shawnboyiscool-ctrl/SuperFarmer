package com.superfarmer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

public final class ModEntityTypes {
    public static final EntityType<SuperFarmerEntity> SUPER_FARMER = register(
            "super_farmer",
            EntityType.Builder.<SuperFarmerEntity>of(SuperFarmerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SuperFarmer.MOD_ID, name)
        );
        return net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(SUPER_FARMER, Villager.createAttributes());
    }
}
