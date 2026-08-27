package com.superfarmer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class SuperFarmerRenderer extends VillagerRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            SuperFarmer.MOD_ID,
            "textures/entity/super_farmer.png"
    );

    public SuperFarmerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }
}
