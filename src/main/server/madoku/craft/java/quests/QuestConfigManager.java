package madoku.craft.java.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.java.core.json.JSONAPIManager;
import madoku.craft.java.core.json.JSONFormatAPIManager;
import madoku.craft.java.core.json.JSONTypeAPIManager;
import madoku.craft.java.core.sync.SyncConfigAPIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads one managed JSON quest file per quest from each quest category folder. */
final class QuestConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestConfigManager.class);
	private static final String QUEST_GROUP = "quest-id";
	private static final String ID = "id";
	private static final String TITLE_KEY = "title-key";
	private static final String DESCRIPTION_KEY = "description-key";
	private static final String CATEGORY = "category";
	private static final String OBJECTIVES = "objectives";
	private static final String SUBCATEGORY = "subcategory";
	private static final String TARGET = "target";
	private static final String KIND = "kind";
	private static final String REQUIRED = "required";
	private static final String ENABLED = JSONTypeAPIManager.FIELD_ENABLED;
	private static final String CONFIG_ROOT_FOLDER = "madoku-craft-quests";

	private static final List<CategoryFolder> CATEGORY_FOLDERS = List.of(
		new CategoryFolder(QuestCategoryIds.CORE, "madoku-main-quests"),
		new CategoryFolder(QuestCategoryIds.FARMING, "madoku-farming-quests"),
		new CategoryFolder(QuestCategoryIds.MOBS, "madoku-mobs-quests"),
		new CategoryFolder(QuestCategoryIds.PETS, "madoku-pets-quests")
	);
	private static List<QuestDefinition> savedClientDefinitions = List.of();
	private static boolean clientSynchronized;

	private QuestConfigManager() {
	}

	static void initialize() {
		SyncConfigAPIManager.register(
			"quests",
			QuestConfigManager::createClientSyncSnapshot,
			QuestConfigManager::applyClientSyncSnapshot,
			QuestConfigManager::resetClientSyncState
		);
	}

	static void reload() {
		try {
			Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
			for (CategoryFolder category : CATEGORY_FOLDERS) {
				Path directory = JSONAPIManager.getOrCreateGlobalSystemDirectory(CONFIG_ROOT_FOLDER)
					.resolve(category.folderName());
				Map<String, JsonObject> files = JSONFormatAPIManager.ensureManagedFolder(
					directory,
					buildStaticDefaults(category.categoryId()),
					fileKey -> buildDefaultFile(category.categoryId(), fileKey),
					(fileKey, sourceRoot) -> true,
					null
				);
				for (Map.Entry<String, JsonObject> file : files.entrySet()) {
					if (!readBoolean(file.getValue(), ENABLED, true)) continue;
					QuestDefinition definition = parse(file.getKey(), category.categoryId(), file.getValue());
					if (definition == null) continue;
					if (definitions.putIfAbsent(definition.id(), definition) != null) {
						LOGGER.warn("Skipping duplicate Madoku quest definition '{}'.", definition.id());
					}
				}
			}
			QuestRegistry.replaceDefinitions(List.copyOf(definitions.values()));
			LOGGER.info("Loaded {} configurable Madoku quest definition(s).", definitions.size());
		} catch (IOException | RuntimeException exception) {
			QuestRegistry.replaceDefinitions(QuestRegistry.builtInDefinitions());
			LOGGER.error("Failed to load Madoku quest definitions; using built-in defaults.", exception);
		}
	}

	static String createClientSyncSnapshot() {
		JsonObject root = new JsonObject();
		JsonObject categories = new JsonObject();
		for (QuestDefinition definition : QuestRegistry.allDefinitions()) {
			JsonObject category = categories.has(definition.categoryId())
				&& categories.get(definition.categoryId()).isJsonObject()
				? categories.getAsJsonObject(definition.categoryId())
				: new JsonObject();
			category.add(definition.id(), toJson(definition));
			categories.add(definition.categoryId(), category);
		}
		root.add("categories", categories);
		return root.toString();
	}

	static void applyClientSyncSnapshot(String snapshot) {
		try {
			JsonElement parsed = com.google.gson.JsonParser.parseString(snapshot == null ? "" : snapshot);
			if (!parsed.isJsonObject()) return;
			JsonElement categoriesElement = parsed.getAsJsonObject().get("categories");
			if (categoriesElement == null || !categoriesElement.isJsonObject()) return;

			Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> categoryEntry : categoriesElement.getAsJsonObject().entrySet()) {
				if (!categoryEntry.getValue().isJsonObject()) continue;
				String categoryId = normalizeId(categoryEntry.getKey());
				for (Map.Entry<String, JsonElement> questEntry : categoryEntry.getValue().getAsJsonObject().entrySet()) {
					if (!questEntry.getValue().isJsonObject()) continue;
					QuestDefinition definition = parse(questEntry.getKey(), categoryId, questEntry.getValue().getAsJsonObject());
					if (definition != null) definitions.putIfAbsent(definition.id(), definition);
				}
			}

			captureClientSyncState();
			QuestRegistry.replaceDefinitions(List.copyOf(definitions.values()));
			clientSynchronized = true;
		} catch (RuntimeException exception) {
			LOGGER.warn("Failed to apply synchronized Madoku quest definitions.", exception);
		}
	}

	static void resetClientSyncState() {
		if (!clientSynchronized) return;
		QuestRegistry.replaceDefinitions(savedClientDefinitions);
		savedClientDefinitions = List.of();
		clientSynchronized = false;
	}

	private static void captureClientSyncState() {
		if (clientSynchronized) return;
		savedClientDefinitions = QuestRegistry.allDefinitions();
	}

	private static Map<String, JsonObject> buildStaticDefaults(String categoryId) {
		if (!QuestCategoryIds.CORE.equals(categoryId)) return Map.of();
		return Map.of(CoreQuestRegistrar.LUMBERJACK, buildDefaultFile(categoryId, CoreQuestRegistrar.LUMBERJACK));
	}

	private static JsonObject buildDefaultFile(String categoryId, String fileKey) {
		QuestDefinition builtIn = QuestRegistry.builtInDefinitions().stream()
			.filter(definition -> definition.categoryId().equals(categoryId) && definition.id().equals(normalizeId(fileKey)))
			.findFirst()
			.orElse(null);
		if (builtIn != null) return toJson(builtIn);

		String normalizedId = normalizeId(fileKey);
		return questFile(
			normalizedId,
			"menu.madoku-craft.quests." + normalizedId,
			"menu.madoku-craft.quests." + normalizedId + ".description",
			categoryId,
			List.of(QuestObjective.collects(QuestTarget.id("minecraft:air"), 1))
		);
	}

	private static JsonObject toJson(QuestDefinition definition) {
		List<QuestObjective> objectives = definition.objectives();
		List<ObjectiveData> objectiveData = new ArrayList<>();
		for (QuestObjective objective : objectives) {
			objectiveData.add(new ObjectiveData(
				objective.subcategory().id(),
				objective.target().kind().name().toLowerCase(Locale.ROOT),
				objective.target().id(),
				objective.requiredCount()
			));
		}
		return questFile(definition.id(), definition.titleKey(), definition.descriptionKey(), definition.categoryId(), objectiveData);
	}

	private static JsonObject questFile(
		String id,
		String titleKey,
		String descriptionKey,
		String categoryId,
		List<?> objectives
	) {
		JsonObject root = new JsonObject();
		JsonObject quest = new JsonObject();
		quest.addProperty(ID, id);
		quest.addProperty(TITLE_KEY, titleKey);
		quest.addProperty(DESCRIPTION_KEY, descriptionKey);
		quest.addProperty(CATEGORY, categoryId);
		JsonArray objectiveArray = new JsonArray();
		for (Object value : objectives) {
			JsonObject objective = new JsonObject();
			if (value instanceof QuestObjective questObjective) {
				objective.addProperty(SUBCATEGORY, questObjective.subcategory().id());
				objective.add(TARGET, targetJson(questObjective.target()));
				objective.addProperty(REQUIRED, questObjective.requiredCount());
			} else if (value instanceof ObjectiveData data) {
				objective.addProperty(SUBCATEGORY, data.subcategory());
				JsonObject target = new JsonObject();
				target.addProperty(KIND, data.kind());
				target.addProperty(ID, data.targetId());
				objective.add(TARGET, target);
				objective.addProperty(REQUIRED, data.required());
			}
			objectiveArray.add(objective);
		}
		quest.add(OBJECTIVES, objectiveArray);
		root.add(QUEST_GROUP, quest);
		return root;
	}

	private static JsonObject targetJson(QuestTarget target) {
		JsonObject json = new JsonObject();
		json.addProperty(KIND, target.kind().name().toLowerCase(Locale.ROOT));
		json.addProperty(ID, target.id());
		return json;
	}

	private static QuestDefinition parse(String fileKey, String categoryId, JsonObject sourceRoot) {
		JsonObject quest = objectField(sourceRoot, QUEST_GROUP);
		String id = normalizeId(readString(quest, ID, fileKey));
		String titleKey = readString(quest, TITLE_KEY, "");
		String descriptionKey = readString(quest, DESCRIPTION_KEY, "");
		String configuredCategory = normalizeId(readString(quest, CATEGORY, categoryId));
		if (id.isBlank() || titleKey.isBlank() || descriptionKey.isBlank() || !categoryId.equals(configuredCategory)) {
			LOGGER.warn("Skipping invalid quest file '{}' in '{}'.", fileKey, categoryId);
			return null;
		}

		JsonElement objectivesElement = quest.get(OBJECTIVES);
		if (objectivesElement == null || !objectivesElement.isJsonArray()) {
			LOGGER.warn("Skipping quest '{}' because objectives must be an array.", id);
			return null;
		}
		List<QuestObjective> objectives = new ArrayList<>();
		for (JsonElement element : objectivesElement.getAsJsonArray()) {
			QuestObjective objective = parseObjective(element);
			if (objective == null) {
				LOGGER.warn("Skipping quest '{}' because one objective is invalid.", id);
				return null;
			}
			objectives.add(objective);
		}
		try {
			return new QuestDefinition(id, titleKey, descriptionKey, categoryId, objectives);
		} catch (IllegalArgumentException exception) {
			LOGGER.warn("Skipping invalid quest definition '{}'.", id, exception);
			return null;
		}
	}

	private static QuestObjective parseObjective(JsonElement element) {
		if (element == null || !element.isJsonObject()) return null;
		JsonObject source = element.getAsJsonObject();
		QuestSubcategory subcategory = QuestSubcategory.fromId(readString(source, SUBCATEGORY, ""));
		JsonObject target = objectField(source, TARGET);
		QuestTarget.Kind kind = QuestTarget.Kind.fromId(readString(target, KIND, ""));
		String targetId = readString(target, ID, "");
		int required = readInt(source, REQUIRED, 0);
		if (subcategory == null || kind == null || targetId.isBlank() || required < 1) return null;
		try {
			return new QuestObjective(subcategory, new QuestTarget(kind, targetId), required);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static JsonObject objectField(JsonObject source, String key) {
		JsonElement element = source == null ? null : source.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
	}

	private static String normalizeId(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String readString(JsonObject source, String key, String fallback) {
		try {
			return source != null && source.has(key) ? source.get(key).getAsString().trim() : fallback;
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

	private record CategoryFolder(String categoryId, String folderName) {
	}

	private record ObjectiveData(String subcategory, String kind, String targetId, int required) {
	}
}
