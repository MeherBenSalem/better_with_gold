package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

public abstract class ReinforcedGoldenArmorItem extends Item {

	protected ReinforcedGoldenArmorItem(ArmorType type) {
		super(new Properties().humanoidArmor(ArmorMaterials.GOLD, type));
	}

	public static class Helmet extends ReinforcedGoldenArmorItem {
		public Helmet() {
			super(ArmorType.HELMET);
		}
	}

	public static class Chestplate extends ReinforcedGoldenArmorItem {
		public Chestplate() {
			super(ArmorType.CHESTPLATE);
		}
	}

	public static class Leggings extends ReinforcedGoldenArmorItem {
		public Leggings() {
			super(ArmorType.LEGGINGS);
		}
	}

	public static class Boots extends ReinforcedGoldenArmorItem {
		public Boots() {
			super(ArmorType.BOOTS);
		}
	}
}
