package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Hosts the shared RosterView. Minecraft 1.20.1 edition. */
public class RosterScreen extends Screen implements RosterView.Host {
	private final RosterView view;

	public RosterScreen(String allyId) {
		super(Component.translatable("screen.better_with_gold.roster"));
		view = new RosterView(this, allyId);
	}

	@Override
	public void tick() {
		if (minecraft != null && minecraft.level != null) {
			view.tick(minecraft.level.getGameTime());
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		GuiCanvas canvas = new GuiCanvas(graphics);
		view.render(canvas, mouseX, mouseY);
		canvas.finish();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return view.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return view.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (FellowshipKeys.ROSTER.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void click() {
		if (minecraft != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	@Override
	public void close() {
		onClose();
	}
}
