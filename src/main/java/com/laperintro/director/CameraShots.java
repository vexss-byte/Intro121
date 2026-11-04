package com.laperintro.director;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Kumpulan gerakan kamera (dolly, orbit, close-up, crane, dll.). Semua dihitung relatif terhadap pad. */
public final class CameraShots {
	public record Pose(Vec3 pos, float yaw, float pitch) {}

	public static final int MEMBER_SHOTS = 6;

	private CameraShots() {}

	private static double ease(double t) {
		t = Mth.clamp(t, 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
		return new Vec3(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z));
	}

	public static Pose lookAt(Vec3 pos, Vec3 target) {
		double dx = target.x - pos.x, dy = target.y - pos.y, dz = target.z - pos.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
		return new Pose(pos, yaw, pitch);
	}

	/** Bidikan untuk member. pad = posisi kaki di pad; u = 0..1 progres bidikan. */
	public static Pose member(int shot, double u, Vec3 pad) {
		double e = ease(u);
		switch (Math.floorMod(shot, MEMBER_SHOTS)) {
			case 0: // dolly masuk dari kamera rendah
				return lookAt(pad.add(lerp(new Vec3(-2.5, 0.9, 7.5), new Vec3(-0.8, 1.1, 3.6), e)), pad.add(0, 1.1, 0));
			case 1: { // orbit
				double a = Math.toRadians(Mth.lerp(e, -60.0, 28.0));
				double r = Mth.lerp(e, 5.5, 4.0);
				return lookAt(pad.add(Math.sin(a) * r, Mth.lerp(e, 1.1, 1.6), Math.cos(a) * r), pad.add(0, 1.1, 0));
			}
			case 2: // close-up wajah
				return lookAt(pad.add(lerp(new Vec3(0.6, 1.7, 3.3), new Vec3(0.2, 1.75, 2.3), e)), pad.add(0, 1.65, 0));
			case 3: // low angle menengadah
				return lookAt(pad.add(lerp(new Vec3(-1.8, 0.35, 4.6), new Vec3(1.2, 0.5, 3.9), e)), pad.add(0, Mth.lerp(e, 1.9, 1.3), 0));
			case 4: // crane naik
				return lookAt(pad.add(2.5, Mth.lerp(e, 0.6, 3.4), 6.0), pad.add(0, 1.2, 0));
			default: // track menyamping
				return lookAt(pad.add(lerp(new Vec3(-6, 1.2, 2.5), new Vec3(-4.5, 1.3, -1.0), e)), pad.add(0, 1.2, 0));
		}
	}

	/** Pembuka: orbit lambat di sekeliling panggung yang masih kosong. */
	public static Pose intro(double u, Vec3 pad) {
		double e = ease(u);
		double a = Math.toRadians(Mth.lerp(e, 35.0, -25.0));
		double r = Mth.lerp(e, 24.0, 15.0);
		double y = Mth.lerp(e, 10.0, 4.0);
		return lookAt(pad.add(Math.sin(a) * r, y, Math.cos(a) * r), pad.add(0, 2.5, 0));
	}

	/** Penutup: awalnya rendah lebar menghadap kerumunan, lalu orbit tinggi. */
	public static Pose finale(double u, Vec3 pad) {
		if (u < 0.4) {
			double e = ease(u / 0.4);
			return lookAt(pad.add(lerp(new Vec3(0, 1.2, 17), new Vec3(0, 2.4, 10.5), e)), pad.add(0, 1.5, -2));
		}
		double e = (u - 0.4) / 0.6;
		double a = Math.toRadians(Mth.lerp(ease(e), 0.0, 220.0));
		double r = Mth.lerp(ease(e), 11.0, 18.0);
		double y = Mth.lerp(ease(e), 3.0, 13.0);
		return lookAt(pad.add(Math.sin(a) * r, y, Math.cos(a) * r), pad.add(0, 1.2, 0));
	}
}
