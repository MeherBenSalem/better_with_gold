package tn.nightbeam.better_with_gold.mixin;

import tn.nightbeam.better_with_gold.client.FellowshipHud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 1.21.1 has no event to add a HUD layer, so the party frames draw after the vanilla HUD. */
@Mixin(Gui.class)
abstract class GuiMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void better_with_gold$partyFrames(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		FellowshipHud.render(graphics);
	}
}
