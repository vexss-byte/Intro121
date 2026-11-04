package com.laperintro.director;

import com.laperintro.LaperIntro;
import com.laperintro.config.IntroConfig;
import com.laperintro.net.CameraPayload;
import com.laperintro.net.HudPayload;
import com.laperintro.net.PosePayload;
import com.laperintro.stage.StageBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mesin sinematik di sisi server. Semua member adalah player asli yang sedang online:
 * tubuh mereka dipindah ke pad, lalu pose dimainkan lewat Player Animation Library di tiap client.
 * Kamera adalah entity rig tak terlihat; client diminta memakainya sebagai kamera (tanpa memindahkan player).
 */
public final class IntroDirector {
	private enum Phase { INTRO, MYSTERY, MEMBER, FINALE }

	private record Slot(UUID id, String name, String quote, String pose, boolean mystery) {}

	private record Saved(ServerLevel level, double x, double y, double z, float yaw, float pitch, boolean invulnerable) {}

	public static final String[] POSES = {
		"pose_sword_up", "pose_arms_wide", "pose_point", "pose_wave",
		"pose_flex", "pose_cheer", "pose_stance", "pose_hold_forward"
	};

	private static final int INTRO_TICKS = 160;
	private static final int FINALE_TICKS = 440;
	private static final int FADE_TICKS = 50;
	private static final int LAST_EXTRA_TICKS = 60;

	private static boolean running;
	private static MinecraftServer server;
	private static ServerLevel level;
	private static BlockPos stage;
	private static Vec3 pad;
	private static final List<Slot> slots = new ArrayList<>();
	private static final Map<UUID, Saved> saved = new HashMap<>();
	private static Entity rig;
	private static boolean rigReady;
	private static int rigWait;
	private static Phase phase;
	private static int idx;
	private static int t;

	private IntroDirector() {}

	public static boolean isRunning() {
		return running;
	}

	// ---------------------------------------------------------------- dimensi

	/** Id dimensi, contoh "minecraft:overworld", diambil dari toString ResourceKey supaya tidak bergantung nama method. */
	public static String dimId(ServerLevel l) {
		String s = l.dimension().toString();
		int i = s.lastIndexOf("/ ");
		int j = s.lastIndexOf(']');
		return (i >= 0 && j > i) ? s.substring(i + 2, j).trim() : s;
	}

	public static ServerLevel resolveLevel(MinecraftServer s) {
		String want = IntroConfig.get().dimension;
		for (ServerLevel l : s.getAllLevels()) {
			if (dimId(l).equals(want)) return l;
		}
		return null;
	}

	// ---------------------------------------------------------------- start / stop

