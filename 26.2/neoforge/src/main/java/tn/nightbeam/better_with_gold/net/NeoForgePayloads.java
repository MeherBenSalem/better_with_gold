package tn.nightbeam.better_with_gold.net;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.ally.Fellowship;
import tn.nightbeam.better_with_gold.client.ClientFellowship;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NeoForgePayloads {
	private NeoForgePayloads() {
	}

	public record ToServer(String json) implements CustomPacketPayload {
		public static final Type<ToServer> TYPE = new Type<>(Compat.id(Constants.MOD_ID, "c2s"));
		public static final StreamCodec<FriendlyByteBuf, ToServer> CODEC = StreamCodec.of(
				(buf, payload) -> buf.writeUtf(payload.json(), Net.MAX_LENGTH), buf -> new ToServer(buf.readUtf(Net.MAX_LENGTH)));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ToClient(String json) implements CustomPacketPayload {
		public static final Type<ToClient> TYPE = new Type<>(Compat.id(Constants.MOD_ID, "s2c"));
		public static final StreamCodec<FriendlyByteBuf, ToClient> CODEC = StreamCodec.of(
				(buf, payload) -> buf.writeUtf(payload.json(), Net.MAX_LENGTH), buf -> new ToClient(buf.readUtf(Net.MAX_LENGTH)));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1")
				.playToServer(ToServer.TYPE, ToServer.CODEC, (payload, context) -> context.enqueueWork(() -> {
					if (context.player() instanceof ServerPlayer player) {
						Fellowship.handle(player, payload.json());
					}
				}))
				.playToClient(ToClient.TYPE, ToClient.CODEC, (payload, context) -> context.enqueueWork(() -> ClientFellowship.handle(payload.json())));
	}
}
