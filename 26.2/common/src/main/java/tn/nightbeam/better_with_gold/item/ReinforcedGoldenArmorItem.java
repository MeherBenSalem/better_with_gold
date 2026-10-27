package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public abstract class ReinforcedGoldenArmorItem extends Item {

	private static final ArmorMaterial GOLD = ArmorMaterials.GOLD;
	private static final ArmorMaterial REINFORCED = new ArmorMaterial(GOLD.durability(), GOLD.defense(),
			GOLD.enchantmentValue(), GOLD.equipSound(), GOLD.toughness(), GOLD.knockbackResistance(),
			GOLD.repairIngredient(), ResourceKey.create(EquipmentAssets.ROOT_ID,
					Identifier.fromNamespaceAndPath("better_with_gold", "reinforced_golden_armor")));

	protected ReinforcedGoldenArmorItem(Item.Properties properties, ArmorType type) {
		super(properties.humanoidArmor(REINFORCED, type));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("item.better_with_gold.reinforced_golden_armor_helmet.description_0"));
	}

	public static class Helmet extends ReinforcedGoldenArmorItem {
		public Helmet(Item.Properties properties) {
			super(properties, ArmorType.HELMET);
		}
	}

	public static class Chestplate extends ReinforcedGoldenArmorItem {
		public Chestplate(Item.Properties properties) {
			super(properties, ArmorType.CHESTPLATE);
		}
	}

	public static class Leggings extends ReinforcedGoldenArmorItem {
		public Leggings(Item.Properties properties) {
			super(properties, ArmorType.LEGGINGS);
		}
	}

	public static class Boots extends ReinforcedGoldenArmorItem {
		public Boots(Item.Properties properties) {
			super(properties, ArmorType.BOOTS);
		}
	}
}
