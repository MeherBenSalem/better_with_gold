package tn.nightbeam.better_with_gold.platform.services;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {

	String getPlatformName();

	boolean isModLoaded(String modId);

	boolean isDevelopmentEnvironment();

	default String getEnvironmentName() {
		return isDevelopmentEnvironment() ? "development" : "production";
	}

	Path configDirectory();

	/** Sends one JSON message to a player over the Fellowship channel. */
	void sendToPlayer(ServerPlayer player, String message);

	/** Sends one JSON message from the client to the server. Client side only. */
	void sendToServer(String message);
}
