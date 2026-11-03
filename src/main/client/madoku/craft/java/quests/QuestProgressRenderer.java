package madoku.craft.java.quests;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws the quest progress indicator directly as colored GUI rectangles. */
final class QuestProgressRenderer {
	static final int BAR_WIDTH = 78;
	static final int BAR_HEIGHT = 2;
	private static final int INCOMPLETE_COLOR = 0xFFD42B2B;
	private static final int COMPLETE_COLOR = 0xFF3EAC4A;

	private QuestProgressRenderer() {
	}

	static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int progress, int required) {
		if (graphics == null || width <= 0 || height <= 0) return;
		int safeRequired = Math.max(1, required);
		int safeProgress = Math.max(0, Math.min(progress, safeRequired));
		int completedWidth = Math.round(width * safeProgress / (float) safeRequired);
		graphics.fill(RenderPipelines.GUI, x, y, x + width, y + height, INCOMPLETE_COLOR);
		if (completedWidth > 0) {
			graphics.fill(RenderPipelines.GUI, x, y, x + completedWidth, y + height, COMPLETE_COLOR);
		}
	}
}
