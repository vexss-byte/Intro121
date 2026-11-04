package com.laperintro.client;

import com.laperintro.LaperIntro;
import com.laperintro.net.CameraPayload;
import com.laperintro.net.HudPayload;
import com.laperintro.net.PosePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LaperIntroClient implements ClientModInitializer {
	private static final boolean HAS_PAL = FabricLoader.getInstance().isModLoaded("player_animation_library");

	/** Pose yang diminta server tetapi belum bisa diterapkan (player belum ter-track di client). */
	private static final Map<UUID, String> pending = new HashMap<>();
	private static CameraType savedCameraType;

	@Override
	public void onInitializeClient() {
		if (!HAS_PAL) {
			LaperIntro.LOGGER.warn("Player Animation Library tidak terpasang: pose tidak akan dimainkan.");
		}

		ClientPlayNetworking.registerGlobalReceiver(PosePayload.TYPE, (payload, context) -> {
			if (!HAS_PAL) return;
			if (!PoseHandler.apply(payload.player(), payload.pose())) pending.put(payload.player(), payload.pose());
			else pending.remove(payload.player());
		});

		ClientPlayNetworking.registerGlobalReceiver(HudPayload.TYPE, (payload, context) -> IntroHud.update(payload));

		ClientPlayNetworking.registerGlobalReceiver(CameraPayload.TYPE, (payload, context) -> {
			Minecraft mc = context.client();
			if (payload.entityId() < 0) {
				restoreCamera(mc);
				return;
			}
			if (mc.level == null) return;
			Entity rig = mc.level.getEntity(payload.entityId());
			if (rig == null) return; // belum ter-track; server mengirim ulang tiap detik
			if (mc.getCameraEntity() != rig) {
				if (savedCameraType == null) savedCameraType = mc.options.getCameraType();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(rig);
			}
		});

		// Coba ulang pose yang tertunda
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (pending.isEmpty() || client.level == null || client.player == null) return;
			if (client.player.tickCount % 5 != 0) return;
			pending.entrySet().removeIf(e -> PoseHandler.apply(e.getKey(), e.getValue()));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			pending.clear();
			IntroHud.reset();
			savedCameraType = null;
		});

		// Sembunyikan HUD vanilla selama sinematik
		List<Identifier> vanilla = List.of(
			VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR,
			VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR,
			VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
			VanillaHudElements.INFO_BAR_FOREGROUND, VanillaHudElements.HELD_ITEM_TOOLTIP,
			VanillaHudElements.MOB_EFFECTS, VanillaHudElements.BOSS_BAR, VanillaHudElements.SCOREBOARD,
			VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.TITLE_AND_SUBTITLE,
			VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES
		);
		for (Identifier id : vanilla) {
			try {
				HudElementRegistry.replaceElement(id, original -> (graphics, delta) -> {
					if (!IntroHud.isCinematic()) original.extractRenderState(graphics, delta);
				});
			} catch (RuntimeException e) {
				LaperIntro.LOGGER.warn("Tidak bisa membungkus elemen HUD {}", id, e);
			}
		}
		HudElementRegistry.addLast(LaperIntro.id("overlay"), IntroHud::render);
	}

	private static void restoreCamera(Minecraft mc) {
		IntroHud.reset();
		pending.clear();
		if (mc.player != null && mc.getCameraEntity() != mc.player) mc.setCameraEntity(mc.player);
		if (savedCameraType != null) {
			mc.options.setCameraType(savedCameraType);
			savedCameraType = null;
		}
	}
}
