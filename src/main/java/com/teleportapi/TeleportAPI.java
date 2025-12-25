package com.teleportapi;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TeleportAPI.MOD_ID)
public class TeleportAPI {

    public static final String MOD_ID = "teleportapi";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TeleportAPI() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(this::commonSetup);

        LOGGER.info("TeleportAPI loading!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TeleportAPI initialized!");
    }
}
