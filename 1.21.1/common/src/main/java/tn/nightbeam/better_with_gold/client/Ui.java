package tn.nightbeam.better_with_gold.client;

import java.util.List;

/**
 * The Remnants look shared by every Fellowship screen: deep navy glass, gold double borders with corner ornaments,
 * gradient bars with segment ticks, bevelled buttons and gold-framed tooltips. Everything is drawn on a Canvas, so
 * it is the same on every Minecraft version.
 */
public final class Ui {
	public static final int GOLD = 0xFFE8B63E;
	public static final int GOLD_HI = 0xFFFFE89A;
	public static final int GOLD_LO = 0xFFA8782A;
	public static final int TEXT = 0xFFEDE8DA;
	public static final int DIM = 0xFFA6A2B4;
	public static final int MUTED = 0xFF706C82;
	public static final int GREEN = 0xFF56C86F;
	public static final int RED = 0xFFE5534B;
	public static final int BLUE = 0xFF5B8DEF;
	public static final int AMBER = 0xFFE0A93A;
	public static final int PANEL_TOP = 0x161A2A;
	public static final int PANEL_BOTTOM = 0x0A0B13;

	private Ui() {
	}

	public static float clamp01(float t) {
		return Math.max(0, Math.min(1, t));
	}

	/** Ease-out cubic. */
	public static float ease(float t) {
		float u = 1 - clamp01(t);
		return 1 - u * u * u;
	}

	/** Multiplies the alpha of an ARGB colour. */
	public static int fade(int argb, float alpha) {
		int a = (int) (((argb >>> 24) & 0xFF) * clamp01(alpha));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	/** A slow 0..1 pulse for highlights. */
	public static float pulse(float periodSeconds) {
		double t = (System.nanoTime() / 1e9) % periodSeconds / periodSeconds;
		return (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
	}

	/** The big panel: drop shadow, gradient glass, dark outline, gold double border and corner ornaments. */
	public static void frame(Canvas c, float x0, float y0, float x1, float y1, float alpha) {
		c.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 4, fade(0x70000000, alpha));
		c.gradient(x0, y0, x1, y1, Canvas.alpha(PANEL_TOP, 0.95F * alpha), Canvas.alpha(PANEL_BOTTOM, 0.97F * alpha));
		c.rect(x0 - 1, y0 - 1, x1 + 1, y1 + 1, fade(0xFF050507, alpha));
		c.rect(x0, y0, x1, y1, fade(GOLD_LO, alpha));
		c.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, fade(0x55C8A04A, alpha));
		c.fill(x0 + 3, y0 + 3, x1 - 3, y0 + 3.5F, fade(0x22FFFFFF, alpha));
		corner(c, x0, y0, 1, 1, alpha);
		corner(c, x1, y0, -1, 1, alpha);
		corner(c, x0, y1, 1, -1, alpha);
		corner(c, x1, y1, -1, -1, alpha);
	}

