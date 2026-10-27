package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.net.FabricChannels;
import tn.nightbeam.better_with_gold.net.Net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class FabricClientNet {
	private FabricClientNet() {
	}

	public static void send(String message) {
		if (ClientPlayNetworking.canSend(FabricChannels.TO_SERVER)) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeUtf(message, Net.MAX_LENGTH);
			ClientPlayNetworking.send(FabricChannels.TO_SERVER, buf);
		}
	}
}
