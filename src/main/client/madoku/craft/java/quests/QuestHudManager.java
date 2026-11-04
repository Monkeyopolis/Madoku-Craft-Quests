package madoku.craft.java.quests;

import java.util.List;

import madoku.craft.java.core.ui.UiLayoutAPIManager;
import madoku.craft.java.core.ui.UiLayoutElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Renders the globally selected quest during gameplay and participates in the Alt-held layout editor. */
final class QuestHudManager {
	private static final Identifier QUEST_HUD_LAYOUT_ID = Identifier.fromNamespaceAndPath("madoku-craft", "selected-quest-hud");
	private static final Identifier QUEST_HUD_COMPACT_TEXTURE = texture("madoku-menu/quests-menu/selected-quest-compact-hud.png");
	private static final Identifier QUEST_HUD_FULL_TEXTURE = texture("madoku-menu/quests-menu/selected-quest-full-hud.png");
	private static final int HUD_WIDTH = 78;
	private static final int HUD_COMPACT_HEIGHT = 40;
	private static final int HUD_FULL_HEIGHT = 60;
	private static final int DEFAULT_RIGHT_INSET = 4;
	private static final int DEFAULT_VERTICAL_DIVISOR = 4;
	private static final int TITLE_SEPARATOR_Y = 13;
	private static final int TITLE_MAX_WIDTH = 78;
	private static final int TEXT_GAP = 2;
	private static final float TEXT_SCALE = 0.7F;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static int cachedDisplayVersion = Integer.MIN_VALUE;
	private static String cachedQuestTitle;
	private static boolean initialized;
	private static final UiLayoutElement QUEST_HUD_LAYOUT = new UiLayoutElement() {
		@Override public Identifier id() { return QUEST_HUD_LAYOUT_ID; }
		@Override public int defaultX() { return 0; }
		@Override public int defaultY() { return 0; }
		@Override public int defaultX(Minecraft client) {
			return client == null || client.getWindow() == null
				? 0
				: Math.max(0, client.getWindow().getGuiScaledWidth() - HUD_WIDTH - DEFAULT_RIGHT_INSET);
		}
		@Override public int defaultY(Minecraft client) {
			return client == null || client.getWindow() == null
				? 0
				: Math.max(0, client.getWindow().getGuiScaledHeight() / DEFAULT_VERTICAL_DIVISOR);
		}
		@Override public Bounds bounds(Minecraft client, int x, int y) {
			return new Bounds(x, y, HUD_WIDTH, hudHeight(MadokuQuestsClient.selectedQuestEntry()));
		}
		@Override public boolean isEnabled(Minecraft client) { return hasRenderablePlayer(client, true); }
		@Override public boolean isVisible(Minecraft client) { return selectedQuestTitle() != null; }
		@Override public void renderEditor(GuiGraphicsExtractor context, Minecraft client, int x, int y) {
			renderQuestHud(context, client, x, y, true);
		}
		@Override public String displayName() { return "Selected Quest HUD"; }
	};

	private QuestHudManager() {
	}

