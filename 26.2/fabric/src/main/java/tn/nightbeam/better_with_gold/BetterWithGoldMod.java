package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.ally.ThreatLabelEntity;
import tn.nightbeam.better_with_gold.command.FellowshipCommands;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.net.FabricPayloads;
import tn.nightbeam.better_with_gold.registry.FabricModItems;
import tn.nightbeam.better_with_gold.registry.FabricModTabs;
import tn.nightbeam.better_with_gold.registry.ModEntities;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class BetterWithGoldMod implements ModInitializer {

	@Override
	public void onInitialize() {
		FabricModItems.register();
		FabricModTabs.register();
		registerEntities();

		PayloadTypeRegistry.serverboundPlay().register(FabricPayloads.ToServer.TYPE, FabricPayloads.ToServer.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FabricPayloads.ToClient.TYPE, FabricPayloads.ToClient.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(FabricPayloads.ToServer.TYPE,
				(payload, context) -> context.server().execute(() -> Fellowship.handle(context.player(), payload.json())));

		ServerLifecycleEvents.SERVER_STARTED.register(Fellowship::serverStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(Fellowship::serverStopping);
		ServerTickEvents.END_SERVER_TICK.register(Fellowship::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Fellowship.join(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Fellowship.leave(handler.player));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Fellowship::allowDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(Fellowship::onDeath);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> FellowshipCommands.register(dispatcher));
		BetterWithGold.init();
	}

	private static void registerEntities() {
		EntityType<AllyEntity> ally = entity(ModEntities.ALLY_ID, FabricEntityType.Builder.createMob(AllyEntity::new, MobCategory.MISC,
						mob -> mob.defaultAttributes(AllyEntity::createAttributes))
				.sized(0.6F, 1.8F).clientTrackingRange(10));
		EntityType<ThreatLabelEntity> label = entity(ModEntities.THREAT_LABEL_ID, EntityType.Builder.<ThreatLabelEntity>of(ThreatLabelEntity::new, MobCategory.MISC)
				.sized(0.05F, 0.05F).noSave().noSummon().clientTrackingRange(6).updateInterval(5));
		ModEntities.ALLY = () -> ally;
		ModEntities.THREAT_LABEL = () -> label;
	}

	private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Compat.id(Constants.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
