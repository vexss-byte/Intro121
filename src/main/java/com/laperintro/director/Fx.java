package com.laperintro.director;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Efek partikel di pad. Makin banyak member yang sudah tampil, makin ramai (seperti di video). */
public final class Fx {
	private Fx() {}

	/** Ledakan debu putih naik, dipakai saat masuk dan keluar. */
	public static void dustBurst(ServerLevel l, Vec3 p) {
		l.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.2, p.z, 40, 0.7, 0.2, 0.7, 0.10);
		l.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.3, p.z, 25, 0.5, 0.6, 0.5, 0.06);
		for (int h = 0; h < 6; h++)
			l.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.3 + h * 0.6, p.z, 3, 0.2, 0.1, 0.2, 0.02);
	}

	/** Partikel ambien; progress 0..1 = seberapa jauh urutan member. */
	public static void ambient(ServerLevel l, Vec3 p, double progress, int tick) {
		if (tick % 3 != 0) return;
		int n = 1 + (int) (progress * 6);
		l.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.2, p.z, n, 1.6, 1.0, 1.6, 0.03);
		if (progress > 0.1) l.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 1.2, p.z, n, 1.8, 1.0, 1.8, 0.0);
		if (progress > 0.3) l.sendParticles(ParticleTypes.FIREWORK, p.x, p.y + 1.0, p.z, n, 1.6, 1.2, 1.6, 0.04);
		if (progress > 0.45) l.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 2.0, p.z, n * 2, 2.5, 1.5, 2.5, 0.02);
		if (progress > 0.7) l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y + 1.5, p.z, n, 1.5, 1.5, 1.5, 0.15);
	}

	/** Asap hitam samar untuk fase siluet "???". */
	public static void mystery(ServerLevel l, Vec3 p, int tick) {
		if (tick % 2 != 0) return;
		l.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.1, p.z, 3, 0.5, 0.1, 0.5, 0.01);
		l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1.0, p.z, 2, 0.5, 0.8, 0.5, 0.05);
	}

	/** Konfeti besar untuk reveal member terakhir dan penutup. */
	public static void confetti(ServerLevel l, Vec3 p, int amount) {
		l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.x, p.y + 1.5, p.z, amount, 2.5, 1.8, 2.5, 0.3);
		l.sendParticles(ParticleTypes.FIREWORK, p.x, p.y + 1.5, p.z, amount, 2.5, 1.8, 2.5, 0.2);
		l.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 1.5, p.z, amount / 2, 3.0, 2.0, 3.0, 0.0);
	}

	/** Dua berkas lampu sorot ke atas di kedua sisi pad. */
	public static void spotlights(ServerLevel l, Vec3 p, int tick) {
		if (tick % 2 != 0) return;
		for (int side : new int[] {-6, 6}) {
			for (int h = 0; h <= 16; h += 2)
				l.sendParticles(ParticleTypes.END_ROD, p.x + side, p.y + h, p.z - 2, 1, 0.15, 0.1, 0.15, 0.0);
		}
	}
}
