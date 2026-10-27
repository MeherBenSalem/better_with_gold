package tn.nightbeam.better_with_gold.net;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.client.ClientFellowship;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** The Fellowship channel: one JSON string per packet. Forge 1.21.1 edition. */
public final class ForgeNetwork {
	public record ToServer(String json) {
	}

	public record ToClient(String json) {
	}

	public static final SimpleChannel CHANNEL = ChannelBuilder.named(Compat.id(Constants.MOD_ID, "main"))
			.networkProtocolVersion(1)
			.optional()
			.simpleChannel()
			.messageBuilder(ToServer.class, NetworkDirection.PLAY_TO_SERVER)
			.encoder((m, buf) -> buf.writeUtf(m.json(), Net.MAX_LENGTH))
			.decoder(buf -> new ToServer(buf.readUtf(Net.MAX_LENGTH)))
			.consumerMainThread(ForgeNetwork::handleServer)
			.add()
			.messageBuilder(ToClient.class, NetworkDirection.PLAY_TO_CLIENT)
			.encoder((m, buf) -> buf.writeUtf(m.json(), Net.MAX_LENGTH))
			.decoder(buf -> new ToClient(buf.readUtf(Net.MAX_LENGTH)))
			.consumerMainThread(ForgeNetwork::handleClient)
			.add();

	private ForgeNetwork() {
	}

	/** Loads the class, which builds and registers the channel. */
	public static void register() {
	}

	private static void handleServer(ToServer message, CustomPayloadEvent.Context context) {
		ServerPlayer player = context.getSender();
		if (player != null) {
			Fellowship.handle(player, message.json());
		}
		context.setPacketHandled(true);
	}

	private static void handleClient(ToClient message, CustomPayloadEvent.Context context) {
		ClientFellowship.handle(message.json());
		context.setPacketHandled(true);
	}

	public static void sendToPlayer(ServerPlayer player, String json) {
		if (CHANNEL.isRemotePresent(player.connection.getConnection())) {
			CHANNEL.send(new ToClient(json), PacketDistributor.PLAYER.with(player));
		}
	}

	public static void sendToServer(String json) {
		CHANNEL.send(new ToServer(json), PacketDistributor.SERVER.noArg());
	}
}
