package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** Key bindings: hold G for the command wheel, U for the roster. Minecraft 26.2 edition. */
public final class FellowshipKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Compat.id(Constants.MOD_ID, "fellowship"));
	public static final KeyMapping WHEEL = new KeyMapping("key.better_with_gold.command_wheel", InputConstants.KEY_G, CATEGORY);
	public static final KeyMapping ROSTER = new KeyMapping("key.better_with_gold.roster", InputConstants.KEY_U, CATEGORY);

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
			if (minecraft.gui.screen() == null) {
				Entity target = minecraft.crosshairPickEntity;
				minecraft.gui.setScreen(new CommandWheelScreen(target == null ? -1 : target.getId(), target == null ? "" : target.getName().getString()));
			}
		}
		while (ROSTER.consumeClick()) {
			if (minecraft.gui.screen() == null) {
				ScreenOpener.openRoster("");
			}
		}
	}
}
