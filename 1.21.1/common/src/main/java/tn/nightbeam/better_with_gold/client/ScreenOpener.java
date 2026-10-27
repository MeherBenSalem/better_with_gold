package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.Minecraft;

/** Opens the Fellowship screens. Minecraft 1.21.1 edition. */
public final class ScreenOpener {
	private ScreenOpener() {
	}

	public static void openRoster(String allyId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && (mc.screen == null || mc.screen instanceof RosterScreen || mc.screen instanceof CommandWheelScreen)) {
			mc.setScreen(new RosterScreen(allyId));
		}
	}
}
