package com.firemerald.additionalplacements.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.*;
import com.firemerald.additionalplacements.block.interfaces.IPlacementBlock;
import com.firemerald.additionalplacements.datagen.ModelGenerator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.*;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEventHandler
{
	@SubscribeEvent
	public static void onBlockRegistry(RegisterEvent event)
	{
		if (event.getForgeRegistry() != null && ForgeRegistries.BLOCKS.getRegistryKey().equals(event.getForgeRegistry().getRegistryKey()))
		{
			IForgeRegistry<Block> registry = event.getForgeRegistry();
			boolean generateSlabs = AdditionalPlacementsMod.COMMON_CONFIG.generateSlabs.get();
			boolean generateStairs = AdditionalPlacementsMod.COMMON_CONFIG.generateStairs.get();
			boolean generateCarpets = AdditionalPlacementsMod.COMMON_CONFIG.generateCarpets.get();
			boolean generatePressurePlates = AdditionalPlacementsMod.COMMON_CONFIG.generatePressurePlates.get();
			boolean generateWeightedPressurePlates = AdditionalPlacementsMod.COMMON_CONFIG.generateWeightedPressurePlates.get();
			List<Pair<ResourceLocation, Block>> created = new ArrayList<>();
			registry.getEntries().forEach(entry -> {
				ResourceLocation name = entry.getKey().location();
				Block block = entry.getValue();
				if (block instanceof SlabBlock)
				{
					if (generateSlabs) tryAdd((SlabBlock) block, name, VerticalSlabBlock::of, created);
				}
				else if (block instanceof StairBlock)
				{
					if (generateStairs) tryAdd((StairBlock) block, name, VerticalStairBlock::of, created);
				}
				else if (block instanceof CarpetBlock)
				{
					if (generateCarpets) tryAdd((CarpetBlock) block, name, AdditionalCarpetBlock::of, created);
				}
				else if (block instanceof PressurePlateBlock)
				{
					if (generatePressurePlates) tryAdd((PressurePlateBlock) block, name, AdditionalPressurePlateBlock::of, created);
				}
				else if (block instanceof WeightedPressurePlateBlock)
				{
					if (generateWeightedPressurePlates) tryAdd((WeightedPressurePlateBlock) block, name, AdditionalWeightedPressurePlateBlock::of, created);
				}
			});
			created.forEach(pair -> registry.register(pair.getLeft(), pair.getRight()));
			AdditionalPlacementsMod.dynamicRegistration = true;
		}
	}

	private static <T extends Block, U extends AdditionalPlacementBlock<T>> void tryAdd(T block, ResourceLocation name, Function<T, U> construct, List<Pair<ResourceLocation, Block>> list)
	{
		if (!((IPlacementBlock<?>) block).hasAdditionalStates() && AdditionalPlacementsMod.COMMON_CONFIG.isValidForGeneration(name))
			list.add(Pair.of(new ResourceLocation(AdditionalPlacementsMod.MOD_ID, name.getNamespace() + "." + name.getPath()), construct.apply(block)));
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
			Function<BiMap<Block, Block>, BiMap<Block, Block>> withAdditionalStates = oldMap -> {
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
			};
			try
			{
				modifyMap(WeatheringCopper.NEXT_BY_BLOCK, WeatheringCopper.PREVIOUS_BY_BLOCK, withAdditionalStates, delegate, initialized, value);
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper maps, copper slabs and stairs will weather into vanilla states. Sorry.", e);
			}
			try
			{
				modifyMap(HoneycombItem.WAXABLES, HoneycombItem.WAX_OFF_BY_BLOCK, withAdditionalStates, delegate, initialized, value);
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper maps, copper slabs and stairs will weather into vanilla states. Sorry.", e);
			}
		}
		catch (ClassNotFoundException | NoSuchFieldException | SecurityException e)
		{
			AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper and HoneycombItem maps, copper slabs and stairs will weather into vanilla states and cannot be waxed. Sorry.", e);
		}
	}

	public static <T, U> void modifyMap(Supplier<BiMap<T, U>> forwardMemoized, Supplier<BiMap<U, T>> backwardMemoized, Function<BiMap<T, U>, BiMap<T, U>> modify, Field delegate, Field initialized, Field value) throws IllegalArgumentException, IllegalAccessException
	{
		@SuppressWarnings("unchecked")
		com.google.common.base.Supplier<BiMap<T, U>> supplier = (com.google.common.base.Supplier<BiMap<T, U>>) delegate.get(forwardMemoized);
		if (supplier == null)
		{
			@SuppressWarnings("unchecked")
			BiMap<T, U> map = (BiMap<T, U>) value.get(forwardMemoized);
			supplier = () -> map;
		}
		final com.google.common.base.Supplier<BiMap<T, U>> oldSupplier = supplier;
		delegate.set(forwardMemoized, (com.google.common.base.Supplier<BiMap<T, U>>) () -> modify.apply(oldSupplier.get()));
		initialized.setBoolean(forwardMemoized, false);
		value.set(forwardMemoized, null);
		delegate.set(backwardMemoized, (com.google.common.base.Supplier<BiMap<U, T>>) () -> forwardMemoized.get().inverse());
		initialized.setBoolean(backwardMemoized, false);
		value.set(backwardMemoized, null);
	}

	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		if (event.includeClient())
		{
			event.getGenerator().addProvider(true, new ModelGenerator(event.getGenerator(), AdditionalPlacementsMod.MOD_ID, event.getExistingFileHelper()));
		}
	}

	public static boolean doubleslabsLoaded;

	@SubscribeEvent
	public static void onFMLCommonSetup(FMLCommonSetupEvent event)
	{
		doubleslabsLoaded = ModList.get().isLoaded("doubleslabs");
	}
}