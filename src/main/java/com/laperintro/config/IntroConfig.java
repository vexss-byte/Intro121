package com.laperintro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.laperintro.LaperIntro;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Konfigurasi di config/laperintro.json. Semua teks dan daftar member bisa diedit tanpa compile ulang. */
public final class IntroConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static IntroConfig instance = new IntroConfig();

	public String title = "LAPER HARDCORE";
	public String subtitle = "PERKENALAN MEMBER";
	/** Dimensi tempat panggung, contoh "minecraft:overworld" atau "minecraft:the_end". */
	public String dimension = "minecraft:overworld";
	/** Posisi kaki player di atas pad emas. */
	public int stageX = 0;
	public int stageY = 150;
	public int stageZ = 0;
	public int secondsPerMember = 10;
	public int mysterySeconds = 4;
	/** Nama player yang boleh memakai command di server. Kosong = hanya console (dan singleplayer). */
	public List<String> admins = new ArrayList<>();
	/** Urutan tampil. Player online yang tidak terdaftar tampil setelahnya, urut abjad. */
	public List<MemberEntry> members = new ArrayList<>();

	public static final class MemberEntry {
		public String name = "";
		public String quote = "";
		/** Salah satu: pose_sword_up, pose_arms_wide, pose_point, pose_wave, pose_flex, pose_cheer, pose_stance, pose_hold_forward. Kosong = otomatis. */
		public String pose = "";
		/** true = muncul dulu sebagai siluet "???" sebelum diperkenalkan. */
		public boolean mystery = false;

		public MemberEntry() {}

		public MemberEntry(String name, String quote, String pose, boolean mystery) {
			this.name = name;
			this.quote = quote;
			this.pose = pose;
			this.mystery = mystery;
		}
	}

	public static IntroConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("laperintro.json");
	}

	public static void load() {
		Path p = path();
		if (Files.exists(p)) {
			try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				IntroConfig loaded = GSON.fromJson(r, IntroConfig.class);
				if (loaded != null) {
					instance = loaded;
					return;
				}
			} catch (IOException | RuntimeException e) {
				LaperIntro.LOGGER.error("Gagal membaca {}; memakai default.", p, e);
			}
		}
		instance = new IntroConfig();
		instance.members.add(new MemberEntry("NamaPlayer1", "Contoh kutipan member pertama", "pose_sword_up", false));
		instance.members.add(new MemberEntry("NamaPlayer2", "Contoh kutipan member kedua", "pose_wave", false));
		instance.members.add(new MemberEntry("NamaPlayer3", "Member ini muncul sebagai siluet dulu", "pose_arms_wide", true));
		save();
	}

	public static void save() {
		Path p = path();
		try {
			Files.createDirectories(p.getParent());
			try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (IOException e) {
			LaperIntro.LOGGER.error("Gagal menulis {}", p, e);
		}
	}
}