	/** A smaller inset box (portrait, list, chronicle). */
	public static void inset(Canvas c, float x0, float y0, float x1, float y1, int border, float alpha) {
		c.fill(x0, y0, x1, y1, fade(0x66000000, alpha));
		c.rect(x0, y0, x1, y1, fade(border, alpha));
		c.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 1.5F, fade(0x14FFFFFF, alpha));
	}

	private static void corner(Canvas c, float x, float y, int sx, int sy, float alpha) {
		int gold = fade(GOLD, alpha);
		float len = 9;
		float ox = x + sx * 1.5F;
		float oy = y + sy * 1.5F;
		c.fill(Math.min(ox, ox + sx * len), Math.min(oy, oy + sy * 1), Math.max(ox, ox + sx * len), Math.max(oy, oy + sy * 1), gold);
		c.fill(Math.min(ox, ox + sx * 1), Math.min(oy, oy + sy * len), Math.max(ox, ox + sx * 1), Math.max(oy, oy + sy * len), gold);
		diamond(c, x, y, 2, 1, fade(GOLD_HI, alpha));
	}

	/** A pixel diamond centred on (cx, cy): {@code n} pixels from centre to tip, each {@code px} units. */
	public static void diamond(Canvas c, float cx, float cy, int n, float px, int argb) {
		for (int j = -n; j <= n; j++) {
			float half = (n - Math.abs(j) + 0.5F) * px;
			c.fill(cx - half, cy + j * px - px / 2, cx + half, cy + j * px + px / 2, argb);
		}
	}

	/** A gold rule fading out at both ends, with a diamond in the middle. */
	public static void rule(Canvas c, float x0, float x1, float y, float alpha) {
		float mid = (x0 + x1) / 2;
		int steps = 12;
		for (int i = 0; i < steps; i++) {
			float a = (i + 1) / (float) steps;
			float l0 = x0 + (mid - x0) * i / steps;
			float l1 = x0 + (mid - x0) * (i + 1) / steps;
			c.fill(l0, y, l1, y + 0.5F, fade(GOLD_LO, a * alpha));
			float r0 = x1 - (x1 - mid) * (i + 1) / steps;
			float r1 = x1 - (x1 - mid) * i / steps;
			c.fill(r0, y, r1, y + 0.5F, fade(GOLD_LO, a * alpha));
		}
		diamond(c, mid, y + 0.25F, 2, 0.75F, fade(GOLD, alpha));
	}

	/**
	 * A bar: dark trough, gradient fill with a shine, an optional pale trail behind the fill (recent damage) and a
	 * tick every {@code segments}-th of the length.
	 */
	public static void bar(Canvas c, float x, float y, float w, float h, float frac, float trail, int color, int segments, float alpha) {
		frac = clamp01(frac);
		c.fill(x - 0.5F, y - 0.5F, x + w + 0.5F, y + h + 0.5F, fade(0xFF050507, alpha));
		c.gradient(x, y, x + w, y + h, fade(0xFF1C1E2A, alpha), fade(0xFF101119, alpha));
		if (trail > frac) {
			c.fill(x + w * frac, y, x + w * clamp01(trail), y + h, fade(opaque(Canvas.mix(color, 0xFFFFFF, 0.55F)), 0.75F * alpha));
		}
		if (frac > 0) {
			int top = opaque(Canvas.mix(color, 0xFFFFFF, 0.28F));
			int bottom = opaque(Canvas.mix(color, 0x000000, 0.3F));
			c.gradient(x, y, x + w * frac, y + h, fade(top, alpha), fade(bottom, alpha));
			c.fill(x, y, x + w * frac, y + Math.max(0.5F, h * 0.3F), fade(0x38FFFFFF, alpha));
		}
		if (segments > 1 && w / segments > 2.5F) {
			for (int k = 1; k < segments; k++) {
				float t = x + w * k / segments;
				c.fill(t - 0.25F, y, t + 0.25F, y + h, fade(0x70000000, alpha));
			}
		}
	}

	/** A bevelled button. Returns the box it occupies: {x0, y0, x1, y1}. */
	public static float[] button(Canvas c, float x, float y, String label, boolean hover, boolean danger, float alpha) {
		float scale = 0.7F;
		float w = c.textWidth(label) * scale + 16;
		float h = 13;
		int edge = danger ? 0xFFB8453E : GOLD;
		c.fill(x - 0.5F, y - 0.5F, x + w + 0.5F, y + h + 0.5F, fade(0xFF050507, alpha));
		c.gradient(x, y, x + w, y + h, fade(hover ? (danger ? 0xFF4A1A18 : 0xFF4A3812) : 0xFF22202A, alpha),
				fade(hover ? (danger ? 0xFF2A0E0C : 0xFF2A1E08) : 0xFF14131A, alpha));
		c.rect(x, y, x + w, y + h, fade(hover ? (danger ? 0xFFFF6B5A : GOLD_HI) : edge, alpha));
		c.fill(x + 1, y + 1, x + w - 1, y + 1.5F, fade(0x30FFFFFF, alpha));
		c.centered(label, x + w / 2, y + 3.4F, scale, fade(danger ? (hover ? 0xFFFFD0C8 : 0xFFFF9A8E) : (hover ? GOLD_HI : GOLD), alpha), true);
		return new float[] {x, y, x + w, y + h};
	}

	/** A small labelled chip; returns its right edge. */
	public static float chip(Canvas c, float x, float y, String label, int color, boolean hover, float alpha) {
		float scale = 0.58F;
		float w = c.textWidth(label) * scale + 8;
		c.fill(x, y, x + w, y + 10, fade(hover ? 0xFF2A2638 : 0xFF181622, alpha));
		c.rect(x, y, x + w, y + 10, fade(opaque(Canvas.mix(color, 0x000000, hover ? 0 : 0.25F)), alpha));
		c.text(label, x + 4, y + 2.6F, scale, fade(opaque(Canvas.mix(color, 0xFFFFFF, 0.35F)), alpha), true);
		return x + w;
	}

	/** A gold-framed tooltip near the mouse, kept on screen. */
	public static void tooltip(Canvas c, float mx, float my, String title, String body, int titleColor) {
		float scale = 0.62F;
		float maxW = 170;
		List<String> lines = c.wrap(body, (int) (maxW / scale));
		float w = Math.max(c.textWidth(title) * 0.7F, 0);
		for (String line : lines) {
			w = Math.max(w, c.textWidth(line) * scale);
		}
		w += 12;
		float h = 14 + lines.size() * 8;
		float x = Math.min(mx + 10, c.width() - w - 4);
		float y = Math.min(my + 10, c.height() - h - 4);
		c.raise();
		c.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x80000000);
		c.gradient(x, y, x + w, y + h, 0xF41A1E30, 0xF40C0D16);
		c.rect(x, y, x + w, y + h, GOLD_LO);
		c.text(title, x + 6, y + 4, 0.7F, titleColor, true);
		float ly = y + 13;
		for (String line : lines) {
			c.text(line, x + 6, ly, scale, DIM, true);
			ly += 8;
		}
	}

	/** An emblem tile: dark square with a class-coloured border and the role icon. */
	public static void emblem(Canvas c, float x, float y, float size, String role, int color, float alpha) {
		c.fill(x - 0.5F, y - 0.5F, x + size + 0.5F, y + size + 0.5F, fade(0xFF050507, alpha));
		c.gradient(x, y, x + size, y + size, fade(opaque(Canvas.mix(color, 0x000000, 0.72F)), alpha), fade(0xFF0C0C12, alpha));
		c.rect(x, y, x + size, y + size, fade(opaque(color), alpha));
		float pad = size * 0.12F;
		Icons.draw(c, Icons.role(role), x + pad, y + pad, (size - 2 * pad) / 16F, color, alpha);
	}
}
