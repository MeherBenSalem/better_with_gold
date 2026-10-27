package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws the party frames. Minecraft 26.3 edition. */
public final class FellowshipHud {
	private FellowshipHud() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) {
			return;
		}
		GuiCanvas canvas = new GuiCanvas(graphics);
		PartyHud.render(canvas, mc.level.getGameTime());
		canvas.finish();
	}
}
