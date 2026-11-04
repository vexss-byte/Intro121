package com.laperintro.client;

import com.laperintro.LaperIntro;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Satu-satunya class yang menyentuh Player Animation Library. Dipisah supaya client tanpa PAL
 * tidak crash (class ini hanya dimuat jika PAL terpasang).
 */
final class PoseHandler {
	private PoseHandler() {}

	/** @return true jika perintah berhasil diterapkan pada entity player yang sudah ada di level client. */
	static boolean apply(UUID id, String pose) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		Entity e = mc.level.getEntity(id);
		if (!(e instanceof Avatar avatar)) return false;

		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(avatar, PlayerAnimLibMod.ANIMATION_LAYER_ID);
		if (!(layer instanceof AnimationController controller)) return false;

		if (pose.isEmpty()) {
			controller.stop();
			return true;
		}
		Animation animation = PlayerAnimResources.getAnimation(LaperIntro.id(pose));
		if (animation == null) {
			LaperIntro.LOGGER.warn("Animasi pose tidak ditemukan: {}", pose);
			return true; // jangan diulang terus
		}
		controller.triggerAnimation(RawAnimation.begin().thenPlayAndHold(animation));
		return true;
	}
}
