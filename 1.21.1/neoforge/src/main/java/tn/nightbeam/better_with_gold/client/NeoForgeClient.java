package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClient {
	private NeoForgeClient() {
	}

	@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModBus {
		private ModBus() {
		}

		@SubscribeEvent
		public static void keys(RegisterKeyMappingsEvent event) {
			FellowshipKeys.all().forEach(event::register);
		}

		@SubscribeEvent
		public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
			ClientSetup.renderers(new ClientSetup.RendererSink() {
				@Override
				public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
					event.registerEntityRenderer(type, provider);
				}
			});
		}

		@SubscribeEvent
		public static void layers(RegisterGuiLayersEvent event) {
			event.registerAbove(VanillaGuiLayers.HOTBAR, Compat.id(Constants.MOD_ID, "party_frames"), (graphics, tracker) -> FellowshipHud.render(graphics));
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		FellowshipKeys.tick(Minecraft.getInstance());
	}

	@SubscribeEvent
	public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientSetup.disconnected();
	}
}