	static void initialize() {
		if (initialized) return;
		UiLayoutAPIManager.registerElement(QUEST_HUD_LAYOUT);
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.MISC_OVERLAYS,
			Identifier.fromNamespaceAndPath("madoku-craft", "selected-quest-hud"),
			QuestHudManager::renderQuestHud
		);
		initialized = true;
	}

	private static void renderQuestHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (UiLayoutAPIManager.isEditMode()) return;
		UiLayoutAPIManager.Position position = UiLayoutAPIManager.position(QUEST_HUD_LAYOUT);
		renderQuestHud(context, client, position.x(), position.y(), false);
	}

	private static void renderQuestHud(GuiGraphicsExtractor context, Minecraft client, int x, int y, boolean editing) {
		if (!hasRenderablePlayer(client, editing)) return;
		String questTitle = selectedQuestTitle();
		if (questTitle == null) return;
		MadokuQuestsClient.QuestEntry entry = MadokuQuestsClient.selectedQuestEntry();
		if (entry == null) return;
		List<MadokuQuestsClient.QuestObjectiveEntry> objectives = entry.objectives();
		int objectiveCount = Math.min(3, objectives.size());
		int hudHeight = hudHeight(entry);

		context.blit(
			RenderPipelines.GUI_TEXTURED,
			objectiveCount > 1 ? QUEST_HUD_FULL_TEXTURE : QUEST_HUD_COMPACT_TEXTURE,
			x,
			y,
			0.0F,
			0.0F,
			HUD_WIDTH,
			hudHeight,
			HUD_WIDTH,
			hudHeight
		);
		float titleScale = fittedTextScale(client, questTitle, TEXT_SCALE, TITLE_MAX_WIDTH);
		drawScaledCenteredText(
			context,
			client,
			questTitle,
			x + HUD_WIDTH / 2,
			y + TITLE_SEPARATOR_Y - TEXT_GAP - scaledTextHeight(client, titleScale),
			titleScale
		);
		float objectiveTextScale = objectiveTextScale(objectiveCount);
		float progressTextScale = objectiveProgressTextScale(objectiveCount);
		int barWidth = objectiveBarWidth(objectiveCount);
		int barHeight = objectiveBarHeight(objectiveCount);
		int rowY = y + objectiveStartY(objectiveCount);
		for (int objectiveIndex = 0; objectiveIndex < objectiveCount; objectiveIndex++) {
			MadokuQuestsClient.QuestObjectiveEntry objective = objectives.get(objectiveIndex);
			int objectiveY = rowY + objectiveIndex * objectiveRowHeight(objectiveCount);
			String objectiveDescription = MadokuQuestsClient.objectiveDescription(objective);
			float fittedObjectiveScale = fittedTextScale(client, objectiveDescription, objectiveTextScale, barWidth);
			drawScaledLeftText(
				context,
				client,
				objectiveDescription,
				x,
				objectiveY,
				fittedObjectiveScale
			);
			int progressY = objectiveY + scaledTextHeight(client, objectiveTextScale) + 2;
			QuestProgressRenderer.draw(
				context,
				x,
				progressY,
				barWidth,
				barHeight,
				objective.progress(),
				objective.required()
			);
			String progress = objective.progress() + "/" + objective.required();
			float progressScale = progressTextScale;
			drawProgressCount(
				context,
				client,
				progress,
				x + barWidth / 2,
				progressY,
				progressScale
			);
		}
	}

	private static String selectedQuestTitle() {
		int currentVersion = MadokuQuestsClient.displayVersion();
		if (currentVersion != cachedDisplayVersion) {
			cachedQuestTitle = MadokuQuestsClient.selectedQuestDisplayText();
			cachedDisplayVersion = currentVersion;
		}
		return cachedQuestTitle;
	}

	private static boolean hasRenderablePlayer(Minecraft client, boolean editing) {
		return client != null && client.level != null && client.player != null
			&& (editing || (client.gui != null && !client.gui.hud.isHidden()))
			&& !client.player.isSpectator();
	}

	private static void drawScaledLeftText(GuiGraphicsExtractor context, Minecraft client, String text, int x, int y, float scale) {
		context.pose().pushMatrix();
		context.pose().scale(scale, scale);
		context.text(client.font, text, Math.round(x / scale), Math.round(y / scale), TEXT_COLOR, true);
		context.pose().popMatrix();
	}

	private static void drawScaledCenteredText(GuiGraphicsExtractor context, Minecraft client, String text, int centerX, int y, float scale) {
		if (text == null || text.isEmpty()) return;
		int startX = Math.round(centerX - (client.font.width(text) * scale / 2.0F));
		drawScaledLeftText(context, client, text, startX, y, scale);
	}

	private static void drawProgressCount(GuiGraphicsExtractor context, Minecraft client, String text, int centerX, int y, float scale) {
		if (text == null || text.isEmpty()) return;
		context.pose().pushMatrix();
		context.pose().translate(centerX, y);
		context.pose().scale(scale, scale);
		context.text(client.font, text, Math.round(-client.font.width(text) / 2.0F), 0, TEXT_COLOR, true);
		context.pose().popMatrix();
	}

	private static int scaledTextHeight(Minecraft client, float scale) {
		return Math.round(client.font.lineHeight * scale);
	}

	private static float objectiveTextScale(int objectiveCount) {
		return 0.7F;
	}

	private static float objectiveProgressTextScale(int objectiveCount) {
		return 0.5F;
	}

	private static int objectiveBarHeight(int objectiveCount) {
		return QuestProgressRenderer.BAR_HEIGHT;
	}

	private static int objectiveBarWidth(int objectiveCount) {
		return HUD_WIDTH;
	}

	private static int objectiveStartY(int objectiveCount) {
		return objectiveCount <= 1 ? 17 : 16;
	}

	private static int objectiveRowHeight(int objectiveCount) {
		return objectiveCount <= 1 ? 12 : 14;
	}

	private static int hudHeight(MadokuQuestsClient.QuestEntry entry) {
		return entry != null && entry.objectives().size() > 1 ? HUD_FULL_HEIGHT : HUD_COMPACT_HEIGHT;
	}

	private static float fittedTextScale(Minecraft client, String text, float preferredScale, int maxWidth) {
		if (text == null || text.isEmpty()) return preferredScale;
		int textWidth = client.font.width(text);
		return textWidth <= 0 ? preferredScale : Math.min(preferredScale, maxWidth / (float) textWidth);
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}
}
