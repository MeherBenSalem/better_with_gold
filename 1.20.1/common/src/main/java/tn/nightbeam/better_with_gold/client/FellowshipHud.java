package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the party frames. Minecraft 1.20.1 edition. */
public final class FellowshipHud {
	private FellowshipHud() {
	}

	public static void render(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || mc.options.renderDebug) {
			return;
		}
		GuiCanvas canvas = new GuiCanvas(graphics);
		PartyHud.render(canvas, mc.level == null ? 0 : mc.level.getGameTime());
		canvas.finish();
	}
}
