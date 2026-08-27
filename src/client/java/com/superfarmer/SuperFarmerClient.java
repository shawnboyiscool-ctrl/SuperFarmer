package com.superfarmer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class SuperFarmerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntityTypes.SUPER_FARMER, SuperFarmerRenderer::new);
    }
}
