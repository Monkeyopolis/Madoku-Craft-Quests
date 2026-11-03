package madoku.craft.java.quests;

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
	private static final Identifier QUEST_HUD_LAYOUT_ID = Identifier.fromNamespaceAndPath("madoku-craft", "selected_quest_hud");
	private static final Identifier QUEST_HUD_TEXTURE = texture("madoku-menu/quests-menu/selected-quest-hud.png");
	private static final int HUD_WIDTH = 78;
	private static final int HUD_HEIGHT = 40;
	private static final int DEFAULT_RIGHT_INSET = 4;
	private static final int DEFAULT_VERTICAL_DIVISOR = 4;
	private static final int TITLE_X = 0;
	private static final int TITLE_SEPARATOR_Y = 13;
	private static final int TITLE_SEPARATOR_HEIGHT = 2;
	private static final int TITLE_MAX_WIDTH = 78;
	private static final int DESCRIPTION_X = 0;
	private static final int DESCRIPTION_GAP = 2;
	private static final int DESCRIPTION_Y = TITLE_SEPARATOR_Y + TITLE_SEPARATOR_HEIGHT + DESCRIPTION_GAP;
	private static final int DESCRIPTION_MAX_WIDTH = 78;
	private static final int PROGRESS_X = 0;
	private static final int PROGRESS_GAP = 4;
	private static final int PROGRESS_BAR_GAP = 2;
	private static final int TEXT_GAP = 2;
	private static final float TEXT_SCALE = 0.7F;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static int defaultX;
	private static int defaultY;
	private static boolean defaultPositionInitialized;
	private static int cachedDisplayVersion = Integer.MIN_VALUE;
	private static String cachedQuestTitle;
	private static String cachedDescription;
	private static String cachedProgress;
	private static boolean initialized;
	private static final UiLayoutElement QUEST_HUD_LAYOUT = new UiLayoutElement() {
		@Override public Identifier id() { return QUEST_HUD_LAYOUT_ID; }
		@Override public int defaultX() { return defaultX; }
		@Override public int defaultY() { return defaultY; }
		@Override public Bounds bounds(Minecraft client, int x, int y) { return new Bounds(x, y, HUD_WIDTH, HUD_HEIGHT); }
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
			Identifier.fromNamespaceAndPath("madoku-craft", "selected_quest_hud"),
			QuestHudManager::renderQuestHud
		);
		initialized = true;
	}

	private static void renderQuestHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (UiLayoutAPIManager.isEditMode()) return;
		initializeDefaultPosition(client, context.guiWidth(), context.guiHeight());
		UiLayoutAPIManager.Position position = UiLayoutAPIManager.position(QUEST_HUD_LAYOUT);
		renderQuestHud(context, client, position.x(), position.y(), false);
	}

	private static void renderQuestHud(GuiGraphicsExtractor context, Minecraft client, int x, int y, boolean editing) {
		if (!hasRenderablePlayer(client, editing)) return;
		initializeDefaultPosition(client, context.guiWidth(), context.guiHeight());
		String questTitle = selectedQuestTitle();
		if (questTitle == null) return;

		context.blit(
			RenderPipelines.GUI_TEXTURED,
			QUEST_HUD_TEXTURE,
			x,
			y,
			0.0F,
			0.0F,
			HUD_WIDTH,
			HUD_HEIGHT,
			HUD_WIDTH,
			HUD_HEIGHT
		);
		float titleScale = fittedTextScale(client, questTitle, TEXT_SCALE, TITLE_MAX_WIDTH);
		drawScaledLeftText(context, client, questTitle, x + TITLE_X, y + TITLE_SEPARATOR_Y - TEXT_GAP - scaledTextHeight(client, titleScale), titleScale);
		String description = cachedDescription;
		float descriptionScale = fittedTextScale(client, description, TEXT_SCALE, DESCRIPTION_MAX_WIDTH);
		int descriptionY = y + DESCRIPTION_Y;
		drawScaledLeftText(context, client, description, x + DESCRIPTION_X, descriptionY, descriptionScale);

		String progress = cachedProgress;
		float progressScale = fittedTextScale(client, progress, TEXT_SCALE, QuestProgressRenderer.BAR_WIDTH);
		int progressY = descriptionY + scaledTextHeight(client, descriptionScale) + PROGRESS_GAP;
		drawScaledLeftText(context, client, progress, x + PROGRESS_X, progressY, progressScale);
		QuestProgressRenderer.draw(
			context,
			x,
			progressY + scaledTextHeight(client, progressScale) + PROGRESS_BAR_GAP,
			QuestProgressRenderer.BAR_WIDTH,
			QuestProgressRenderer.BAR_HEIGHT,
			selectedQuestProgress(),
			selectedQuestRequired()
		);
	}

	private static String selectedQuestTitle() {
		int currentVersion = MadokuQuestsClient.displayVersion();
		if (currentVersion != cachedDisplayVersion) {
			cachedQuestTitle = MadokuQuestsClient.selectedQuestDisplayText();
			cachedDescription = cachedQuestTitle == null ? null : MadokuQuestsClient.selectedQuestDescriptionText();
			cachedProgress = cachedQuestTitle == null ? null : MadokuQuestsClient.selectedQuestProgressText();
			cachedDisplayVersion = currentVersion;
		}
		return cachedQuestTitle;
	}

	private static int selectedQuestProgress() {
		MadokuQuestsClient.QuestSelection selection = MadokuQuestsClient.selectedQuest();
		if (selection == null) return 0;
		MadokuQuestsClient.QuestEntry entry = MadokuQuestsClient.questEntry(selection.categoryId(), selection.questIndex());
		return entry == null ? 0 : entry.progress();
	}

	private static int selectedQuestRequired() {
		MadokuQuestsClient.QuestSelection selection = MadokuQuestsClient.selectedQuest();
		if (selection == null) return 0;
		MadokuQuestsClient.QuestEntry entry = MadokuQuestsClient.questEntry(selection.categoryId(), selection.questIndex());
		return entry == null ? 0 : entry.required();
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

	private static void initializeDefaultPosition(Minecraft client, int guiWidth, int guiHeight) {
		if (defaultPositionInitialized || client == null || client.getWindow() == null || guiWidth <= 0 || guiHeight <= 0) return;
		defaultX = Math.max(0, guiWidth - HUD_WIDTH - DEFAULT_RIGHT_INSET);
		defaultY = Math.max(0, guiHeight / DEFAULT_VERTICAL_DIVISOR);
		UiLayoutAPIManager.initializePositionIfUnset(QUEST_HUD_LAYOUT_ID, client, defaultX, defaultY);
		defaultPositionInitialized = true;
	}

	private static int scaledTextHeight(Minecraft client, float scale) {
		return Math.round(client.font.lineHeight * scale);
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
