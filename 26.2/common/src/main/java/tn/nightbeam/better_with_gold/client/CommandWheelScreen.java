package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Hosts the shared WheelView: opens while the key is held, gives the order on release or click. Minecraft 26.2 edition. */
public class CommandWheelScreen extends Screen {
	private final WheelView view;

	public CommandWheelScreen(int focusId, String focusName) {
		super(Component.translatable("screen.better_with_gold.command_wheel"));
		view = new WheelView(focusId, focusName);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		GuiCanvas canvas = new GuiCanvas(graphics);
		view.render(canvas, mouseX, mouseY);
		canvas.finish();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (FellowshipKeys.WHEEL.matches(event)) {
			view.commit();
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		view.commit();
		onClose();
		return true;
	}
}
