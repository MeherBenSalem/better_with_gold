package tn.nightbeam.better_with_gold.platform;

import tn.nightbeam.better_with_gold.net.ForgeNetwork;
import tn.nightbeam.better_with_gold.platform.services.IPlatformHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {

	@Override
	public String getPlatformName() {
		return "Forge";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	@Override
	public Path configDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public void sendToPlayer(ServerPlayer player, String message) {
		ForgeNetwork.sendToPlayer(player, message);
	}

	@Override
	public void sendToServer(String message) {
		ForgeNetwork.sendToServer(message);
	}
}
