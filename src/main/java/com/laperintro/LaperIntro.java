package com.laperintro;

import com.laperintro.command.IntroCommand;
import com.laperintro.config.IntroConfig;
import com.laperintro.director.IntroDirector;
import com.laperintro.net.CameraPayload;
import com.laperintro.net.HudPayload;
import com.laperintro.net.PosePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaperIntro implements ModInitializer {
	public static final String MOD_ID = "laperintro";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		IntroConfig.load();

		PayloadTypeRegistry.clientboundPlay().register(PosePayload.TYPE, PosePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HudPayload.TYPE, HudPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CameraPayload.TYPE, CameraPayload.CODEC);

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> IntroCommand.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(IntroDirector::tick);

		LOGGER.info("Laper Intro dimuat. Gunakan /laperintro build lalu /laperintro start.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
