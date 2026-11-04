package com.laperintro.client;

import com.laperintro.net.HudPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/** Overlay sinematik: judul, nameplate kuning, nama di atas, subtitle kutipan, fade hitam. */
final class IntroHud {
	private static final int GOLD = 0xFFFFAA00;
	private static final int GOLD_DIM = 0xFF8C7A3A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFB0B0B0;

	private static int mode = HudPayload.NONE;
	private static int index;
	private static int total;
	private static String name = "";
	private static String text = "";
	private static String titleText = "LAPER HARDCORE";
	private static long durMs;
	private static long startMs;

	private IntroHud() {}

	static boolean isCinematic() {
		return mode != HudPayload.NONE;
	}

	static void update(HudPayload p) {
		if (p.mode() == HudPayload.TITLE || p.mode() == HudPayload.FINALE) {
			if (!p.name().isEmpty()) titleText = p.name();
		}
		mode = p.mode();
		if (p.mode() != HudPayload.FADE || startMs == 0) {
			index = p.index();
			total = p.total();
			name = p.name();
			text = p.text();
		}
		durMs = p.durationTicks() * 50L;
		startMs = System.currentTimeMillis();
	}

	static void reset() {
		mode = HudPayload.NONE;
		startMs = 0;
	}

	private static double ease(double t) {
		t = Mth.clamp(t, 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	private static int alpha(double a, int rgb) {
		return ((int) (Mth.clamp(a, 0.0, 1.0) * 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static void scaled(GuiGraphicsExtractor g, Font font, String s, float x, float y, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale);
		g.text(font, Component.literal(s), 0, 0, color, true);
		g.pose().popMatrix();
	}

	private static void centered(GuiGraphicsExtractor g, Font font, String s, float cx, float y, float scale, int color) {
		float w = font.width(Component.literal(s)) * scale;
		scaled(g, font, s, cx - w / 2f, y, scale, color);
	}

	static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
		if (mode == HudPayload.NONE) return;
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int w = g.guiWidth();
		int h = g.guiHeight();
		long el = System.currentTimeMillis() - startMs;

		switch (mode) {
			case HudPayload.TITLE -> {
				double a = Math.min(ease(el / 500.0), ease((durMs - el) / 600.0));
				drawTitle(g, font, w, h * 0.12f, a, text);
			}
			case HudPayload.FINALE, HudPayload.FADE -> {
				double a = mode == HudPayload.FADE ? 1.0 : ease(el / 800.0);
				drawTitle(g, font, w, h * 0.04f, a, "");
				if (mode == HudPayload.FADE) {
					double f = durMs <= 0 ? 1.0 : (double) el / durMs;
					g.fill(0, 0, w, h, alpha(f, 0x000000));
				}
			}
			case HudPayload.MYSTERY -> {
				String label = String.format(Locale.ROOT, "[%02d] ???", index);
				centered(g, font, label, w / 2f, h * 0.04f, 2.2f, alpha(ease(el / 250.0), GOLD_DIM));
			}
			case HudPayload.LAST_INTRO -> {
				int chars = (int) Math.min(text.length(), el / 70);
				centered(g, font, text.substring(0, chars), w / 2f, h * 0.25f, 1.6f, WHITE);
			}
			case HudPayload.MEMBER -> drawMember(g, font, w, h, el);
			default -> { }
		}
	}

	private static void drawTitle(GuiGraphicsExtractor g, Font font, int w, float y, double a, String subtitle) {
		centered(g, font, titleText, w / 2f, y, 3.0f, alpha(a, GOLD));
		float tw = font.width(Component.literal(titleText)) * 3.0f;
		int ly = (int) (y + 3.0f * 9 + 3);
		g.fill((int) (w / 2f - tw / 2f), ly, (int) (w / 2f + tw / 2f), ly + 1, alpha(a, GOLD));
		if (!subtitle.isEmpty()) centered(g, font, subtitle, w / 2f, ly + 6, 1.0f, alpha(a, 0xFFFFFF));
	}

	private static void drawMember(GuiGraphicsExtractor g, Font font, int w, int h, long el) {
		boolean last = index == total && total > 1;

		// nama besar di tengah atas
		String top = String.format(Locale.ROOT, "[%02d] %s", index, name);
		centered(g, font, top, w / 2f, h * 0.04f, 2.2f, alpha(ease(el / 250.0), GOLD));

		// nameplate kuning meluncur dari kiri
		final int pw = 172, ph = 46;
		float px = Mth.lerp((float) ease(el / 350.0), -pw - 4, w * 0.07f);
		int x = (int) px;
		int y = (int) (h * 0.60f);
		g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, GOLD);
		g.fill(x, y, x + pw, y + ph, 0xF0121212);
		g.fill(x, y, x + 3, y + ph, GOLD);
		scaled(g, font, String.format(Locale.ROOT, "#%02d %s", index, name), x + 9, y + 6, 1.35f, WHITE);
		scaled(g, font, titleText, x + 9, y + 25, 0.75f, GOLD);
		String sub = last ? "THE LAST MEMBER" : String.format(Locale.ROOT, "MEMBER %02d OF %02d", index, total);
		scaled(g, font, sub, x + 9, y + 35, 0.75f, GRAY);

		// subtitle kutipan di bawah
		if (!text.isEmpty() && el > 700) {
			double a = ease((el - 700) / 300.0);
			float tw = font.width(Component.literal(text));
			int bx = (int) (w / 2f - tw / 2f);
			int by = h - 34;
			g.fill(bx - 5, by - 3, bx + (int) tw + 5, by + 11, alpha(a * 0.63, 0x000000));
			g.text(font, Component.literal(text), bx, by, alpha(a, 0xFFFFFF), true);
		}
	}
}
