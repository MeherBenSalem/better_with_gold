package tn.nightbeam.better_with_gold.platform;

import tn.nightbeam.better_with_gold.client.FabricClientNet;
import tn.nightbeam.better_with_gold.net.FabricChannels;
import tn.nightbeam.better_with_gold.net.Net;
import tn.nightbeam.better_with_gold.platform.services.IPlatformHelper;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {

	@Override
	public String getPlatformName() {
		return "Fabric";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Path configDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public void sendToPlayer(ServerPlayer player, String message) {
		if (ServerPlayNetworking.canSend(player, FabricChannels.TO_CLIENT)) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeUtf(message, Net.MAX_LENGTH);
			ServerPlayNetworking.send(player, FabricChannels.TO_CLIENT, buf);
		}
	}

	@Override
	public void sendToServer(String message) {
		FabricClientNet.send(message);
	}
}
