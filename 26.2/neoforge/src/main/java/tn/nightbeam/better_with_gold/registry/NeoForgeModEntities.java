package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.ThreatLabelEntity;
import tn.nightbeam.better_with_gold.compat.Compat;
import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class NeoForgeModEntities {

	private NeoForgeModEntities() {
	}

	public static void register(IEventBus bus) {
		bus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
				EntityType<AllyEntity> ally = EntityType.Builder.<AllyEntity>of(AllyEntity::new, MobCategory.MISC)
						.sized(0.6F, 1.8F).clientTrackingRange(10).build(key(ModEntities.ALLY_ID));
				EntityType<ThreatLabelEntity> label = EntityType.Builder.<ThreatLabelEntity>of(ThreatLabelEntity::new, MobCategory.MISC)
						.sized(0.05F, 0.05F).noSave().noSummon().clientTrackingRange(6).updateInterval(5).build(key(ModEntities.THREAT_LABEL_ID));
				event.register(Registries.ENTITY_TYPE, Compat.id(Constants.MOD_ID, ModEntities.ALLY_ID), () -> ally);
				event.register(Registries.ENTITY_TYPE, Compat.id(Constants.MOD_ID, ModEntities.THREAT_LABEL_ID), () -> label);
				ModEntities.ALLY = () -> ally;
				ModEntities.THREAT_LABEL = () -> label;
			}
		});
		bus.addListener((EntityAttributeCreationEvent event) -> event.put(ModEntities.ALLY.get(), AllyEntity.createAttributes().build()));
	}

	private static ResourceKey<EntityType<?>> key(String name) {
		return ResourceKey.create(Registries.ENTITY_TYPE, Compat.id(Constants.MOD_ID, name));
	}
}
