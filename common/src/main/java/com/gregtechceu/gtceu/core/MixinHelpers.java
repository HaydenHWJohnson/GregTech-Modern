package com.gregtechceu.gtceu.core;

import com.google.common.collect.ImmutableMap;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorage;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipes;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.BlockBehaviourAccessor;
import com.gregtechceu.gtceu.data.pack.GTDynamicDataPack;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MixinHelpers {

    public static void addMaterialBlockTags(Map<ResourceLocation, List<TagLoader.EntryWithSource>> tagMap, TagPrefix prefix, Map<Material, ? extends BlockEntry<? extends Block>> map) {
        // Add tool tags
        if (!prefix.miningToolTag().isEmpty()) {
            map.forEach((material, block) -> {
                tagMap.computeIfAbsent(CustomTags.TOOL_TIERS[material.getBlockHarvestLevel()].location(), path -> new ArrayList<>())
                        .add(new TagLoader.EntryWithSource(TagEntry.element(block.getId()), GTValues.CUSTOM_TAG_SOURCE));

                var entry = new TagLoader.EntryWithSource(TagEntry.element(block.getId()), GTValues.CUSTOM_TAG_SOURCE);
                if (material.hasProperty(PropertyKey.WOOD)) {
                    tagMap.computeIfAbsent(BlockTags.MINEABLE_WITH_AXE.location(), path -> new ArrayList<>()).add(entry);
                } else {
                    for (var tag : prefix.miningToolTag()) {
                        tagMap.computeIfAbsent(tag.location(), path -> new ArrayList<>()).add(entry);
                    }
                    if (!ConfigHolder.INSTANCE.machines.requireGTToolsForBlocks) {
                        tagMap.computeIfAbsent(BlockTags.MINEABLE_WITH_PICKAXE.location(), path -> new ArrayList<>()).add(entry);
                    }
                }
            });
        }
        // Copy item tags to blocks
        map.forEach((material, block) -> {
            for (TagKey<Block> blockTag : prefix.getAllBlockTags(material)) {
                tagMap.computeIfAbsent(blockTag.location(), path -> new ArrayList<>())
                        .add(new TagLoader.EntryWithSource(TagEntry.element(block.getId()), GTValues.CUSTOM_TAG_SOURCE));
            }
        });
    }

    public static void addMaterialBlockLootTables(ImmutableMap.Builder<ResourceLocation, LootTable> lootTables, TagPrefix prefix, Map<Material, ? extends BlockEntry<? extends Block>> map) {
        map.forEach((material, blockEntry) -> {
            ResourceLocation lootTableId = new ResourceLocation(blockEntry.getId().getNamespace(), "blocks/" + blockEntry.getId().getPath());
            ((BlockBehaviourAccessor)blockEntry.get()).setDrops(lootTableId);
            lootTables.put(lootTableId, BlockLoot.createSingleItemTable(blockEntry.get()).setParamSet(LootContextParamSets.BLOCK).build());
        });
    }

    @ExpectPlatform
    public static void addFluidTexture(Material material, FluidStorage.FluidEntry value) {
        throw new AssertionError();
    }

    public static List<PackResources> addDynamicDataPack(Collection<PackResources> packs) {
        List<PackResources> packResources = new ArrayList<>(packs);
        // Clear old data
        GTDynamicDataPack.clearServer();

        // Register recipes & unification data again
        long startTime = System.currentTimeMillis();
        ChemicalHelper.reinitializeUnification();
        GTRecipes.recipeAddition(GTDynamicDataPack::addRecipe);
        GTCEu.LOGGER.info("GregTech Recipe loading took {}ms", System.currentTimeMillis() - startTime);

        // Load the data
        packResources.add(new GTDynamicDataPack("gtceu:dynamic_data", AddonFinder.getAddons().stream().map(IGTAddon::addonModId).collect(Collectors.toSet())));
        return packResources;
    }

    public static List<PackResources> addDynamicResourcePack(Collection<PackResources> packs) {
        List<PackResources> packResources = new ArrayList<>(packs);
        // Clear old data
        GTDynamicResourcePack.clearClient();

        // Load the data
        packResources.add(new GTDynamicResourcePack("gtceu:dynamic_assets", AddonFinder.getAddons().stream().map(IGTAddon::addonModId).collect(Collectors.toSet())));
        return packResources;
    }

    /**
     * register fluid models for materials
     */
    public static void registerMaterialFluidModels() {
        for (var material : GTRegistries.MATERIALS) {
            var fluidProperty = material.getProperty(PropertyKey.FLUID);
            if (fluidProperty == null) continue;

            for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                FluidStorage storage = fluidProperty.getStorage();
                Fluid fluid = storage.get(key);
                if (fluid instanceof GTFluid gtFluid) {
                    FluidStack testFor = FluidStack.create(gtFluid, FluidHelper.getBucket());
                    GTDynamicResourcePack.addItemModel(
                            Registry.ITEM.getKey(gtFluid.getBucket()),
                            new DelegatedModel(GTCEu.id("item/bucket/" + (FluidHelper.isLighterThanAir(testFor) ? "bucket_gas" : "bucket")))
                    );
                }
            }
        }
    }

    // unused on purpose. Do not call, will destroy ram usage.
    public static void initializeDynamicTextures() {
        //MaterialBlockRenderer.initTextures();
        //TagPrefixItemRenderer.initTextures();
    }
}
