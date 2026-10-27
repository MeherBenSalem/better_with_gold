package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public abstract class ReinforcedGoldenArmorItem extends ArmorItem {

	private static final ArmorMaterial GOLD = ArmorMaterials.GOLD.value();
	private static final Holder<ArmorMaterial> REINFORCED = Holder.direct(new ArmorMaterial(GOLD.defense(),
			GOLD.enchantmentValue(), GOLD.equipSound(), GOLD.repairIngredient(),
			List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("better_with_gold", "gold_amyth"))),
			GOLD.toughness(), GOLD.knockbackResistance()));

	public ReinforcedGoldenArmorItem(Type type, Properties properties) {
		super(REINFORCED, type, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.better_with_gold.reinforced_golden_armor_helmet.description_0"));
	}

	public static class Helmet extends ReinforcedGoldenArmorItem {
		public Helmet() {
			super(Type.HELMET, new Properties());
		}
	}

	public static class Chestplate extends ReinforcedGoldenArmorItem {
		public Chestplate() {
			super(Type.CHESTPLATE, new Properties());
		}
	}

	public static class Leggings extends ReinforcedGoldenArmorItem {
		public Leggings() {
			super(Type.LEGGINGS, new Properties());
		}
	}

	public static class Boots extends ReinforcedGoldenArmorItem {
		public Boots() {
			super(Type.BOOTS, new Properties());
		}
	}
}
