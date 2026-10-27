package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hosts the shared WheelView: opens while the key is held, gives the order on release or click. Minecraft 1.21.1 edition. */
public class CommandWheelScreen extends Screen {
	private final WheelView view;

	public CommandWheelScreen(int focusId, String focusName) {
		super(Component.translatable("screen.better_with_gold.command_wheel"));
		view = new WheelView(focusId, focusName);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		GuiCanvas canvas = new GuiCanvas(graphics);
		view.render(canvas, mouseX, mouseY);
		canvas.finish();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (FellowshipKeys.WHEEL.matches(keyCode, scanCode)) {
			view.commit();
			onClose();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		view.commit();
		onClose();
		return true;
	}
}
