package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.FabricPayloads;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricClientNet {
	private FabricClientNet() {
	}

	public static void send(String message) {
		if (ClientPlayNetworking.canSend(FabricPayloads.ToServer.TYPE)) {
			ClientPlayNetworking.send(new FabricPayloads.ToServer(message));
		}
	}
}
