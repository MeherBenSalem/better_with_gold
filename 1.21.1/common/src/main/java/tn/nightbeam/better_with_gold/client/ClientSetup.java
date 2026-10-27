package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.ally.AllyEntity;
import tn.nightbeam.better_with_gold.ally.ThreatLabelEntity;
import tn.nightbeam.better_with_gold.registry.ModEntities;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Entity renderers, registered through each loader's own hook. Minecraft 1.21.1 edition. */
public final class ClientSetup {
	public interface RendererSink {
		<T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
	}

	private ClientSetup() {
	}

	public static void renderers(RendererSink sink) {
		sink.<AllyEntity>register(ModEntities.ALLY.get(), AllyRenderer::new);
		sink.<ThreatLabelEntity>register(ModEntities.THREAT_LABEL.get(), NoopRenderer::new);
	}

	public static void disconnected() {
		ClientFellowship.reset();
		AllyRenderer.clearCache();
	}
}
