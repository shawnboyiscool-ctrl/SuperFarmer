package com.superfarmer;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
    public static final ResourceKey<Item> SUPER_FARMER_SPAWN_EGG_ID = create("super_farmer_spawn_egg");

    public static final Item SUPER_FARMER_SPAWN_EGG = register(
            SUPER_FARMER_SPAWN_EGG_ID,
            SpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntityTypes.SUPER_FARMER)
    );

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SuperFarmer.MOD_ID, name));
    }

    private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(creativeTab -> {
            creativeTab.accept(SUPER_FARMER_SPAWN_EGG);
        });
    }
}
