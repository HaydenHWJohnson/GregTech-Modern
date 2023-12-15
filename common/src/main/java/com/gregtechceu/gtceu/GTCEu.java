package com.gregtechceu.gtceu;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTCEu {
    public static final String MOD_ID = "gtceu";
    public static final String NAME = "GregTechCEu";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    /** Will be available at the Pre-Initialization stage */
    @Getter
    private static boolean highTier;
    private static boolean highTierInitialized;

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, FormattingUtil.toLowerCaseUnder(path));
    }

    public static String appendIdString(String id) {
        return id.indexOf(':') == -1 ? (MOD_ID + ":" + id) : id;
    }

    public static ResourceLocation appendId(String id) {
        int colonIndex = id.indexOf(':');
        String namespace = (colonIndex >= 0) ? id.substring(0, colonIndex) : MOD_ID;
        String path = (colonIndex >= 0) ? id.substring(colonIndex + 1) : id;
        return new ResourceLocation(namespace, path);
    }

    public static boolean isKubeJSLoaded() {
        return LDLib.isModLoaded(GTValues.MODID_KUBEJS);
    }

    public static boolean isCreateLoaded() {
        return LDLib.isModLoaded(GTValues.MODID_CREATE);
    }

    public static boolean isIrisOculusLoaded() {
        return LDLib.isModLoaded(GTValues.MODID_IRIS) || LDLib.isModLoaded(GTValues.MODID_OCULUS);
    }

    public static boolean isSodiumRubidiumEmbeddiumLoaded() {
        return LDLib.isModLoaded(GTValues.MODID_SODIUM) || LDLib.isModLoaded(GTValues.MODID_RUBIDIUM) ||LDLib.isModLoaded(GTValues.MODID_EMBEDDIUM);
    }

    public static boolean isRebornEnergyLoaded() {
        return Platform.isForge() || LDLib.isModLoaded(GTValues.MODID_REBORN_ENERGY);
    }

    public static boolean isAE2Loaded() {
        return LDLib.isModLoaded(GTValues.MODID_APPENG);
    }

    public static boolean isAlmostUnifiedLoaded() {
        return LDLib.isModLoaded(GTValues.MODID_ALMOSTUNIFIED);
    }


    /**
     * Initializes High-Tier. Internal use only, do not attempt to call this.
     */
    @ApiStatus.Internal
    public static void initializeHighTier() {
        if (highTierInitialized) throw new IllegalStateException("High-Tier is already initialized.");
        highTier = ConfigHolder.INSTANCE.machines.highTierContent || AddonFinder.getAddons().stream().anyMatch(IGTAddon::requiresHighTier) || Platform.isDevEnv();
        highTierInitialized = true;

        if (isHighTier()) GTCEu.LOGGER.info("High-Tier is Enabled.");
        else GTCEu.LOGGER.info("High-Tier is Disabled.");
    }
}
