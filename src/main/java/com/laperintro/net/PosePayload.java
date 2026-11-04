package com.laperintro.net;

import com.laperintro.LaperIntro;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Server -> client: mainkan pose pada badan player asli. pose kosong = hentikan. */
public record PosePayload(UUID player, String pose) implements CustomPacketPayload {
	public static final Type<PosePayload> TYPE = new Type<>(LaperIntro.id("pose"));
	public static final StreamCodec<FriendlyByteBuf, PosePayload> CODEC = StreamCodec.ofMember(PosePayload::write, PosePayload::read);

	private void write(FriendlyByteBuf buf) {
		buf.writeUUID(player);
		buf.writeUtf(pose);
	}

	private static PosePayload read(FriendlyByteBuf buf) {
		return new PosePayload(buf.readUUID(), buf.readUtf());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
