package madoku.craft.java.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import madoku.craft.java.core.menu.MenuAPIManager;
import madoku.craft.java.core.menu.MenuEntry;
import madoku.craft.java.core.json.JSONAPIManager;
import madoku.craft.java.core.json.JSONFormatAPIManager;
import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.module.MadokuModuleLifecycleManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client bootstrap and category discovery for the Quests module. */
public final class MadokuQuestsClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(MadokuQuestsClient.class);
	private static final String CONFIG_FOLDER_NAME = "madoku-craft-quests";
	private static final String CONFIG_FILE_NAME = "madoku-quests";
	private static final String SELECTED_QUEST = "selected-quest";
	private static final String SELECTION_CONFIGURED = "selection-configured";
	private static final String CATEGORY_ID = "category";
	private static final String QUEST_INDEX = "index";
	private static final List<CategoryDefinition> CATEGORY_DEFINITIONS = List.of(
		new CategoryDefinition(QuestRegistry.CORE_CATEGORY, "menu.madoku-craft.quests.category.core", MadokuModuleIds.CORE, "core-quests.png"),
		new CategoryDefinition(QuestRegistry.FARMING_CATEGORY, "menu.madoku-craft.quests.category.farming", MadokuModuleIds.FARMING, "farming-quests.png"),
		new CategoryDefinition(QuestRegistry.MOBS_CATEGORY, "menu.madoku-craft.quests.category.mobs", MadokuModuleIds.MOBS, "mobs-quests.png"),
		new CategoryDefinition(QuestRegistry.PETS_CATEGORY, "menu.madoku-craft.quests.category.pets", MadokuModuleIds.PETS, "pets-quests.png")
	);
	private static boolean initialized;
	private static QuestSelection selectedQuest;
	private static boolean selectionConfigured;
	private static QuestSnapshot questSnapshot = QuestSnapshot.empty();
	private static int displayVersion;

	private MadokuQuestsClient() {
	}

	public static void initialize() {
		if (initialized) return;
		loadSelection();
		MenuAPIManager.registerEntry(new MenuEntry(
			"quests",
			"menu.madoku-craft.quests",
			texture("madoku-menu/main-menu/quests-button.png"),
			texture("madoku-menu/main-menu/quests-button-highlighted.png"),
			60,
			client -> {
				if (client != null && client.player != null) client.setScreenAndShow(new QuestsMenuScreen());
			}
		));
		ClientPlayNetworking.registerGlobalReceiver(QuestsPayloadAPIManager.TYPE, (payload, context) ->
			context.client().execute(() -> applyQuestState(payload))
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearQuestState());
		QuestHudManager.initialize();
		initialized = true;
	}

	public static List<QuestCategory> activeCategories() {
		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.client();
		return CATEGORY_DEFINITIONS.stream()
			.filter(definition -> lifecycle.isRunning(definition.moduleId()))
			.map(definition -> new QuestCategory(
				definition.id(),
				definition.labelKey(),
				texture("madoku-menu/quests-menu/icons/" + definition.iconName())
			))
			.toList();
	}

	static void selectQuest(String categoryId, int questIndex) {
		if (!hasQuest(categoryId, questIndex)) return;
		selectedQuest = new QuestSelection(categoryId, questIndex);
		selectionConfigured = true;
		displayVersion++;
		saveSelection();
	}

	static void deselectQuest() {
		selectedQuest = null;
		selectionConfigured = true;
		displayVersion++;
		saveSelection();
	}

	static void turnInQuest(String categoryId, int questIndex) {
		if (!questComplete(categoryId, questIndex)) return;
		if (ClientPlayNetworking.canSend(QuestsPayloadAPIManager.TurnInPayload.TYPE)) {
			ClientPlayNetworking.send(new QuestsPayloadAPIManager.TurnInPayload(categoryId, questIndex));
		}
	}

	static int displayVersion() {
		return displayVersion;
	}

	static QuestSelection selectedQuest() {
		return selectedQuest;
	}

	static boolean isQuestSelected(String categoryId, int questIndex) {
		return selectedQuest != null
			&& selectedQuest.categoryId().equals(categoryId)
			&& selectedQuest.questIndex() == questIndex;
	}

	static String selectedQuestDisplayText() {
		QuestSelection selection = selectedQuest;
		if (selection == null || !hasQuest(selection.categoryId(), selection.questIndex())) return null;
		QuestEntry entry = questEntry(selection.categoryId(), selection.questIndex());
		QuestDifficulty difficulty = entry == null ? null : QuestDifficulty.fromId(entry.difficulty());
		if (entry == null || difficulty == null || entry.titleKey().isBlank()) return null;
		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.client();
		for (CategoryDefinition definition : CATEGORY_DEFINITIONS) {
			if (!definition.id().equals(selection.categoryId()) || !lifecycle.isRunning(definition.moduleId())) continue;
			return Component.translatable(entry.titleKey(), difficulty.tierLabel()).getString();
		}
		return null;
	}

	static String selectedQuestProgressText() {
		QuestSelection selection = selectedQuest;
		if (selection == null) return null;
		QuestEntry entry = questEntry(selection.categoryId(), selection.questIndex());
		return entry == null ? null : entry.progress() + "/" + entry.required();
	}

	static String selectedQuestDescriptionText() {
		QuestSelection selection = selectedQuest;
		if (selection == null) return null;
		QuestEntry entry = questEntry(selection.categoryId(), selection.questIndex());
		return entry == null || entry.descriptionKey().isBlank()
			? null
			: Component.translatable(entry.descriptionKey(), entry.required()).getString();
	}

	static int questCount(String categoryId) {
		if (categoryId == null || categoryId.isBlank()) return 0;
		return questSnapshot.questsByCategory().getOrDefault(categoryId, List.of()).size();
	}

	static QuestEntry questEntry(String categoryId, int questIndex) {
		if (categoryId == null || categoryId.isBlank() || questIndex < 0) return null;
		List<QuestEntry> quests = questSnapshot.questsByCategory().get(categoryId);
		return quests == null || questIndex >= quests.size() ? null : quests.get(questIndex);
	}

	static String questTitle(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		QuestDifficulty difficulty = entry == null ? null : QuestDifficulty.fromId(entry.difficulty());
		return entry == null || difficulty == null || entry.titleKey().isBlank()
			? ""
			: Component.translatable(entry.titleKey(), difficulty.tierLabel()).getString();
	}

	static String questDescription(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		return entry == null || entry.descriptionKey().isBlank()
			? ""
			: Component.translatable(entry.descriptionKey(), entry.required()).getString();
	}

	static String questDifficulty(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		return entry == null ? "" : Component.translatable("menu.madoku-craft.quests.difficulty." + entry.difficulty()).getString();
	}

	static String questProgress(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		return entry == null ? "" : entry.progress() + "/" + entry.required();
	}

	static boolean questComplete(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		return entry != null && entry.complete();
	}

	static boolean hasQuest(String categoryId, int questIndex) {
		return categoryId != null && questIndex >= 0 && questIndex < questCount(categoryId);
	}

	private static void applyQuestState(QuestsPayloadAPIManager payload) {
		if (payload == null) return;
		Map<String, List<QuestEntry>> questsByCategory = new LinkedHashMap<>();
		String encoded = payload.quests();
		if (encoded != null && !encoded.isBlank()) {
			try {
				JsonElement root = JsonParser.parseString(encoded);
				if (root.isJsonArray()) {
					for (JsonElement element : root.getAsJsonArray()) {
						if (!element.isJsonObject()) continue;
						JsonObject entry = element.getAsJsonObject();
						String id = readString(entry, "id", "");
						String category = readString(entry, "category", "");
						String difficulty = readString(entry, "difficulty", "");
						String titleKey = readString(entry, "title-key", "");
						String descriptionKey = readString(entry, "description-key", "");
						if (id.isBlank()
							|| category.isBlank()
							|| titleKey.isBlank()
							|| descriptionKey.isBlank()
							|| QuestDifficulty.fromId(difficulty) == null) continue;
						List<QuestEntry> quests = questsByCategory.computeIfAbsent(category, ignored -> new ArrayList<>());
						if (quests.size() < 7) {
							quests.add(new QuestEntry(
								id,
								difficulty,
								titleKey,
								descriptionKey,
								Math.max(0, readInt(entry, "display-progress", 0)),
								Math.max(0, readInt(entry, "display-required", 0)),
								readBoolean(entry, "complete", false)
							));
						}
					}
				}
			} catch (RuntimeException exception) {
				LOGGER.warn("Failed to decode the synchronized Madoku Quests state.", exception);
			}
		}
		questSnapshot = new QuestSnapshot(payload.day(), questsByCategory);
		displayVersion++;
		if (!selectionConfigured && hasQuest("core", 0)) {
			selectedQuest = new QuestSelection("core", 0);
			selectionConfigured = true;
			displayVersion++;
			saveSelection();
		}
	}

	private static void clearQuestState() {
		questSnapshot = QuestSnapshot.empty();
		displayVersion++;
	}

	private static void loadSelection() {
		try {
			Path directory = JSONAPIManager.getOrCreateGlobalSystemDirectory(CONFIG_FOLDER_NAME);
			Path file = directory.resolve(CONFIG_FILE_NAME + ".json");
			JsonObject defaults = buildDefaultsJson();
			JsonObject rawData = JSONFormatAPIManager.readManagedDocument(file).data();
			JsonObject rawSelection = readObject(rawData, SELECTED_QUEST);
			boolean hasSelectionFields = rawSelection.has(CATEGORY_ID) && rawSelection.has(QUEST_INDEX);
			JsonObject normalized = JSONFormatAPIManager.ensureManagedFile(file, defaults);
			boolean configured = readBoolean(normalized, SELECTION_CONFIGURED, false);
			JsonObject selection = readObject(normalized, SELECTED_QUEST);
			String categoryId = readString(selection, CATEGORY_ID, "");
			int questIndex = readInt(selection, QUEST_INDEX, -1);
			selectionConfigured = hasSelectionFields && (configured || !categoryId.isBlank());
			selectedQuest = categoryId.isBlank() || questIndex < 0 || questIndex >= 7
				? null
				: new QuestSelection(categoryId, questIndex);
			JSONFormatAPIManager.writeManagedFile(file, buildConfigJson(), defaults);
		} catch (IOException | RuntimeException exception) {
			selectedQuest = null;
			selectionConfigured = false;
			LOGGER.error("Failed to load Madoku Quests selection; starting without a selected quest.", exception);
		}
	}

	private static void saveSelection() {
		try {
			Path directory = JSONAPIManager.getOrCreateGlobalSystemDirectory(CONFIG_FOLDER_NAME);
			Path file = directory.resolve(CONFIG_FILE_NAME + ".json");
			JSONFormatAPIManager.writeManagedFile(file, buildConfigJson(), buildDefaultsJson());
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to save Madoku Quests selection.", exception);
		}
	}

	private static JsonObject buildDefaultsJson() {
		return JSONFormatAPIManager.object()
			.put(SELECTION_CONFIGURED, false)
			.object(SELECTED_QUEST, selection -> selection
				.put(CATEGORY_ID, "")
				.put(QUEST_INDEX, -1))
			.build();
	}

	private static JsonObject buildConfigJson() {
		JSONFormatAPIManager.ObjectBuilder root = JSONFormatAPIManager.object();
		root.put(SELECTION_CONFIGURED, selectionConfigured);
		root.object(SELECTED_QUEST, selection -> {
			if (selectedQuest == null) return;
			selection.put(CATEGORY_ID, selectedQuest.categoryId());
			selection.put(QUEST_INDEX, selectedQuest.questIndex());
		});
		return root.build();
	}

	private static JsonObject readObject(JsonObject source, String key) {
		JsonElement element = source == null ? null : source.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
	}

	private static String readString(JsonObject source, String key, String fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsString() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static int readInt(JsonObject source, String key, int fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsInt() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static boolean readBoolean(JsonObject source, String key, boolean fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsBoolean() : fallback;
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}

	public record QuestCategory(String id, String labelKey, Identifier iconTexture) {
	}

	record QuestSelection(String categoryId, int questIndex) {
	}

	private record CategoryDefinition(String id, String labelKey, Identifier moduleId, String iconName) {
	}

	public record QuestEntry(
		String id,
		String difficulty,
		String titleKey,
		String descriptionKey,
		int progress,
		int required,
		boolean complete
	) {
	}

	private record QuestSnapshot(long day, Map<String, List<QuestEntry>> questsByCategory) {
		private QuestSnapshot {
			Map<String, List<QuestEntry>> copy = new LinkedHashMap<>();
			if (questsByCategory != null) {
				questsByCategory.forEach((category, quests) -> copy.put(category, List.copyOf(quests)));
			}
			questsByCategory = Map.copyOf(copy);
		}

		private static QuestSnapshot empty() {
			return new QuestSnapshot(0L, Map.of());
		}
	}
}
