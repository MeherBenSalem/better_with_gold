package tn.nightbeam.better_with_gold.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public abstract class ForgeReinforcedGoldenArmorItem extends ReinforcedGoldenArmorItem {

	protected ForgeReinforcedGoldenArmorItem(Type type, Properties properties) {
		super(type, properties);
	}

	public static class Helmet extends ForgeReinforcedGoldenArmorItem {
		public Helmet() {
			super(Type.HELMET, new Properties());
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "better_with_gold:textures/models/armor/gold_amyth_layer_1.png";
		}
	}

	public static class Chestplate extends ForgeReinforcedGoldenArmorItem {
		public Chestplate() {
			super(Type.CHESTPLATE, new Properties());
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "better_with_gold:textures/models/armor/gold_amyth_layer_1.png";
		}
	}

	public static class Leggings extends ForgeReinforcedGoldenArmorItem {
		public Leggings() {
			super(Type.LEGGINGS, new Properties());
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "better_with_gold:textures/models/armor/gold_amyth_layer_2.png";
		}
	}

	public static class Boots extends ForgeReinforcedGoldenArmorItem {
		public Boots() {
			super(Type.BOOTS, new Properties());
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "better_with_gold:textures/models/armor/gold_amyth_layer_1.png";
		}
	}
}
