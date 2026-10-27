package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.FabricPayloads;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class BetterWithGoldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FellowshipKeys.all().forEach(KeyBindingHelper::registerKeyBinding);
		ClientTickEvents.END_CLIENT_TICK.register(FellowshipKeys::tick);
		HudRenderCallback.EVENT.register((graphics, tracker) -> FellowshipHud.render(graphics));
		ClientPlayNetworking.registerGlobalReceiver(FabricPayloads.ToClient.TYPE,
				(payload, context) -> context.client().execute(() -> ClientFellowship.handle(payload.json())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientSetup::disconnected));
		ClientSetup.renderers(new ClientSetup.RendererSink() {
			@Override
			public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
				EntityRendererRegistry.register(type, provider);
			}
		});
	}
}
