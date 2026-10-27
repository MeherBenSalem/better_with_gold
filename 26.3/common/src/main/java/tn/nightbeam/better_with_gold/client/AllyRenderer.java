package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Allies use the player model with a skin per class and variant, plus worn armour. Minecraft 26.3 edition. */
public class AllyRenderer extends HumanoidMobRenderer<AllyEntity, AvatarRenderState, PlayerModel> {
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();

	public static class State extends AvatarRenderState {
		public Identifier texture;
	}

	public AllyRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
				context.getEquipmentRenderer()));
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AllyEntity entity, AvatarRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		if (state instanceof State s) {
			s.texture = texture(entity.classId(), entity.variant());
		}
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state instanceof State s && s.texture != null ? s.texture : texture("bulwark", 0);
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(AllyEntity mob, HumanoidArm arm) {
		if (mob.getItemHeldByArm(arm).isEmpty()) {
			return HumanoidModel.ArmPose.EMPTY;
		}
		return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW) ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
	}

	static Identifier texture(String classId, int variant) {
		String key = classId + "_" + variant;
		return TEXTURES.computeIfAbsent(key, k -> {
			Identifier id = Compat.id(Constants.MOD_ID, "textures/entity/ally/" + k + ".png");
			if (Minecraft.getInstance().getResourceManager().getResource(id).isPresent()) {
				return id;
			}
			Identifier first = Compat.id(Constants.MOD_ID, "textures/entity/ally/" + classId + "_0.png");
			if (Minecraft.getInstance().getResourceManager().getResource(first).isPresent()) {
				return first;
			}
			return Compat.id(Constants.MOD_ID, "textures/entity/ally/bulwark_0.png");
		});
	}

	public static void clearCache() {
		TEXTURES.clear();
	}
}
