package madoku.craft.java.quests;

import java.util.List;

import madoku.craft.java.core.menu.MenuScreen;
import madoku.craft.java.core.menu.MenuSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Placeholder Quests screen with the final category, viewport, and scroll layout. */
public final class QuestsMenuScreen extends Screen {
	private static final Identifier CONTAINER_TEXTURE = texture("madoku-menu/quests-menu/quests-menu-container.png");
	private static final Identifier QUEST_ENTRY_TEXTURE = texture("madoku-menu/quests-menu/quest-entry.png");
	private static final Identifier QUEST_ENTRY_SELECTED_TEXTURE = texture("madoku-menu/quests-menu/quest-entry-selected.png");
	private static final Identifier CATEGORY_SLOT_TEXTURE = texture("shared-ui/category-slot.png");
	private static final Identifier CATEGORY_SLOT_SELECTED_TEXTURE = texture("shared-ui/category-slot-selected.png");
	private static final Identifier COMPLETED_BUTTON_TEXTURE = texture("shared-ui/completed-button.png");
	private static final Identifier INCOMPLETE_BUTTON_TEXTURE = texture("shared-ui/incomplete-button.png");
	private static final Identifier EXIT_TEXTURE = texture("shared-ui/exit-button.png");
	private static final Identifier EXIT_HIGHLIGHTED_TEXTURE = texture("shared-ui/exit-button-highlighted.png");
	private static final Identifier SCROLLER_TEXTURE = texture("shared-ui/scroller.png");
	private static final Identifier SCROLLER_HIGHLIGHTED_TEXTURE = texture("shared-ui/scroller-highlighted.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int BACKGROUND_RENDER_SIZE = 256;
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT = 166;
	private static final int EXIT_X = 157;
	private static final int EXIT_Y = 7;
	private static final int EXIT_SIZE = 12;
	private static final int CATEGORIES_HEADER_LEFT = 7;
	private static final int CATEGORIES_HEADER_WIDTH = 26;
	private static final int CATEGORY_HEADER_LEFT = 53;
	private static final int CATEGORY_HEADER_WIDTH = 80;
	private static final int HEADER_SEPARATOR_Y = 17;
	private static final int HEADER_TEXT_GAP = 2;
	private static final float CATEGORIES_HEADER_TEXT_SCALE = 0.8F;
	private static final float CATEGORY_HEADER_TEXT_SCALE = 0.8F;
	private static final int CATEGORY_SLOT_X = 11;
	private static final int CATEGORY_SLOT_TOP = 23;
	private static final int CATEGORY_SLOT_SIZE = 18;
	private static final int CATEGORY_ICON_X = 12;
	private static final int CATEGORY_ICON_TOP = 24;
	private static final int CATEGORY_ICON_STEP = 22;
	private static final int CATEGORY_ICON_SIZE = 16;
	private static final int QUEST_VIEWPORT_X = 37;
	private static final int QUEST_VIEWPORT_Y = 23;
	private static final int QUEST_VIEWPORT_WIDTH = 112;
	private static final int QUEST_VIEWPORT_HEIGHT = 128;
	private static final int QUEST_ENTRY_WIDTH = 112;
	private static final int QUEST_ENTRY_HEIGHT = 40;
	private static final int QUEST_ENTRY_GAP = 4;
	private static final int QUEST_TEXT_X = 9;
	private static final int QUEST_TEXT_MAX_WIDTH = 78;
	private static final int QUEST_DESCRIPTION_Y = 17;
	private static final int QUEST_PROGRESS_GAP = 4;
	private static final int QUEST_PROGRESS_BAR_GAP = 2;
	private static final int QUEST_STATUS_X = 93;
	private static final int QUEST_STATUS_Y = 14;
	private static final int SCROLLER_X = 158;
	private static final int SCROLLER_Y = 24;
	private static final int SCROLLER_WIDTH = 10;
	private static final int SCROLLER_HEIGHT = 15;
	private static final int SCROLLER_TRACK_HEIGHT = 127;
	private static final int MAX_QUESTS_PER_CATEGORY = 7;
	private static final int QUEST_FOOTER_Y = QUEST_VIEWPORT_Y + QUEST_VIEWPORT_HEIGHT + 2;
	private static final int QUEST_FOOTER_LEFT_MAX_WIDTH = 82;
	private static final float QUEST_FOOTER_TEXT_SCALE = 0.7F;
	private static final int SCROLL_STEP = 16;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final float QUEST_TEXT_SCALE = 0.7F;

	private List<MadokuQuestsClient.QuestCategory> categories = List.of();
	private int selectedCategory;
	private int scrollOffset;
	private boolean draggingScroller;
	private int scrollerDragOffset;

	public QuestsMenuScreen() {
		super(Component.translatable("menu.madoku-craft.quests.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(null);
	}

	@Override
	protected void init() {
		categories = MadokuQuestsClient.activeCategories();
		if (selectedCategory >= categories.size()) selectedCategory = 0;
		MadokuQuestsClient.QuestSelection selection = MadokuQuestsClient.selectedQuest();
		if (selection != null) {
			for (int index = 0; index < categories.size(); index++) {
				if (categories.get(index).id().equals(MadokuQuestsClient.selectedQuestCategoryId())) {
					selectedCategory = index;
					break;
				}
			}
		}
		this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> renderMenu(guiGraphics, mouseX, mouseY));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 1) return super.mouseClicked(event, doubleClick);

		int panelX = panelX();
		int panelY = panelY();
		if (contains(panelX + EXIT_X, panelY + EXIT_Y, EXIT_SIZE, EXIT_SIZE, event.x(), event.y())) {
			MenuSoundManager.playButtonClick();
			Minecraft.getInstance().setScreenAndShow(new MenuScreen());
			return true;
		}

		for (int index = 0; index < categories.size(); index++) {
			if (!categorySlotBounds(panelX, panelY, index).contains(event.x(), event.y())) continue;
			if (selectedCategory != index) {
				selectedCategory = index;
				scrollOffset = 0;
				MenuSoundManager.playButtonClick();
			}
			return true;
		}

		if (isScrolling() && scrollerBounds(panelX, panelY).contains(event.x(), event.y())) {
			draggingScroller = true;
			scrollerDragOffset = (int) Math.round(event.y()) - scrollerBounds(panelX, panelY).y();
			return true;
		}

		if (questViewportBounds(panelX, panelY).contains(event.x(), event.y())) {
			for (int index = 0; index < currentQuestCount(); index++) {
				Bounds entry = questEntryBounds(panelX, panelY, index);
				if (!entry.contains(event.x(), event.y())) continue;
				String categoryId = selectedCategory < categories.size() ? categories.get(selectedCategory).id() : "";
				MadokuQuestsClient.QuestEntry questEntry = MadokuQuestsClient.questEntry(categoryId, index);
				if (questEntry == null) return true;
				boolean alreadySelected = MadokuQuestsClient.isQuestSelected(questEntry.id());
				if (statusBounds(entry).contains(event.x(), event.y())) {
					if (MadokuQuestsClient.questComplete(categoryId, index)) {
						MadokuQuestsClient.turnInQuest(questEntry.id());
						MenuSoundManager.playButtonClick();
					}
				} else {
					if (alreadySelected) MadokuQuestsClient.deselectQuest();
					else MadokuQuestsClient.selectQuest(questEntry.id());
					MenuSoundManager.playButtonClick();
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 1 && draggingScroller) {
			draggingScroller = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (event.button() == 1 && draggingScroller) {
			int maxScrollOffset = maxScrollOffset();
			int travel = SCROLLER_TRACK_HEIGHT - SCROLLER_HEIGHT;
			if (maxScrollOffset > 0 && travel > 0) {
				int trackOffset = (int) Math.round(event.y()) - panelY() - SCROLLER_Y - scrollerDragOffset;
				int clampedTrackOffset = Math.max(0, Math.min(trackOffset, travel));
				scrollOffset = Math.round(clampedTrackOffset * maxScrollOffset / (float) travel);
			}
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!questViewportBounds(panelX(), panelY()).contains(mouseX, mouseY)) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int wheelDelta = (int) Math.round(scrollY * SCROLL_STEP);
		if (wheelDelta == 0 && scrollY != 0.0D) wheelDelta = scrollY > 0.0D ? 1 : -1;
		scrollOffset = clampScrollOffset(scrollOffset - wheelDelta);
		return true;
	}

	private void renderMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x88000000);

		int panelX = panelX();
		int panelY = panelY();
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			CONTAINER_TEXTURE,
			panelX,
			panelY,
			0.0F,
			0.0F,
			BACKGROUND_RENDER_SIZE,
			BACKGROUND_RENDER_SIZE,
			TEXTURE_SIZE,
			TEXTURE_SIZE
		);

		String categoriesLabel = Component.translatable("menu.madoku-craft.quests.categories").getString();
		float categoriesTextScale = fittedTextScale(categoriesLabel, CATEGORIES_HEADER_TEXT_SCALE, CATEGORIES_HEADER_WIDTH);
		drawScaledCenteredText(
			guiGraphics,
			categoriesLabel,
			panelX + CATEGORIES_HEADER_LEFT + CATEGORIES_HEADER_WIDTH / 2,
			panelY + headerTextY(categoriesTextScale),
			categoriesTextScale,
			TEXT_COLOR
		);
		String categoryName = selectedCategory < categories.size()
			? Component.translatable(categories.get(selectedCategory).labelKey()).getString()
			: Component.translatable("menu.madoku-craft.quests.category.core").getString();
		float categoryTextScale = fittedTextScale(categoryName, CATEGORY_HEADER_TEXT_SCALE, CATEGORY_HEADER_WIDTH);
		drawScaledCenteredText(
			guiGraphics,
			categoryName,
			panelX + CATEGORY_HEADER_LEFT + CATEGORY_HEADER_WIDTH / 2,
			panelY + headerTextY(categoryTextScale),
			categoryTextScale,
			TEXT_COLOR
		);

		boolean exitHovered = contains(panelX + EXIT_X, panelY + EXIT_Y, EXIT_SIZE, EXIT_SIZE, mouseX, mouseY);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			exitHovered ? EXIT_HIGHLIGHTED_TEXTURE : EXIT_TEXTURE,
			panelX + EXIT_X,
			panelY + EXIT_Y,
			0.0F,
			0.0F,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE
		);

		for (int index = 0; index < categories.size(); index++) {
			MadokuQuestsClient.QuestCategory category = categories.get(index);
			Bounds slot = categorySlotBounds(panelX, panelY, index);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				selectedCategory == index ? CATEGORY_SLOT_SELECTED_TEXTURE : CATEGORY_SLOT_TEXTURE,
				slot.x(),
				slot.y(),
				0.0F,
				0.0F,
				CATEGORY_SLOT_SIZE,
				CATEGORY_SLOT_SIZE,
				CATEGORY_SLOT_SIZE,
				CATEGORY_SLOT_SIZE
			);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				category.iconTexture(),
				panelX + CATEGORY_ICON_X,
				panelY + CATEGORY_ICON_TOP + index * CATEGORY_ICON_STEP,
				0.0F,
				0.0F,
				CATEGORY_ICON_SIZE,
				CATEGORY_ICON_SIZE,
				CATEGORY_ICON_SIZE,
				CATEGORY_ICON_SIZE
			);
		}

		int currentScrollOffset = scrollOffset();
		guiGraphics.enableScissor(
			panelX + QUEST_VIEWPORT_X,
			panelY + QUEST_VIEWPORT_Y,
			panelX + QUEST_VIEWPORT_X + QUEST_VIEWPORT_WIDTH,
			panelY + QUEST_VIEWPORT_Y + QUEST_VIEWPORT_HEIGHT
		);
		for (int index = 0; index < currentQuestCount(); index++) {
			Bounds entry = questEntryBounds(panelX, panelY, index, currentScrollOffset);
			if (entry.y() + entry.height() <= panelY + QUEST_VIEWPORT_Y || entry.y() >= panelY + QUEST_VIEWPORT_Y + QUEST_VIEWPORT_HEIGHT) continue;
			String categoryId = selectedCategory < categories.size() ? categories.get(selectedCategory).id() : "";
			MadokuQuestsClient.QuestEntry questEntry = MadokuQuestsClient.questEntry(categoryId, index);
			boolean selected = questEntry != null && MadokuQuestsClient.isQuestSelected(questEntry.id());
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				selected ? QUEST_ENTRY_SELECTED_TEXTURE : QUEST_ENTRY_TEXTURE,
				entry.x(),
				entry.y(),
				0.0F,
				0.0F,
				QUEST_ENTRY_WIDTH,
				QUEST_ENTRY_HEIGHT,
				QUEST_ENTRY_WIDTH,
				QUEST_ENTRY_HEIGHT
			);
			String questTitle = MadokuQuestsClient.questTitle(
				selectedCategory < categories.size() ? categories.get(selectedCategory).id() : "",
				index
			);
			String questDescription = MadokuQuestsClient.questDescription(categoryId, index);
			String questProgress = MadokuQuestsClient.questProgress(categoryId, index);
			float descriptionScale = fittedTextScale(questDescription, QUEST_TEXT_SCALE, QUEST_TEXT_MAX_WIDTH);
			int descriptionY = entry.y() + QUEST_DESCRIPTION_Y;
			drawFittedLeftText(
				guiGraphics,
				questTitle,
				entry.x() + QUEST_TEXT_X,
				entry.y() + 6,
				QUEST_TEXT_SCALE,
				QUEST_TEXT_MAX_WIDTH,
				TEXT_COLOR
			);
			drawFittedLeftText(
				guiGraphics,
				questDescription,
				entry.x() + QUEST_TEXT_X,
				descriptionY,
				QUEST_TEXT_SCALE,
				QUEST_TEXT_MAX_WIDTH,
				TEXT_COLOR
			);
			drawFittedLeftText(
				guiGraphics,
				questProgress,
				entry.x() + QUEST_TEXT_X,
				descriptionY + scaledTextHeight(descriptionScale) + QUEST_PROGRESS_GAP,
				QUEST_TEXT_SCALE,
				QUEST_TEXT_MAX_WIDTH,
				TEXT_COLOR
			);
			if (questEntry != null) {
				float progressScale = fittedTextScale(questProgress, QUEST_TEXT_SCALE, QUEST_TEXT_MAX_WIDTH);
				QuestProgressRenderer.draw(
					guiGraphics,
					entry.x() + QUEST_TEXT_X,
					descriptionY + scaledTextHeight(descriptionScale) + QUEST_PROGRESS_GAP
						+ scaledTextHeight(progressScale) + QUEST_PROGRESS_BAR_GAP,
					QuestProgressRenderer.BAR_WIDTH,
					QuestProgressRenderer.BAR_HEIGHT,
					questEntry.progress(),
					questEntry.required()
				);
			}
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				MadokuQuestsClient.questComplete(
					selectedCategory < categories.size() ? categories.get(selectedCategory).id() : "",
					index
				) ? COMPLETED_BUTTON_TEXTURE : INCOMPLETE_BUTTON_TEXTURE,
				entry.x() + QUEST_STATUS_X,
				entry.y() + QUEST_STATUS_Y,
				0.0F,
				0.0F,
				EXIT_SIZE,
				EXIT_SIZE,
				EXIT_SIZE,
				EXIT_SIZE
			);
		}
		guiGraphics.disableScissor();

		if (isScrolling()) {
			Bounds scroller = scrollerBounds(panelX, panelY);
			boolean scrollerHovered = scroller.contains(mouseX, mouseY);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				scrollerHovered || draggingScroller ? SCROLLER_HIGHLIGHTED_TEXTURE : SCROLLER_TEXTURE,
				scroller.x(),
				scroller.y(),
				0.0F,
				0.0F,
				SCROLLER_WIDTH,
				SCROLLER_HEIGHT,
				SCROLLER_WIDTH,
				SCROLLER_HEIGHT
			);
		}

		int questCount = currentQuestCount();
		String footerText = questCount >= MAX_QUESTS_PER_CATEGORY
			? Component.translatable("menu.madoku-craft.quests.no_new").getString()
			: Component.translatable("menu.madoku-craft.quests.new_in", formatQuestTimer()).getString();
		drawFittedLeftText(
			guiGraphics,
			footerText,
			panelX + QUEST_VIEWPORT_X,
			panelY + QUEST_FOOTER_Y,
			QUEST_FOOTER_TEXT_SCALE,
			QUEST_FOOTER_LEFT_MAX_WIDTH,
			TEXT_COLOR
		);
		String countText = questCount + "/" + MAX_QUESTS_PER_CATEGORY;
		drawFittedRightText(
			guiGraphics,
			countText,
			panelX + QUEST_VIEWPORT_X + QUEST_VIEWPORT_WIDTH,
			panelY + QUEST_FOOTER_Y,
			QUEST_FOOTER_TEXT_SCALE,
			TEXT_COLOR
		);
	}

	private Bounds questEntryBounds(int panelX, int panelY, int index) {
		return questEntryBounds(panelX, panelY, index, scrollOffset());
	}

	private Bounds categorySlotBounds(int panelX, int panelY, int index) {
		return new Bounds(
			panelX + CATEGORY_SLOT_X,
			panelY + CATEGORY_SLOT_TOP + index * CATEGORY_ICON_STEP,
			CATEGORY_SLOT_SIZE,
			CATEGORY_SLOT_SIZE
		);
	}

	private Bounds questEntryBounds(int panelX, int panelY, int index, int currentScrollOffset) {
		return new Bounds(
			panelX + QUEST_VIEWPORT_X,
			panelY + QUEST_VIEWPORT_Y + index * (QUEST_ENTRY_HEIGHT + QUEST_ENTRY_GAP) - currentScrollOffset,
			QUEST_ENTRY_WIDTH,
			QUEST_ENTRY_HEIGHT
		);
	}

	private Bounds statusBounds(Bounds entry) {
		return new Bounds(entry.x() + QUEST_STATUS_X, entry.y() + QUEST_STATUS_Y, EXIT_SIZE, EXIT_SIZE);
	}

	private Bounds questViewportBounds(int panelX, int panelY) {
		return new Bounds(panelX + QUEST_VIEWPORT_X, panelY + QUEST_VIEWPORT_Y, QUEST_VIEWPORT_WIDTH, QUEST_VIEWPORT_HEIGHT);
	}

	private Bounds scrollerBounds(int panelX, int panelY) {
		int maximum = maxScrollOffset();
		int travel = SCROLLER_TRACK_HEIGHT - SCROLLER_HEIGHT;
		int thumbOffset = maximum == 0 ? 0 : Math.round(scrollOffset() * travel / (float) maximum);
		return new Bounds(panelX + SCROLLER_X, panelY + SCROLLER_Y + thumbOffset, SCROLLER_WIDTH, SCROLLER_HEIGHT);
	}

	private int maxScrollOffset() {
		int questCount = currentQuestCount();
		if (questCount <= 0) return 0;
		int contentHeight = questCount * QUEST_ENTRY_HEIGHT + (questCount - 1) * QUEST_ENTRY_GAP;
		return Math.max(0, contentHeight - QUEST_VIEWPORT_HEIGHT);
	}

	private int currentQuestCount() {
		if (selectedCategory < 0 || selectedCategory >= categories.size()) return 0;
		return Math.min(MAX_QUESTS_PER_CATEGORY,
			MadokuQuestsClient.questCount(categories.get(selectedCategory).id()));
	}

	private boolean isScrolling() {
		return maxScrollOffset() > 0;
	}

	private int scrollOffset() {
		scrollOffset = clampScrollOffset(scrollOffset);
		return scrollOffset;
	}

	private int clampScrollOffset(int value) {
		return Math.max(0, Math.min(value, maxScrollOffset()));
	}

	private String formatQuestTimer() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return "--:--:--";
		long nextQuestWorldTime = MadokuQuestsClient.nextQuestWorldTime();
		if (nextQuestWorldTime < 0L) return "--:--:--";
		long remainingTicks = Math.max(0L, nextQuestWorldTime - minecraft.level.getOverworldClockTime());
		long remainingSeconds = Math.max(0L, (remainingTicks + 19L) / 20L);
		long hours = remainingSeconds / 3_600L;
		long minutes = (remainingSeconds % 3_600L) / 60L;
		long seconds = remainingSeconds % 60L;
		if (remainingSeconds < 60L) return remainingSeconds + "s";
		String secondText = (seconds < 10L ? "0" : "") + seconds;
		if (hours == 0L) return minutes + ":" + secondText;
		String minuteText = (minutes < 10L ? "0" : "") + minutes;
		return hours + ":" + minuteText + ":" + secondText;
	}

	private int panelX() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	private int headerTextY(float scale) {
		return HEADER_SEPARATOR_Y - HEADER_TEXT_GAP - scaledTextHeight(scale);
	}

	private int scaledTextHeight(float scale) {
		return Math.round(this.font.lineHeight * scale);
	}

	private void drawScaledLeftText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		guiGraphics.text(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private void drawFittedLeftText(
		GuiGraphicsExtractor guiGraphics,
		String text,
		int x,
		int y,
		float preferredScale,
		int maxWidth,
		int color
	) {
		drawScaledLeftText(guiGraphics, text, x, y, fittedTextScale(text, preferredScale, maxWidth), color);
	}

	private void drawScaledCenteredText(GuiGraphicsExtractor guiGraphics, String text, int centerX, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		int startX = Math.round(centerX - (this.font.width(text) * scale / 2.0F));
		drawScaledLeftText(guiGraphics, text, startX, y, scale, color);
	}

	private void drawFittedRightText(
		GuiGraphicsExtractor guiGraphics,
		String text,
		int rightX,
		int y,
		float preferredScale,
		int color
	) {
		if (text == null || text.isEmpty()) return;
		float scale = Math.min(preferredScale, 1.0F);
		int startX = Math.round(rightX - this.font.width(text) * scale);
		drawScaledLeftText(guiGraphics, text, startX, y, scale, color);
	}

	private float fittedTextScale(String text, float preferredScale, int maxWidth) {
		if (text == null || text.isEmpty()) return preferredScale;
		int textWidth = this.font.width(text);
		return textWidth <= 0 ? preferredScale : Math.min(preferredScale, maxWidth / (float) textWidth);
	}

	private static boolean contains(int x, int y, int width, int height, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}

	private record Bounds(int x, int y, int width, int height) {
		private boolean contains(double mouseX, double mouseY) {
			return QuestsMenuScreen.contains(x, y, width, height, mouseX, mouseY);
		}
	}
}
