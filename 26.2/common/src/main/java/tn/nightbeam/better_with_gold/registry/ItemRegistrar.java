package tn.nightbeam.better_with_gold.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Function;

@FunctionalInterface
public interface ItemRegistrar {
	Item register(Identifier id, Function<Item.Properties, Item> factory);
}
