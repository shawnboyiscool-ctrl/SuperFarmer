package com.superfarmer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperFarmer implements ModInitializer {
    public static final String MOD_ID = "superfarmer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntityTypes.initialize();
        ModItems.initialize();
        LOGGER.info("Super Farmer loaded");
    }
}
