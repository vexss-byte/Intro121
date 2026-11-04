package com.laperintro.net;

import com.laperintro.LaperIntro;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: status overlay sinematik. */
public record HudPayload(int mode, int index, int total, String name, String text, int durationTicks) implements CustomPacketPayload {
	public static final int NONE = 0;
	public static final int TITLE = 1;
	public static final int MEMBER = 2;
	public static final int MYSTERY = 3;
	public static final int LAST_INTRO = 4;
	public static final int FINALE = 5;
	public static final int FADE = 6;

	public static final Type<HudPayload> TYPE = new Type<>(LaperIntro.id("hud"));
	public static final StreamCodec<FriendlyByteBuf, HudPayload> CODEC = StreamCodec.ofMember(HudPayload::write, HudPayload::read);

	public static HudPayload of(int mode) {
		return new HudPayload(mode, 0, 0, "", "", 0);
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(mode);
		buf.writeVarInt(index);
		buf.writeVarInt(total);
		buf.writeUtf(name);
		buf.writeUtf(text);
		buf.writeVarInt(durationTicks);
	}

	private static HudPayload read(FriendlyByteBuf buf) {
		return new HudPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
