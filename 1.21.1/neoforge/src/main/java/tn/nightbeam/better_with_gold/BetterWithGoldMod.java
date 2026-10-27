package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.command.FellowshipCommands;
import tn.nightbeam.better_with_gold.net.NeoForgePayloads;
import tn.nightbeam.better_with_gold.registry.NeoForgeModEntities;
import tn.nightbeam.better_with_gold.registry.NeoForgeModItems;
import tn.nightbeam.better_with_gold.registry.NeoForgeModTabs;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class BetterWithGoldMod {

	public BetterWithGoldMod(IEventBus modBus) {
		NeoForgeModItems.register(modBus);
		NeoForgeModTabs.register(modBus);
		NeoForgeModEntities.register(modBus);
		modBus.addListener(NeoForgePayloads::register);

		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener(ServerStartedEvent.class, event -> Fellowship.serverStarted(event.getServer()));
		bus.addListener(ServerStoppingEvent.class, event -> Fellowship.serverStopping(event.getServer()));
		bus.addListener(ServerTickEvent.Post.class, event -> Fellowship.tick(event.getServer()));
		bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Fellowship.join(player);
			}
		});
		bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Fellowship.leave(player);
			}
		});
		bus.addListener(LivingIncomingDamageEvent.class, event -> {
			if (!Fellowship.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(LivingDeathEvent.class, event -> {
			if (!event.isCanceled()) {
				Fellowship.onDeath(event.getEntity(), event.getSource());
			}
		});
		bus.addListener(RegisterCommandsEvent.class, event -> FellowshipCommands.register(event.getDispatcher()));
		BetterWithGold.init();
	}
}
