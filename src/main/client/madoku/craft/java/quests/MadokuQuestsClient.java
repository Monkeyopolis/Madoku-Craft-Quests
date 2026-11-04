package madoku.craft.java.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
	private static final String QUEST_ID = "id";
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

	static void selectQuest(String questId) {
		String normalizedQuestId = normalizeId(questId);
		if (normalizedQuestId.isBlank() || questEntryById(normalizedQuestId) == null) return;
		selectedQuest = new QuestSelection(normalizedQuestId);
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

	static void turnInQuest(String questId) {
		QuestEntry entry = questEntryById(questId);
		if (entry == null || !entry.complete() || entry.id().isBlank()) return;
		if (ClientPlayNetworking.canSend(QuestsPayloadAPIManager.TurnInPayload.TYPE)) {
			ClientPlayNetworking.send(new QuestsPayloadAPIManager.TurnInPayload(entry.id()));
		}
	}

	static int displayVersion() {
		return displayVersion;
	}

	static QuestSelection selectedQuest() {
		return selectedQuest;
	}

	static boolean isQuestSelected(String questId) {
		return selectedQuest != null && selectedQuest.questId().equals(normalizeId(questId));
	}

	static String selectedQuestDisplayText() {
		QuestEntry entry = selectedQuestEntry();
		QuestDifficulty difficulty = entry == null ? null : QuestDifficulty.fromId(entry.difficulty());
		if (entry == null || difficulty == null || entry.titleKey().isBlank()) return null;
		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.client();
		for (CategoryDefinition definition : CATEGORY_DEFINITIONS) {
			if (!definition.id().equals(entry.categoryId()) || !lifecycle.isRunning(definition.moduleId())) continue;
			return Component.translatable(entry.titleKey(), difficulty.tierLabel()).getString();
		}
		return null;
	}

	static String selectedQuestProgressText() {
		QuestEntry entry = selectedQuestEntry();
		return entry == null ? null : entry.progress() + "/" + entry.required();
	}

	static String selectedQuestDescriptionText() {
		QuestEntry entry = selectedQuestEntry();
		return entry == null || entry.descriptionKey().isBlank()
			? null
			: Component.translatable(entry.descriptionKey(), entry.required()).getString();
	}

	static String selectedQuestCategoryId() {
		QuestEntry entry = selectedQuestEntry();
		return entry == null ? "" : entry.categoryId();
	}

	static long nextQuestWorldTime() {
		return questSnapshot.nextQuestWorldTime();
	}

	static QuestEntry selectedQuestEntry() {
		return selectedQuest == null ? null : questEntryById(selectedQuest.questId());
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

	private static QuestEntry questEntryById(String questId) {
		String normalizedQuestId = normalizeId(questId);
		return normalizedQuestId.isBlank() ? null : questSnapshot.questsById().get(normalizedQuestId);
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

	static List<QuestObjectiveEntry> questObjectives(String categoryId, int questIndex) {
		QuestEntry entry = questEntry(categoryId, questIndex);
		return entry == null ? List.of() : entry.objectives();
	}

	static String objectiveDescription(QuestObjectiveEntry objective) {
		if (objective == null) return "";
		if (!objective.descriptionKey().isBlank()) {
			return Component.translatable(objective.descriptionKey(), objective.required()).getString();
		}
		String targetName = objective.targetId().isBlank()
			? "objective"
			: objective.targetId().substring(objective.targetId().indexOf(':') + 1).replace('_', ' ');
		String actionKey = "menu.madoku-craft.quests.objective." + normalizeId(objective.subcategory());
		return Component.translatable(actionKey, targetName).getString();
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
						int displayProgress = Math.max(0, readInt(entry, "display-progress", 0));
						int displayRequired = Math.max(0, readInt(entry, "display-required", 0));
						List<QuestObjectiveEntry> objectives = readObjectiveEntries(
							entry,
							descriptionKey,
							displayProgress,
							displayRequired
						);
						List<QuestEntry> quests = questsByCategory.computeIfAbsent(category, ignored -> new ArrayList<>());
						if (quests.size() < 7) {
							quests.add(new QuestEntry(
								id,
								category,
								difficulty,
								titleKey,
								descriptionKey,
								displayProgress,
								displayRequired,
								readBoolean(entry, "complete", false),
								objectives
							));
						}
					}
				}
			} catch (RuntimeException exception) {
				LOGGER.warn("Failed to decode the synchronized Madoku Quests state.", exception);
			}
		}
		questSnapshot = new QuestSnapshot(payload.nextQuestWorldTime(), questsByCategory);
		displayVersion++;
		if (!selectionConfigured) {
			QuestEntry firstMainQuest = questEntry("core", 0);
			if (firstMainQuest == null) return;
			selectedQuest = new QuestSelection(firstMainQuest.id());
			selectionConfigured = true;
			displayVersion++;
			saveSelection();
		}
	}

	private static List<QuestObjectiveEntry> readObjectiveEntries(
		JsonObject quest,
		String fallbackDescriptionKey,
		int fallbackProgress,
		int fallbackRequired
	) {
		List<QuestObjectiveEntry> objectives = new ArrayList<>();
		JsonElement element = quest == null ? null : quest.get("objectives");
		if (element != null && element.isJsonArray()) {
			for (JsonElement objectiveElement : element.getAsJsonArray()) {
				if (objectives.size() >= 3 || objectiveElement == null || !objectiveElement.isJsonObject()) break;
				JsonObject objective = objectiveElement.getAsJsonObject();
				int required = Math.max(1, readInt(objective, "required", 0));
				objectives.add(new QuestObjectiveEntry(
					readString(objective, "description-key", ""),
					readString(objective, "subcategory", "collect"),
					readString(objective, "target-id", ""),
					Math.max(0, readInt(objective, "progress", 0)),
					required
				));
			}
		}
		if (objectives.isEmpty() && fallbackRequired > 0) {
			objectives.add(new QuestObjectiveEntry(
				fallbackDescriptionKey,
				"collect",
				"",
				fallbackProgress,
				fallbackRequired
			));
		}
		return List.copyOf(objectives);
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
			boolean hasSelectionFields = rawSelection.has(QUEST_ID);
			JsonObject normalized = JSONFormatAPIManager.ensureManagedFile(file, defaults);
			boolean configured = readBoolean(normalized, SELECTION_CONFIGURED, false);
			JsonObject selection = readObject(normalized, SELECTED_QUEST);
			String questId = normalizeId(readString(selection, QUEST_ID, ""));
			selectionConfigured = hasSelectionFields && (configured || !questId.isBlank());
			selectedQuest = questId.isBlank()
				? null
				: new QuestSelection(questId);
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
				.put(QUEST_ID, ""))
			.build();
	}

	private static JsonObject buildConfigJson() {
		JSONFormatAPIManager.ObjectBuilder root = JSONFormatAPIManager.object();
		root.put(SELECTION_CONFIGURED, selectionConfigured);
		root.object(SELECTED_QUEST, selection -> selection.put(
			QUEST_ID,
			selectedQuest == null ? "" : selectedQuest.questId()
		));
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

	private static String normalizeId(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}

	public record QuestCategory(String id, String labelKey, Identifier iconTexture) {
	}

	record QuestSelection(String questId) {
	}

	private record CategoryDefinition(String id, String labelKey, Identifier moduleId, String iconName) {
	}

	public record QuestEntry(
		String id,
		String categoryId,
		String difficulty,
		String titleKey,
		String descriptionKey,
		int progress,
		int required,
		boolean complete,
		List<QuestObjectiveEntry> objectives
	) {
		public QuestEntry {
			objectives = objectives == null ? List.of() : List.copyOf(objectives);
		}
	}

	public record QuestObjectiveEntry(
		String descriptionKey,
		String subcategory,
		String targetId,
		int progress,
		int required
	) {
		public QuestObjectiveEntry {
			descriptionKey = descriptionKey == null ? "" : descriptionKey.trim();
			subcategory = subcategory == null ? "collect" : subcategory.trim().toLowerCase(Locale.ROOT);
			targetId = targetId == null ? "" : targetId.trim().toLowerCase(Locale.ROOT);
			required = Math.max(1, required);
			progress = Math.max(0, Math.min(progress, required));
		}
	}

	private record QuestSnapshot(
		long nextQuestWorldTime,
		Map<String, List<QuestEntry>> questsByCategory,
		Map<String, QuestEntry> questsById
	) {
		private QuestSnapshot(long nextQuestWorldTime, Map<String, List<QuestEntry>> questsByCategory) {
			this(nextQuestWorldTime, questsByCategory, Map.of());
		}

		private QuestSnapshot {
			Map<String, List<QuestEntry>> copy = new LinkedHashMap<>();
			Map<String, QuestEntry> index = new HashMap<>();
			if (questsByCategory != null) {
				questsByCategory.forEach((category, quests) -> {
					List<QuestEntry> immutableQuests = List.copyOf(quests);
					copy.put(category, immutableQuests);
					for (QuestEntry quest : immutableQuests) index.put(quest.id(), quest);
				});
			}
			questsByCategory = Map.copyOf(copy);
			questsById = Map.copyOf(index);
		}

		private static QuestSnapshot empty() {
			return new QuestSnapshot(-1L, Map.of());
		}
	}
}
