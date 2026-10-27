package tn.nightbeam.better_with_gold.client;

import java.util.List;

/**
 * Drawing operations used by the skill tree screen and the HUD. Each Minecraft version provides its own
 * implementation, so the screen logic itself is shared. Colors are ARGB.
 */
public interface Canvas {
	int width();

	int height();

	void fill(float x0, float y0, float x1, float y1, int argb);

	void gradient(float x0, float y0, float x1, float y1, int top, int bottom);

	void line(float x0, float y0, float x1, float y1, float thickness, int argb);

	/** Draws a whole texture from assets/better_with_gold/textures/gui/{name}.png, tinted. */
	void texture(String name, float x, float y, float w, float h, int argb);

	void item(String itemId, float x, float y, float size);

	void text(String text, float x, float y, float scale, int argb, boolean shadow);

	int textWidth(String text);

	List<String> wrap(String text, int width);

	void scissor(int x0, int y0, int x1, int y1);

	void endScissor();

	/** Everything drawn after this call appears above items drawn before it. */
	void raise();

	default void text(String text, float x, float y, int argb) {
		text(text, x, y, 1, argb, true);
	}

	default void centered(String text, float x, float y, float scale, int argb, boolean shadow) {
		text(text, x - textWidth(text) * scale / 2f, y, scale, argb, shadow);
	}

	default void rect(float x0, float y0, float x1, float y1, int border) {
		fill(x0, y0, x1, y0 + 1, border);
		fill(x0, y1 - 1, x1, y1, border);
		fill(x0, y0 + 1, x0 + 1, y1 - 1, border);
		fill(x1 - 1, y0 + 1, x1, y1 - 1, border);
	}

	/** A dark panel with a thin colored border. */
	default void panel(float x0, float y0, float x1, float y1, int border) {
		fill(x0, y0, x1, y1, 0xE60B0C14);
		rect(x0, y0, x1, y1, border);
		fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x22FFFFFF);
	}

	static int alpha(int rgb, float alpha) {
		return ((int) (Math.max(0, Math.min(1, alpha)) * 255) << 24) | (rgb & 0xFFFFFF);
	}

	static int mix(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}
}
