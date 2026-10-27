package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Two tabs: Fellowship (the Warbanner and the hiring essentials) and Gilded Regalia (the gold arsenal). */
public final class ModCreativeTabs {

	public static CreativeModeTab FELLOWSHIP;
	public static CreativeModeTab BETTER_WITH_GOLD;

	private ModCreativeTabs() {
	}

	public static void register(CreativeTabRegistrar registrar) {
		FELLOWSHIP = registrar.register(
				new ResourceLocation(Constants.MOD_ID, "fellowship"),
				() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
						.title(Component.translatable("item_group.better_with_gold.fellowship"))
						.icon(() -> new ItemStack(ModItems.WARBANNER))
						.displayItems((parameters, output) -> {
							output.accept(ModItems.WARBANNER);
							output.accept(ModItems.GOLDEN_FISH);
						})
						.build()
		);
		BETTER_WITH_GOLD = registrar.register(
				new ResourceLocation(Constants.MOD_ID, "better_with_gold"),
				() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
						.title(Component.translatable("item_group.better_with_gold.better_with_gold"))
						.icon(() -> new ItemStack(ModItems.REINFORCED_GOLDEN_ARMOR_CHESTPLATE))
						.displayItems((parameters, output) -> {
							output.accept(ModItems.UPGRADE_TEMPLATE);
							output.accept(ModItems.REINFORCED_GOLDEN_ARMOR_HELMET);
							output.accept(ModItems.REINFORCED_GOLDEN_ARMOR_CHESTPLATE);
							output.accept(ModItems.REINFORCED_GOLDEN_ARMOR_LEGGINGS);
							output.accept(ModItems.REINFORCED_GOLDEN_ARMOR_BOOTS);
							output.accept(ModItems.REINFORCED_GOLDEN_SWORD);
							output.accept(ModItems.REINFORCED_GOLDEN_PICK_AXE);
							output.accept(ModItems.REINFORCED_GOLDEN_AXE);
							output.accept(ModItems.REINFORCED_GOLDEN_SHOVEL);
							output.accept(ModItems.REINFORCED_GOLDEN_HOE);
							output.accept(ModItems.GOLDEN_SCYTHE);
							output.accept(ModItems.REINFORCED_GOLDEN_SCYTHE);
							output.accept(ModItems.GREATER_GOLDEN_AXE);
							output.accept(ModItems.REINFORCED_GREATER_GOLDEN_AXE);
							output.accept(ModItems.GREATER_GOLDEN_HAMMER);
							output.accept(ModItems.REINFORCED_GREATER_GOLDEN_HAMMER);
							output.accept(ModItems.GOLDEN_FISH);
						})
						.build()
		);
	}
}
