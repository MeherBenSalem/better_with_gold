package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.Minecraft;

/** Opens the Fellowship screens. Minecraft 26.2 edition. */
public final class ScreenOpener {
	private ScreenOpener() {
	}

	public static void openRoster(String allyId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && (mc.gui.screen() == null || mc.gui.screen() instanceof RosterScreen || mc.gui.screen() instanceof CommandWheelScreen)) {
			mc.gui.setScreen(new RosterScreen(allyId));
		}
	}
}
