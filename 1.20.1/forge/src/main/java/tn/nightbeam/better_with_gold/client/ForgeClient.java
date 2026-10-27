package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class ForgeClient {
	private ForgeClient() {
	}

	@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
		public static void overlays(RegisterGuiOverlaysEvent event) {
			event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "party_frames", (gui, graphics, partialTick, width, height) -> FellowshipHud.render(graphics));
		}
	}

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			FellowshipKeys.tick(Minecraft.getInstance());
		}
	}

	@SubscribeEvent
	public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientSetup.disconnected();
	}
}
