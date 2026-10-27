package tn.nightbeam.better_with_gold.net;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;

import net.minecraft.resources.ResourceLocation;

/** The Fellowship channels: one JSON string per packet. Minecraft 1.20.1 edition. */
public final class FabricChannels {
	public static final ResourceLocation TO_SERVER = Compat.id(Constants.MOD_ID, "c2s");
	public static final ResourceLocation TO_CLIENT = Compat.id(Constants.MOD_ID, "s2c");

	private FabricChannels() {
	}
}
