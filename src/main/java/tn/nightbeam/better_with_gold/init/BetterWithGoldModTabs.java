/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.nightbeam.better_with_gold.init;

import tn.nightbeam.better_with_gold.BetterWithGoldMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BetterWithGoldModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BetterWithGoldMod.MODID);
	public static final RegistryObject<CreativeModeTab> BETTER_WITH_GOLD = REGISTRY.register("better_with_gold",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.better_with_gold.better_with_gold")).icon(() -> new ItemStack(BetterWithGoldModItems.REINFORCED_GOLDEN_PICK_AXE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_PICK_AXE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_AXE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_SHOVEL.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_HOE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_SWORD.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_ARMOR_HELMET.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_ARMOR_CHESTPLATE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_ARMOR_LEGGINGS.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_ARMOR_BOOTS.get());
				tabData.accept(BetterWithGoldModItems.GOLDEN_SCYTHE.get());
				tabData.accept(BetterWithGoldModItems.GOLDEN_FISH.get());
				tabData.accept(BetterWithGoldModItems.GREATER_GOLDEN_AXE.get());
				tabData.accept(BetterWithGoldModItems.GREATER_GOLDEN_HAMMER.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GOLDEN_SCYTHE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GREATER_GOLDEN_AXE.get());
				tabData.accept(BetterWithGoldModItems.REINFORCED_GREATER_GOLDEN_HAMMER.get());
			}).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(BetterWithGoldModItems.UPGRADE_TEMPLATE.get());
		}
	}
}