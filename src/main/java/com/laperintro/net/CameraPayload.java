package com.laperintro.net;

import com.laperintro.LaperIntro;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: arahkan kamera client ke entity rig (entityId), atau kembalikan ke player jika -1. */
public record CameraPayload(int entityId) implements CustomPacketPayload {
	public static final Type<CameraPayload> TYPE = new Type<>(LaperIntro.id("camera"));
	public static final StreamCodec<FriendlyByteBuf, CameraPayload> CODEC = StreamCodec.ofMember(CameraPayload::write, CameraPayload::read);

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId + 1);
	}

	private static CameraPayload read(FriendlyByteBuf buf) {
		return new CameraPayload(buf.readVarInt() - 1);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
