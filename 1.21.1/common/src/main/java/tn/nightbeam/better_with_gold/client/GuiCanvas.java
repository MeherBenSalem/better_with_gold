package tn.nightbeam.better_with_gold.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import tn.nightbeam.better_with_gold.Constants;
import tn.nightbeam.better_with_gold.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Canvas backed by GuiGraphics. Runs of fills and lines are batched into one draw. Minecraft 1.21.1 edition. */
public final class GuiCanvas implements Canvas {
	private static final Map<String, ItemStack> ITEMS = new HashMap<>();
	private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
	private static final Map<String, Integer> SIZES = Map.of("logo", 128);
	private final GuiGraphics g;
	private final Font font;
	private BufferBuilder batch;

	public GuiCanvas(GuiGraphics g) {
		this.g = g;
		this.font = Minecraft.getInstance().font;
		g.pose().pushPose();
	}

	public void finish() {
		flush();
		g.pose().popPose();
	}

	@Override
	public int width() {
		return g.guiWidth();
	}

	@Override
	public int height() {
		return g.guiHeight();
	}

	private BufferBuilder batch() {
		if (batch == null) {
			batch = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		}
		return batch;
	}

	private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c0, int c1, int c2, int c3) {
		Matrix4f m = g.pose().last().pose();
		BufferBuilder b = batch();
		b.addVertex(m, x0, y0, 0).setColor(c0);
		b.addVertex(m, x1, y1, 0).setColor(c1);
		b.addVertex(m, x2, y2, 0).setColor(c2);
		b.addVertex(m, x3, y3, 0).setColor(c3);
	}

	private void flush() {
		if (batch == null) {
			return;
		}
		MeshData mesh = batch.build();
		batch = null;
		if (mesh != null) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			BufferUploader.drawWithShader(mesh);
			RenderSystem.enableCull();
		}
	}

	@Override
	public void fill(float x0, float y0, float x1, float y1, int argb) {
		quad(x0, y0, x0, y1, x1, y1, x1, y0, argb, argb, argb, argb);
	}

	@Override
	public void gradient(float x0, float y0, float x1, float y1, int top, int bottom) {
		quad(x0, y0, x0, y1, x1, y1, x1, y0, top, bottom, bottom, top);
	}

	@Override
	public void line(float x0, float y0, float x1, float y1, float thickness, int argb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 0.01f) {
			return;
		}
		float nx = -dy / length * thickness / 2;
		float ny = dx / length * thickness / 2;
		quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, argb, argb, argb, argb);
	}

	@Override
	public void texture(String name, float x, float y, float w, float h, int argb) {
		flush();
		ResourceLocation id = TEXTURES.computeIfAbsent(name, n -> Compat.id(Constants.MOD_ID, "textures/gui/" + n + ".png"));
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		int size = SIZES.getOrDefault(name, 128);
		g.pose().scale(w / (float) size, h / (float) size, 1);
		g.blit(id, 0, 0, 0, 0, size, size, size, size);
		g.pose().popPose();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	@Override
	public void item(String itemId, float x, float y, float size) {
		flush();
		ItemStack stack = ITEMS.computeIfAbsent(itemId, Compat::stack);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(size / 16f, size / 16f, 1);
		g.renderItem(stack, 0, 0);
		g.pose().popPose();
	}

	@Override
	public void text(String text, float x, float y, float scale, int argb, boolean shadow) {
		if (((argb >>> 24) & 0xFF) < 6 || text.isEmpty()) {
			return;
		}
		flush();
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1);
		g.drawString(font, text, 0, 0, argb, shadow);
		g.pose().popPose();
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
		flush();
		g.enableScissor(x0, y0, x1, y1);
	}

	@Override
	public void endScissor() {
		flush();
		g.disableScissor();
	}

	@Override
	public void raise() {
		flush();
		g.pose().translate(0, 0, 250);
	}
}
