package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;

public abstract class ReinforcedGoldenArmorItem extends ArmorItem {

	public ReinforcedGoldenArmorItem(Type type, Properties properties) {
		super(ArmorMaterials.GOLD, type, properties);
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
