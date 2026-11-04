package com.laperintro.command;

import com.laperintro.config.IntroConfig;
import com.laperintro.director.IntroDirector;
import com.laperintro.stage.StageBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class IntroCommand {
	private IntroCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("laperintro")
			.requires(IntroCommand::allowed)
			.then(Commands.literal("setstage").executes(IntroCommand::setStage))
			.then(Commands.literal("build").executes(IntroCommand::build))
			.then(Commands.literal("start").executes(IntroCommand::start))
			.then(Commands.literal("stop").executes(IntroCommand::stop))
			.then(Commands.literal("reload").executes(IntroCommand::reload)));
	}

	/** Singleplayer, console, atau nama yang tercantum di "admins" pada config. */
	private static boolean allowed(CommandSourceStack src) {
		if (src.getServer().isSingleplayer()) return true;
		ServerPlayer p = src.getPlayer();
		if (p == null) return true;
		return IntroConfig.get().admins.contains(p.getName().getString());
	}

	private static int setStage(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			src.sendFailure(Component.literal("Jalankan sebagai player."));
			return 0;
		}
		BlockPos pos = p.blockPosition();
		IntroConfig cfg = IntroConfig.get();
		cfg.stageX = pos.getX();
		cfg.stageY = pos.getY();
		cfg.stageZ = pos.getZ();
		cfg.dimension = IntroDirector.dimId((ServerLevel) p.level());
		IntroConfig.save();
		src.sendSuccess(() -> Component.literal("Stage diset di " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " (" + cfg.dimension + "). Jalankan /laperintro build."), true);
		return 1;
	}

	private static int build(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		IntroConfig cfg = IntroConfig.get();
		ServerLevel lvl = IntroDirector.resolveLevel(src.getServer());
		if (lvl == null) {
			src.sendFailure(Component.literal("Dimensi tidak ditemukan: " + cfg.dimension));
			return 0;
		}
		StageBuilder.build(lvl, new BlockPos(cfg.stageX, cfg.stageY, cfg.stageZ));
		src.sendSuccess(() -> Component.literal("Panggung dibangun. Ruang tunggu ada " + StageBuilder.HOLD_DEPTH + " blok di bawahnya."), true);
		return 1;
	}

	private static int start(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		String err = IntroDirector.start(src.getServer());
		if (err != null) {
			src.sendFailure(Component.literal(err));
			return 0;
		}
		src.sendSuccess(() -> Component.literal("Sinematik dimulai."), true);
		return 1;
	}

	private static int stop(CommandContext<CommandSourceStack> ctx) {
		IntroDirector.stop(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.literal("Sinematik dihentikan."), true);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		IntroConfig.load();
		ctx.getSource().sendSuccess(() -> Component.literal("Config dimuat ulang."), false);
		return 1;
	}
}
