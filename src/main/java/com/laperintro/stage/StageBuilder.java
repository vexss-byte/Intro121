package com.laperintro.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Membangun panggung seperti di video: lantai gelap melingkar, garis putih putus-putus, pad emas di tengah,
 * gerbang dengan jendela biru, tiang berlentera, rangka di atas, dan ruang tunggu tertutup di bawahnya.
 * Titik {@code c} = posisi kaki player di atas pad emas.
 */
public final class StageBuilder {
	private static final int FLAGS = 2; // kirim ke client, tanpa update tetangga

	public static final int HOLD_DEPTH = 40;
	public static final int HOLD_HALF = 11;

	private StageBuilder() {}

	private static void set(ServerLevel l, int x, int y, int z, Block b) {
		l.setBlock(new BlockPos(x, y, z), b.defaultBlockState(), FLAGS);
	}

	private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
		l.setBlock(new BlockPos(x, y, z), s, FLAGS);
	}

	public static void build(ServerLevel l, BlockPos c) {
		final int cx = c.getX(), cz = c.getZ(), top = c.getY() - 1;

		// 1. kosongkan volume panggung
		for (int dx = -16; dx <= 16; dx++)
			for (int dz = -16; dz <= 16; dz++)
				for (int y = top + 1; y <= top + 18; y++)
					set(l, cx + dx, y, cz + dz, Blocks.AIR);

		// 2. lantai bundar dua lapis
		for (int dx = -15; dx <= 15; dx++) {
			for (int dz = -15; dz <= 15; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 14.5) continue;
				Block surface;
				if (d > 13.2) surface = Blocks.POLISHED_BLACKSTONE_BRICKS;
				else surface = ((dx + dz) & 1) == 0 ? Blocks.BLACKSTONE : Blocks.POLISHED_BLACKSTONE;
				set(l, cx + dx, top, cz + dz, surface);
				set(l, cx + dx, top - 1, cz + dz, Blocks.BLACKSTONE);
			}
		}

		// 3. cincin putus-putus putih (radius 10) dan garis lintasan ke arah kamera (+Z)
		for (int deg = 0; deg < 360; deg += 14) {
			double a = Math.toRadians(deg);
			set(l, cx + (int) Math.round(Math.cos(a) * 10), top, cz + (int) Math.round(Math.sin(a) * 10), Blocks.QUARTZ_BLOCK);
		}
		for (int z = 4; z <= 13; z += 3) {
			set(l, cx, top, cz + z, Blocks.QUARTZ_BLOCK);
			set(l, cx - 4, top, cz + z, Blocks.QUARTZ_BLOCK);
			set(l, cx + 4, top, cz + z, Blocks.QUARTZ_BLOCK);
		}

		// 4. pad emas 3x3
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				set(l, cx + dx, top, cz + dz, Blocks.GOLD_BLOCK);

		// 5. gerbang di belakang pad (-Z) dengan jendela biru
		final int gz = cz - 9;
		for (int side : new int[] {-8, 7}) {
			for (int w = 0; w < 2; w++)
				for (int dz = 0; dz < 2; dz++)
					for (int y = top + 1; y <= top + 13; y++)
						set(l, cx + side + w, y, gz + dz, Blocks.POLISHED_BLACKSTONE_BRICKS);
		}
		for (int x = cx - 8; x <= cx + 8; x++)
			for (int dz = 0; dz < 2; dz++)
				for (int y = top + 11; y <= top + 13; y++)
					set(l, x, y, gz + dz, Blocks.POLISHED_BLACKSTONE_BRICKS);
		for (int x = cx - 8; x <= cx + 8; x += 2)
			set(l, x, top + 14, gz, Blocks.BLACKSTONE);
		for (int x = cx - 6; x <= cx + 6; x++)
			for (int y = top + 1; y <= top + 10; y++)
				set(l, x, y, gz, Blocks.GLASS);

		// 6. tiang berlentera mengelilingi tepi
		for (int deg = 0; deg < 360; deg += 30) {
			double a = Math.toRadians(deg);
			int px = cx + (int) Math.round(Math.cos(a) * 13.5);
			int pz = cz + (int) Math.round(Math.sin(a) * 13.5);
			if (pz < gz + 3 && Math.abs(px - cx) < 9) continue; // jangan menabrak gerbang
			for (int y = top + 1; y <= top + 4; y++)
				set(l, px, y, pz, Blocks.POLISHED_BLACKSTONE_BRICKS);
			set(l, px, top + 5, pz, Blocks.LANTERN);
		}

		// 7. rangka di atas panggung (empat tiang sudut + balok)
		for (int sx : new int[] {-14, 14}) {
			for (int sz : new int[] {-14, 14}) {
				for (int y = top + 1; y <= top + 16; y++)
					set(l, cx + sx, y, cz + sz, Blocks.BLACKSTONE);
			}
		}
		for (int i = -14; i <= 14; i++) {
			set(l, cx + i, top + 16, cz - 14, Blocks.BLACKSTONE);
			set(l, cx + i, top + 16, cz + 14, Blocks.BLACKSTONE);
			set(l, cx - 14, top + 16, cz + i, Blocks.BLACKSTONE);
			set(l, cx + 14, top + 16, cz + i, Blocks.BLACKSTONE);
		}

		buildHoldingRoom(l, c);
	}

	/** Ruang tunggu tertutup, jauh di bawah panggung, tempat member menunggu giliran di luar bidikan kamera. */
	private static void buildHoldingRoom(ServerLevel l, BlockPos c) {
		final int cx = c.getX(), cz = c.getZ(), floor = c.getY() - HOLD_DEPTH - 1;
		final int h = HOLD_HALF + 1;
		for (int dx = -h; dx <= h; dx++) {
			for (int dz = -h; dz <= h; dz++) {
				boolean wall = Math.abs(dx) == h || Math.abs(dz) == h;
				set(l, cx + dx, floor, cz + dz, Blocks.BLACKSTONE);
				set(l, cx + dx, floor + 7, cz + dz, Blocks.BLACKSTONE);
				for (int y = floor + 1; y <= floor + 6; y++)
					set(l, cx + dx, y, cz + dz, wall ? Blocks.BLACKSTONE : Blocks.AIR);
			}
		}
		set(l, cx, floor + 6, cz, Blocks.GLOWSTONE);
	}

	/** Posisi menunggu untuk member ke-i (0-based) di ruang tunggu. */
	public static double[] holdSlot(BlockPos c, int i) {
		double x = c.getX() + 0.5 - 9 + (i % 7) * 3;
		double z = c.getZ() + 0.5 - 9 + ((i / 7) % 7) * 3;
		double y = c.getY() - HOLD_DEPTH;
		return new double[] {x, y, z};
	}
}
