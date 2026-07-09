package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class ModCreativeTabs {

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Constants.MOD_ID, "better_with_gold")
	);

	public static CreativeModeTab BETTER_WITH_GOLD;

	private ModCreativeTabs() {
	}

	public static void register(CreativeTabRegistrar registrar) {
		BETTER_WITH_GOLD = registrar.register(
				Identifier.fromNamespaceAndPath(Constants.MOD_ID, "better_with_gold"),
				() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
						.title(Component.translatable("item_group.better_with_gold.better_with_gold"))
						.icon(() -> new ItemStack(ModItems.REINFORCED_GOLDEN_PICK_AXE))
						.build()
		);
	}

	public static void addItems(Consumer<ItemStack> output) {
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_PICK_AXE));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_AXE));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_SHOVEL));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_HOE));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_SWORD));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_ARMOR_HELMET));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_ARMOR_CHESTPLATE));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_ARMOR_LEGGINGS));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_ARMOR_BOOTS));
		output.accept(new ItemStack(ModItems.GOLDEN_SCYTHE));
		output.accept(new ItemStack(ModItems.GOLDEN_FISH));
		output.accept(new ItemStack(ModItems.GREATER_GOLDEN_AXE));
		output.accept(new ItemStack(ModItems.GREATER_GOLDEN_HAMMER));
		output.accept(new ItemStack(ModItems.REINFORCED_GOLDEN_SCYTHE));
		output.accept(new ItemStack(ModItems.REINFORCED_GREATER_GOLDEN_AXE));
		output.accept(new ItemStack(ModItems.REINFORCED_GREATER_GOLDEN_HAMMER));
	}
}
