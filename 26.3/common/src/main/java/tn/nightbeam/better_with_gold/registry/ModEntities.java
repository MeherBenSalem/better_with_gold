package tn.nightbeam.better_with_gold.registry;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.ThreatLabelEntity;

import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/** Entity types, filled in by each loader's registration. */
public final class ModEntities {
	public static Supplier<EntityType<AllyEntity>> ALLY;
	public static Supplier<EntityType<ThreatLabelEntity>> THREAT_LABEL;

	public static final String ALLY_ID = "ally";
	public static final String THREAT_LABEL_ID = "aggro_label";

	private ModEntities() {
	}
}
