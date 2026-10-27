package tn.nightbeam.better_with_gold.client;

import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Canvas backed by the 26.x GUI render state. Float geometry goes through the 2D pose. Minecraft 26.2 and 26.2 edition. */
public final class GuiCanvas implements Canvas {
	private static final Map<String, ItemStack> ITEMS = new HashMap<>();
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();
	/** Pixel size of each GUI texture; the wheel art is 512x512. */
	private static final Map<String, Integer> SIZES = Map.of("logo", 128);
	private static final int DEFAULT_SIZE = 512;
	private final GuiGraphicsExtractor g;
	private final Matrix3x2fStack pose;
	private final Font font;

	public GuiCanvas(GuiGraphicsExtractor g) {
		this.g = g;
		this.pose = g.pose();
		this.font = Minecraft.getInstance().font;
	}

	public void finish() {
	}

	@Override
	public int width() {
		return g.guiWidth();
	}

	@Override
	public int height() {
		return g.guiHeight();
	}

	@Override
	public void fill(float x0, float y0, float x1, float y1, int argb) {
		if (x1 <= x0 || y1 <= y0 || (argb >>> 24) == 0) {
			return;
		}
		pose.pushMatrix();
		pose.translate(x0, y0);
		pose.scale(x1 - x0, y1 - y0);
		g.fill(0, 0, 1, 1, argb);
		pose.popMatrix();
	}

	@Override
	public void gradient(float x0, float y0, float x1, float y1, int top, int bottom) {
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		pose.pushMatrix();
		pose.translate(x0, y0);
		pose.scale((x1 - x0) / 64f, (y1 - y0) / 64f);
		g.fillGradient(0, 0, 64, 64, top, bottom);
		pose.popMatrix();
	}

	@Override
	public void line(float x0, float y0, float x1, float y1, float thickness, int argb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 0.01f || (argb >>> 24) == 0) {
			return;
		}
		pose.pushMatrix();
		pose.translate(x0, y0);
		pose.rotate((float) Math.atan2(dy, dx));
		pose.translate(0, -thickness / 2f);
		pose.scale(length, thickness);
		g.fill(0, 0, 1, 1, argb);
		pose.popMatrix();
	}

	@Override
	public void texture(String name, float x, float y, float w, float h, int argb) {
		Identifier id = TEXTURES.computeIfAbsent(name, n -> Compat.id(Constants.MOD_ID, "textures/gui/" + n + ".png"));
		int size = SIZES.getOrDefault(name, DEFAULT_SIZE);
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(w / size, h / size);
		g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, size, size, size, size, argb);
		pose.popMatrix();
	}

	@Override
	public void textureRotated(String name, float cx, float cy, float w, float h, float degrees, int argb) {
		Identifier id = TEXTURES.computeIfAbsent(name, n -> Compat.id(Constants.MOD_ID, "textures/gui/" + n + ".png"));
		int size = SIZES.getOrDefault(name, DEFAULT_SIZE);
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate((float) Math.toRadians(degrees));
		pose.translate(-w / 2, -h / 2);
		pose.scale(w / size, h / size);
		g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, size, size, size, size, argb);
		pose.popMatrix();
	}

	@Override
	public boolean entity(int entityId, int x0, int y0, int x1, int y1, float mouseX, float mouseY) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !(mc.level.getEntity(entityId) instanceof LivingEntity living)) {
			return false;
		}
		// as InventoryScreen.extractEntityInInventoryFollowsMouse, without the name tag
		float centerX = (x0 + x1) / 2.0F;
		float centerY = (y0 + y1) / 2.0F;
		float xAngle = (float) Math.atan((centerX - mouseX) / 40.0F);
		float yAngle = (float) Math.atan((centerY - mouseY) / 40.0F);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * (float) (Math.PI / 180.0));
		rotation.mul(xRotation);
		EntityRenderer<? super LivingEntity, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(living);
		EntityRenderState state = renderer.createRenderState(living, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		if (state instanceof LivingEntityRenderState living2) {
			living2.bodyRot = 180.0F + xAngle * 20.0F;
			living2.yRot = xAngle * 20.0F;
			living2.xRot = living2.pose != Pose.FALL_FLYING ? -yAngle * 20.0F : 0.0F;
			living2.boundingBoxWidth = living2.boundingBoxWidth / living2.scale;
			living2.boundingBoxHeight = living2.boundingBoxHeight / living2.scale;
			living2.scale = 1.0F;
		}
		int size = Math.max(8, (int) ((y1 - y0) / 2.15F));
		g.entity(state, size, new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F), rotation, xRotation, x0, y0, x1, y1);
		return true;
	}

	@Override
	public void item(String itemId, float x, float y, float size) {
		ItemStack stack = ITEMS.computeIfAbsent(itemId, Compat::stack);
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(size / 16f, size / 16f);
		g.item(stack, 0, 0);
		pose.popMatrix();
	}

	@Override
	public void text(String text, float x, float y, float scale, int argb, boolean shadow) {
		if (((argb >>> 24) & 0xFF) < 6 || text.isEmpty()) {
			return;
		}
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		g.text(font, text, 0, 0, argb, shadow);
		pose.popMatrix();
	}

	@Override
	public int textWidth(String text) {
		return font.width(text);
	}

	@Override
	public List<String> wrap(String text, int width) {
		List<String> lines = new ArrayList<>();
		for (FormattedText line : font.getSplitter().splitLines(text, Math.max(10, width), Style.EMPTY)) {
			lines.add(line.getString());
		}
		if (lines.isEmpty()) {
			lines.add("");
		}
		return lines;
	}

	@Override
	public void scissor(int x0, int y0, int x1, int y1) {
		g.enableScissor(x0, y0, x1, y1);
	}

	@Override
	public void endScissor() {
		g.disableScissor();
	}

	@Override
	public void raise() {
		g.nextStratum();
	}
}
