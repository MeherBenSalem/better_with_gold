package tn.nightbeam.better_with_gold.platform;

import tn.nightbeam.better_with_gold.net.NeoForgePayloads;
import tn.nightbeam.better_with_gold.platform.services.IPlatformHelper;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

	@Override
	public String getPlatformName() {
		return "NeoForge";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.getCurrent().isProduction();
	}

	@Override
	public Path configDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public void sendToPlayer(ServerPlayer player, String message) {
		if (player.connection != null && player.connection.hasChannel(NeoForgePayloads.ToClient.TYPE.id())) {
			PacketDistributor.sendToPlayer(player, new NeoForgePayloads.ToClient(message));
		}
	}

	@Override
	public void sendToServer(String message) {
		net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new NeoForgePayloads.ToServer(message));
	}
}
