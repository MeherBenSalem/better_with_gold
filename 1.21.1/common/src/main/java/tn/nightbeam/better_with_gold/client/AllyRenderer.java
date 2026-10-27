package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Allies use the player model with a skin per class and variant, plus worn armour. Minecraft 1.21.1 edition. */
public class AllyRenderer extends HumanoidMobRenderer<AllyEntity, PlayerModel<AllyEntity>> {
	private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

	public AllyRenderer(EntityRendererProvider.Context context) {
		super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(AllyEntity entity) {
		return texture(entity.classId(), entity.variant());
	}

	static ResourceLocation texture(String classId, int variant) {
		String key = classId + "_" + variant;
		return TEXTURES.computeIfAbsent(key, k -> {
			ResourceLocation id = Compat.id(Constants.MOD_ID, "textures/entity/ally/" + k + ".png");
			if (Minecraft.getInstance().getResourceManager().getResource(id).isPresent()) {
				return id;
			}
			ResourceLocation first = Compat.id(Constants.MOD_ID, "textures/entity/ally/" + classId + "_0.png");
			if (Minecraft.getInstance().getResourceManager().getResource(first).isPresent()) {
				return first;
			}
			return Compat.id(Constants.MOD_ID, "textures/entity/ally/bulwark_0.png");
		});
	}

	public static void clearCache() {
		TEXTURES.clear();
	}

	/** Bow pose while fighting with a bow; items held up otherwise. */
	public static class Model extends PlayerModel<AllyEntity> {
		public Model(ModelPart root) {
			super(root, false);
		}

		@Override
		public void prepareMobModel(AllyEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
			ItemStack main = entity.getMainHandItem();
			ItemStack off = entity.getOffhandItem();
			HumanoidModel.ArmPose mainPose = main.isEmpty() ? ArmPose.EMPTY : main.is(Items.BOW) && entity.isAggressive() ? ArmPose.BOW_AND_ARROW : ArmPose.ITEM;
			HumanoidModel.ArmPose offPose = off.isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
			if (entity.getMainArm() == HumanoidArm.RIGHT) {
				rightArmPose = mainPose;
				leftArmPose = offPose;
			} else {
				leftArmPose = mainPose;
				rightArmPose = offPose;
			}
			super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
		}
	}
}
