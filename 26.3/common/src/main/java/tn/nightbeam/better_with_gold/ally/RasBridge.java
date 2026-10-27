package tn.nightbeam.better_with_gold.ally;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.platform.Services;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Optional link to Remnants | RPG Stats, Skills &amp; Progression (mod id rpg_attribute_system), reached through its
 * public RasApi by reflection, so neither mod needs the other. With it installed, allies cannot out-level their
 * leader's RPG level.
 */
public final class RasBridge {
	public static final String MOD_ID = "rpg_attribute_system";
	private static Boolean present;
	private static Method getLevel;

	private RasBridge() {
	}

	public static boolean isLoaded() {
		if (present == null) {
			present = false;
			try {
				if (Services.PLATFORM.isModLoaded(MOD_ID)) {
					Class<?> api = Class.forName("tn.nightbeam.ras.api.RasApi");
					getLevel = api.getMethod("getLevel", Player.class);
					present = true;
					Constants.LOG.info("RPG Attribute System found: allies are capped at their leader's RPG level");
				}
			} catch (ReflectiveOperationException | LinkageError ex) {
				Constants.LOG.warn("RPG Attribute System is installed but its API could not be reached; integration disabled", ex);
			}
		}
		return present;
	}

	/** The player's RPG level, or 0 when the mod is absent or the call fails. Server side only. */
	public static int level(Player player) {
		if (!isLoaded() || player == null) {
			return 0;
		}
		try {
			Object value = getLevel.invoke(null, player);
			return value instanceof Number number ? number.intValue() : 0;
		} catch (ReflectiveOperationException | RuntimeException ex) {
			Constants.LOG.debug("RasApi.getLevel failed", ex);
			return 0;
		}
	}
}
