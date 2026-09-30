package tn.nightbeam.better_with_gold.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

@FunctionalInterface
public interface CreativeTabRegistrar {
	CreativeModeTab register(Identifier id, Supplier<CreativeModeTab> factory);
}
