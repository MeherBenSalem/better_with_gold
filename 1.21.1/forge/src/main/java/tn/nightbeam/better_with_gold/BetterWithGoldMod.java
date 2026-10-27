package tn.nightbeam.better_with_gold;

import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.command.FellowshipCommands;
import tn.nightbeam.better_with_gold.net.ForgeNetwork;
import tn.nightbeam.better_with_gold.registry.ForgeModEntities;
import tn.nightbeam.better_with_gold.registry.ForgeModItems;
import tn.nightbeam.better_with_gold.registry.ForgeModTabs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class BetterWithGoldMod {

	public BetterWithGoldMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ForgeModItems.register(modBus);
		ForgeModTabs.register(modBus);
		ForgeModEntities.register(modBus);
		ForgeNetwork.register();

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((ServerStartedEvent event) -> Fellowship.serverStarted(event.getServer()));
		bus.addListener((ServerStoppingEvent event) -> Fellowship.serverStopping(event.getServer()));
		bus.addListener((TickEvent.ServerTickEvent.Post event) -> Fellowship.tick(event.getServer()));
		bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Fellowship.join(player);
			}
		});
		bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				Fellowship.leave(player);
			}
		});
		bus.addListener((LivingAttackEvent event) -> {
			if (!Fellowship.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		bus.addListener((LivingDeathEvent event) -> {
			if (!event.isCanceled()) {
				Fellowship.onDeath(event.getEntity(), event.getSource());
			}
		});
		bus.addListener((RegisterCommandsEvent event) -> FellowshipCommands.register(event.getDispatcher()));
		BetterWithGold.init();
	}
}