	/** @return pesan error, atau null jika berhasil dimulai. */
	public static String start(MinecraftServer s) {
		if (running) return "Sinematik sedang berjalan. Pakai /laperintro stop untuk menghentikan.";
		IntroConfig cfg = IntroConfig.get();
		ServerLevel lvl = resolveLevel(s);
		if (lvl == null) return "Dimensi stage tidak ditemukan: " + cfg.dimension;

		List<ServerPlayer> online = new ArrayList<>(s.getPlayerList().getPlayers());
		if (online.isEmpty()) return "Tidak ada player online.";

		Map<String, ServerPlayer> byName = new HashMap<>();
		for (ServerPlayer p : online) byName.put(p.getName().getString().toLowerCase(Locale.ROOT), p);

		List<Slot> list = new ArrayList<>();
		Set<UUID> used = new HashSet<>();
		int k = 0;
		for (IntroConfig.MemberEntry m : cfg.members) {
			ServerPlayer p = byName.get(m.name == null ? "" : m.name.toLowerCase(Locale.ROOT));
			if (p == null || !used.add(p.getUUID())) continue;
			list.add(new Slot(p.getUUID(), p.getName().getString(), m.quote == null ? "" : m.quote, pickPose(m.pose, k++), m.mystery));
		}
		List<ServerPlayer> rest = new ArrayList<>();
		for (ServerPlayer p : online) if (!used.contains(p.getUUID())) rest.add(p);
		rest.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)));
		for (ServerPlayer p : rest) {
			list.add(new Slot(p.getUUID(), p.getName().getString(), "", pickPose("", k++), false));
		}

		server = s;
		level = lvl;
		stage = new BlockPos(cfg.stageX, cfg.stageY, cfg.stageZ);
		pad = new Vec3(cfg.stageX + 0.5, cfg.stageY, cfg.stageZ + 0.5);
		slots.clear();
		slots.addAll(list);
		saved.clear();
		rig = null;
		rigReady = false;
		rigWait = 0;
		idx = 0;
		t = 0;
		phase = Phase.INTRO;
		running = true;

		for (int i = 0; i < slots.size(); i++) {
			ServerPlayer p = player(slots.get(i));
			if (p == null) continue;
			saved.put(p.getUUID(), new Saved((ServerLevel) p.level(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), p.isInvulnerable()));
			p.setInvulnerable(true); // server hardcore: jangan ada yang mati selama sinematik
			double[] h = StageBuilder.holdSlot(stage, i);
			tp(p, level, h[0], h[1], h[2], 0f);
		}
		LaperIntro.LOGGER.info("Sinematik dimulai dengan {} member.", slots.size());
		return null;
	}

	public static void stop(MinecraftServer s) {
		if (!running) return;
		finish();
	}

	private static String pickPose(String want, int i) {
		if (want != null) {
			for (String p : POSES) if (p.equals(want)) return want;
		}
		return POSES[Math.floorMod(i, POSES.length)];
	}

	// ---------------------------------------------------------------- tick

	public static void tick(MinecraftServer s) {
		if (!running) return;
		server = s;

		if (!rigReady) {
			rigWait++;
			if (rigWait >= 5 && spawnRig()) {
				rigReady = true;
				beginIntro();
			} else if (rigWait > 200) {
				LaperIntro.LOGGER.error("Entity kamera gagal dibuat; sinematik dibatalkan.");
				finish();
			}
			return;
		}

		t++;
		if (rig.isRemoved()) {
			finish();
			return;
		}
		if (t % 20 == 0) broadcast(new CameraPayload(rig.getId()));

		switch (phase) {
			case INTRO -> tickIntro();
			case MYSTERY -> tickMystery();
			case MEMBER -> tickMember();
			case FINALE -> tickFinale();
		}
	}

	private static boolean spawnRig() {
		Entity e = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
		if (e == null) return false;
		e.setInvisible(true);
		e.setNoGravity(true);
		e.setInvulnerable(true);
		e.snapTo(pad.add(0, 12, 22));
		if (!level.addFreshEntity(e)) return false;
		rig = e;
		return true;
	}

	private static void applyCamera(CameraShots.Pose pose) {
		rig.snapTo(pose.pos());
		rig.setYRot(pose.yaw());
		rig.setXRot(pose.pitch());
		rig.setYHeadRot(pose.yaw());
	}

	// ---------------------------------------------------------------- fase

	private static void beginIntro() {
		phase = Phase.INTRO;
		t = 0;
		applyCamera(CameraShots.intro(0, pad));
		IntroConfig cfg = IntroConfig.get();
		broadcast(new CameraPayload(rig.getId()));
		broadcast(new HudPayload(HudPayload.TITLE, 0, slots.size(), cfg.title, cfg.subtitle, INTRO_TICKS));
	}

	private static void tickIntro() {
		applyCamera(CameraShots.intro((double) t / INTRO_TICKS, pad));
		Fx.ambient(level, pad, 0.0, t);
		if (t >= INTRO_TICKS) beginMember(0);
	}

	private static void beginMember(int i) {
		if (i >= slots.size()) {
			beginFinale();
			return;
		}
		idx = i;
		t = 0;
		Slot sl = slots.get(i);
		ServerPlayer p = player(sl);
		if (p == null) { // member keluar server di tengah sinematik
			beginMember(i + 1);
			return;
		}
		tp(p, level, pad.x, pad.y, pad.z, 0f);

		boolean last = isLast(i);
		if (sl.mystery() || last) {
			phase = Phase.MYSTERY;
			int dur = mysteryTicks(last);
			p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur + 20, 0, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur + 20, 0, false, false));
			broadcast(new PosePayload(sl.id(), "pose_wave"));
			broadcast(new HudPayload(last ? HudPayload.LAST_INTRO : HudPayload.MYSTERY, i + 1, slots.size(), "???", last ? "DAN YANG TERAKHIR..." : "", dur));
			applyCamera(CameraShots.member(3, 0, pad));
		} else {
			beginReveal();
		}
	}

	private static void tickMystery() {
		boolean last = isLast(idx);
		int dur = mysteryTicks(last);
		double u = (double) t / dur;
		applyCamera(CameraShots.member(3, u, pad));
		Fx.mystery(level, pad, t);
		pinPerformer();
		if (t >= dur) beginReveal();
	}

	private static void beginReveal() {
		Slot sl = slots.get(idx);
		ServerPlayer p = player(sl);
		if (p != null) {
			p.removeEffect(MobEffects.INVISIBILITY);
			p.removeEffect(MobEffects.GLOWING);
		}
		phase = Phase.MEMBER;
		t = 0;
		boolean last = isLast(idx);
		Fx.dustBurst(level, pad);
		if (last) Fx.confetti(level, pad, 140);
		broadcast(new PosePayload(sl.id(), sl.pose()));
		broadcast(new HudPayload(HudPayload.MEMBER, idx + 1, slots.size(), sl.name(), sl.quote(), memberTicks(last)));
		applyCamera(CameraShots.member(shotFor(idx, 0), 0, pad));
	}

	private static void tickMember() {
		boolean last = isLast(idx);
		int total = memberTicks(last);
		int seg = Math.min(2, t * 3 / total);
		double segLen = total / 3.0;
		double u = (t - seg * segLen) / segLen;
		applyCamera(CameraShots.member(shotFor(idx, seg), u, pad));

		Fx.ambient(level, pad, slots.size() <= 1 ? 1.0 : (double) idx / (slots.size() - 1), t);
		if (last && t % 15 == 0) Fx.confetti(level, pad, 40);
		if (t == total - 8) Fx.dustBurst(level, pad);
		pinPerformer();
		if (t >= total) endMember();
	}

	private static void endMember() {
		Slot sl = slots.get(idx);
		broadcast(new PosePayload(sl.id(), ""));
		ServerPlayer p = player(sl);
		if (p != null) {
			double[] h = StageBuilder.holdSlot(stage, idx);
			tp(p, level, h[0], h[1], h[2], 0f);
		}
		beginMember(idx + 1);
	}

	private static void beginFinale() {
		phase = Phase.FINALE;
		t = 0;
		int n = slots.size();
		int rows = (n + 7) / 8;
		for (int i = 0; i < n; i++) {
			Slot sl = slots.get(i);
			ServerPlayer p = player(sl);
			if (p == null) continue;
			int row = i / 8;
			int col = i % 8;
			int cnt = Math.min(8, n - row * 8);
			double x = pad.x + (col - (cnt - 1) / 2.0) * 2.0;
			double z = pad.z + 3 - (rows - 1 - row) * 2.0;
			tp(p, level, x, pad.y, z, 0f);
			broadcast(new PosePayload(sl.id(), "pose_cheer"));
		}
		IntroConfig cfg = IntroConfig.get();
		broadcast(new HudPayload(HudPayload.FINALE, 0, n, cfg.title, "", FINALE_TICKS));
		Fx.confetti(level, pad, 200);
	}

	private static void tickFinale() {
		applyCamera(CameraShots.finale((double) t / FINALE_TICKS, pad));
		Fx.spotlights(level, pad, t);
		Fx.ambient(level, pad, 1.0, t);
		if (t % 20 == 0) Fx.confetti(level, pad.add(0, 1, 0), 50);
		if (t == FINALE_TICKS - FADE_TICKS) broadcast(new HudPayload(HudPayload.FADE, 0, slots.size(), "", "", FADE_TICKS));
		if (t >= FINALE_TICKS) finish();
	}

	private static void finish() {
		for (Slot sl : slots) broadcast(new PosePayload(sl.id(), ""));
		broadcast(new HudPayload(HudPayload.NONE, 0, 0, "", "", 0));
		broadcast(new CameraPayload(-1));
		for (Slot sl : slots) {
			ServerPlayer p = player(sl);
			Saved sv = saved.get(sl.id());
			if (p == null) continue;
			p.removeEffect(MobEffects.INVISIBILITY);
			p.removeEffect(MobEffects.GLOWING);
			if (sv != null) {
				p.setInvulnerable(sv.invulnerable());
				tp(p, sv.level(), sv.x(), sv.y(), sv.z(), sv.yaw());
			}
		}
		if (rig != null) rig.discard();
		rig = null;
		slots.clear();
		saved.clear();
		running = false;
		LaperIntro.LOGGER.info("Sinematik selesai.");
	}

	// ---------------------------------------------------------------- util

	private static boolean isLast(int i) {
		return slots.size() > 1 && i == slots.size() - 1;
	}

	private static int memberTicks(boolean last) {
		return IntroConfig.get().secondsPerMember * 20 + (last ? LAST_EXTRA_TICKS : 0);
	}

	private static int mysteryTicks(boolean last) {
		return IntroConfig.get().mysterySeconds * 20 + (last ? 40 : 0);
	}

	private static int shotFor(int member, int seg) {
		return Math.floorMod(member * 3 + seg * 2 + 1, CameraShots.MEMBER_SHOTS);
	}

	private static ServerPlayer player(Slot sl) {
		return server.getPlayerList().getPlayer(sl.id());
	}

	private static void tp(ServerPlayer p, ServerLevel to, double x, double y, double z, float yaw) {
		p.teleportTo(to, x, y, z, Set.<Relative>of(), yaw, 0f, false);
	}

	/** Cegah performer berjalan menjauh dari pad. */
	private static void pinPerformer() {
		if (t % 4 != 0) return;
		ServerPlayer p = player(slots.get(idx));
		if (p == null) return;
		double dx = p.getX() - pad.x, dz = p.getZ() - pad.z, dy = p.getY() - pad.y;
		if (dx * dx + dz * dz > 0.25 || Math.abs(dy) > 0.5 || p.level() != level) {
			tp(p, level, pad.x, pad.y, pad.z, 0f);
		}
	}

	private static void broadcast(CustomPacketPayload payload) {
		for (Slot sl : slots) {
			ServerPlayer p = player(sl);
			if (p != null) ServerPlayNetworking.send(p, payload);
		}
	}
}
