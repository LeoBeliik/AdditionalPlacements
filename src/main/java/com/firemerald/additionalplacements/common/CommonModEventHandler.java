package com.firemerald.additionalplacements.common;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.*;
import com.firemerald.additionalplacements.block.interfaces.IPlacementBlock;
import com.firemerald.additionalplacements.datagen.ModelGenerator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEventHandler
{
	@SubscribeEvent
	public static void onBlockRegistry(RegistryEvent.Register<Block> event)
	{
		IForgeRegistry<Block> registry = event.getRegistry();
		final List<Block> created = new LinkedList<>();
		boolean generateSlabs = AdditionalPlacementsMod.COMMON_CONFIG.generateSlabs.get();
		boolean generateStairs = AdditionalPlacementsMod.COMMON_CONFIG.generateStairs.get();
		boolean generateCarpets = AdditionalPlacementsMod.COMMON_CONFIG.generateCarpets.get();
		boolean generatePressurePlates = AdditionalPlacementsMod.COMMON_CONFIG.generatePressurePlates.get();
		boolean generateWeightedPressurePlates = AdditionalPlacementsMod.COMMON_CONFIG.generateWeightedPressurePlates.get();
		List<String> blacklist = AdditionalPlacementsMod.COMMON_CONFIG.blacklist.get();
		registry.getEntries().forEach(entry -> {
			ResourceLocation name = entry.getKey().location();
			Block block = entry.getValue();
			if (block instanceof SlabBlock)
			{
				if (generateSlabs && !((IPlacementBlock<?>) block).hasAdditionalStates() && !blacklist.contains(name.toString()))
					if (block instanceof WeatheringCopper)
						created.add(new VerticalWeatheringSlabBlock<>((SlabBlock & WeatheringCopper) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
					else
						created.add(new VerticalSlabBlock((SlabBlock) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
			}
			else if (block instanceof StairBlock)
			{
				if (generateStairs && !((IPlacementBlock<?>) block).hasAdditionalStates() && !blacklist.contains(name.toString()))
					if (block instanceof WeatheringCopper)
						created.add(new VerticalWeatheringStairBlock<>((StairBlock & WeatheringCopper) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
					else
						created.add(new VerticalStairBlock((StairBlock) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
			}
			else if (block instanceof CarpetBlock)
			{
				if (generateCarpets && !((IPlacementBlock<?>) block).hasAdditionalStates() && !blacklist.contains(name.toString()))
					created.add(new AdditionalCarpetBlock((CarpetBlock) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
			}
			else if (block instanceof PressurePlateBlock)
			{
				if (generatePressurePlates && !((IPlacementBlock<?>) block).hasAdditionalStates() && !blacklist.contains(name.toString()))
					created.add(new AdditionalPressurePlateBlock((PressurePlateBlock) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
			}
			else if (block instanceof WeightedPressurePlateBlock)
			{
				if (generateWeightedPressurePlates && !((IPlacementBlock<?>) block).hasAdditionalStates() && !blacklist.contains(name.toString()))
					created.add(new AdditionalWeightedPressurePlateBlock((WeightedPressurePlateBlock) block).setRegistryName(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()));
			}
		});
		created.forEach(registry::register);
		AdditionalPlacementsMod.dynamicRegistration = true;
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event)
	{
		try
		{
			Class<?> clazz = Class.forName("com.google.common.base.Suppliers$NonSerializableMemoizingSupplier");
			Field delegate = clazz.getDeclaredField("delegate");
			delegate.setAccessible(true);
			Field initialized = clazz.getDeclaredField("initialized");
			initialized.setAccessible(true);
			Field value = clazz.getDeclaredField("value");
			value.setAccessible(true);
			@SuppressWarnings("unchecked")
			com.google.common.base.Supplier<BiMap<Block, Block>> supplier = (com.google.common.base.Supplier<BiMap<Block, Block>>) delegate.get(WeatheringCopper.NEXT_BY_BLOCK);
			if (supplier == null)
			{
				@SuppressWarnings("unchecked")
				BiMap<Block, Block> map = (BiMap<Block, Block>) value.get(WeatheringCopper.NEXT_BY_BLOCK);
				supplier = () -> map;
			}
			final com.google.common.base.Supplier<BiMap<Block, Block>> oldSupplier = supplier;
			com.google.common.base.Supplier<BiMap<Block, Block>> newSupplier = (com.google.common.base.Supplier<BiMap<Block, Block>>) (() -> {
				BiMap<Block, Block> oldMap = oldSupplier.get();
				BiMap<Block, Block> newMap = HashBiMap.create(oldMap);
				oldMap.forEach((b1, b2) -> {
					if (b1 instanceof IPlacementBlock && b2 instanceof IPlacementBlock)
					{
						IPlacementBlock<?> p1 = (IPlacementBlock<?>) b1;
						IPlacementBlock<?> p2 = (IPlacementBlock<?>) b2;
						if (p1.hasAdditionalStates() && p2.hasAdditionalStates()) newMap.put(p1.getOtherBlock(), p2.getOtherBlock());
					}
				});
				return newMap;
			});
			delegate.set(WeatheringCopper.NEXT_BY_BLOCK, newSupplier);
			initialized.setBoolean(WeatheringCopper.NEXT_BY_BLOCK, false);
			value.set(WeatheringCopper.NEXT_BY_BLOCK, null);
			delegate.set(WeatheringCopper.PREVIOUS_BY_BLOCK, (com.google.common.base.Supplier<BiMap<Block, Block>>) (() -> WeatheringCopper.NEXT_BY_BLOCK.get().inverse()));
			initialized.setBoolean(WeatheringCopper.PREVIOUS_BY_BLOCK, false);
			value.set(WeatheringCopper.PREVIOUS_BY_BLOCK, null);
		}
		catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | ClassNotFoundException e)
		{
			AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper maps, copper slabs and stairs will weather into vanilla states. Sorry.", e);
		}
	}
	
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		if (event.includeClient())
		{
			event.getGenerator().addProvider(new ModelGenerator(event.getGenerator(), AdditionalPlacementsMod.MOD_ID, event.getExistingFileHelper()));
		}
	}
}