package tn.nightbeam.better_with_gold.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

@FunctionalInterface
public interface CreativeTabRegistrar {
	CreativeModeTab register(ResourceLocation id, Supplier<CreativeModeTab> factory);
}
