package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public abstract class ReinforcedGoldenArmorItem extends ArmorItem {

	private static final ArmorMaterial GOLD = ArmorMaterials.GOLD.value();
	private static final Holder<ArmorMaterial> REINFORCED = Holder.direct(new ArmorMaterial(GOLD.defense(),
			GOLD.enchantmentValue(), GOLD.equipSound(), GOLD.repairIngredient(),
			List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("better_with_gold", "gold_amyth"))),
			GOLD.toughness(), GOLD.knockbackResistance()));

	public ReinforcedGoldenArmorItem(Type type, Properties properties) {
		super(REINFORCED, type, properties);
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
