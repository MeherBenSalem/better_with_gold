package tn.nightbeam.better_with_gold.net;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.client.ClientFellowship;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** The Fellowship channel: one JSON string per packet. Forge 1.20.1 edition. */
public final class ForgeNetwork {
	private static final String PROTOCOL = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Compat.id(Constants.MOD_ID, "main"), () -> PROTOCOL,
			NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

	public record ToServer(String json) {
	}

	public record ToClient(String json) {
	}

	private ForgeNetwork() {
	}

	public static void register() {
		CHANNEL.registerMessage(0, ToServer.class, (m, buf) -> buf.writeUtf(m.json(), Net.MAX_LENGTH), buf -> new ToServer(buf.readUtf(Net.MAX_LENGTH)),
				ForgeNetwork::handleServer, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		CHANNEL.registerMessage(1, ToClient.class, (m, buf) -> buf.writeUtf(m.json(), Net.MAX_LENGTH), buf -> new ToClient(buf.readUtf(Net.MAX_LENGTH)),
				ForgeNetwork::handleClient, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

	private static void handleServer(ToServer message, Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		ServerPlayer player = context.getSender();
		context.enqueueWork(() -> {
			if (player != null) {
				Fellowship.handle(player, message.json());
			}
		});
		context.setPacketHandled(true);
	}

	private static void handleClient(ToClient message, Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> ClientFellowship.handle(message.json()));
		context.setPacketHandled(true);
	}

	public static void sendToPlayer(ServerPlayer player, String json) {
		if (CHANNEL.isRemotePresent(player.connection.connection)) {
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ToClient(json));
		}
	}

	public static void sendToServer(String json) {
		CHANNEL.sendToServer(new ToServer(json));
	}
}
