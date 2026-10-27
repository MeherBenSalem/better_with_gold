package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Key bindings: hold G for the command wheel, U for the roster. Minecraft 1.21.1 edition. */
public final class FellowshipKeys {
	public static final String CATEGORY = "key.categories.better_with_gold";
	public static final KeyMapping WHEEL = new KeyMapping("key.better_with_gold.command_wheel", GLFW.GLFW_KEY_G, CATEGORY);
	public static final KeyMapping ROSTER = new KeyMapping("key.better_with_gold.roster", GLFW.GLFW_KEY_U, CATEGORY);

	private FellowshipKeys() {
	}

	public static List<KeyMapping> all() {
		return List.of(WHEEL, ROSTER);
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		while (WHEEL.consumeClick()) {
			if (minecraft.screen == null) {
				Entity target = minecraft.crosshairPickEntity;
				minecraft.setScreen(new CommandWheelScreen(target == null ? -1 : target.getId(), target == null ? "" : target.getName().getString()));
			}
		}
		while (ROSTER.consumeClick()) {
			if (minecraft.screen == null) {
				ScreenOpener.openRoster("");
			}
		}
	}
}
