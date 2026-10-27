package tn.nightbeam.better_with_gold.net;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricPayloads {
	private FabricPayloads() {
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
}
