package tn.nightbeam.better_with_gold.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Hosts the shared RosterView. Minecraft 26.3 edition. */
public class RosterScreen extends Screen implements RosterView.Host {
	private final RosterView view;

	public RosterScreen(String allyId) {
		super(Component.translatable("screen.better_with_gold.roster"));
		view = new RosterView(this, allyId);
	}

	@Override
	public void tick() {
		if (minecraft.level != null) {
			view.tick(minecraft.level.getGameTime());
		}
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return view.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return view.mouseScrolled(mouseX, mouseY, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (FellowshipKeys.ROSTER.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public void close() {
		onClose();
	}
}
