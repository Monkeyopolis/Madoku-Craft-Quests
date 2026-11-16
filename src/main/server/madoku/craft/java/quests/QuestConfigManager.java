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
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.resources.Identifier;

/** Loads one managed JSON quest file per quest from each quest category folder. */
final class QuestConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestConfigManager.class);
	private static final String QUEST_GROUP = "quest-id";
	private static final String ID = "id";
	private static final String TITLE_KEY = "title-key";
	private static final String DESCRIPTION_KEY = "description-key";
	private static final String CATEGORY = "category";
	private static final String RARITY = "rarity";
	private static final String REQUIRED_MODULES = "required-modules";
	private static final String OBJECTIVES = "objectives";
	private static final String OBJECTIVE_DESCRIPTION_KEY = "description-key";
	private static final String SUBCATEGORY = "subcategory";
	private static final String TARGET = "target";
	private static final String TARGET_IDS = "ids";
	// Optional namespaced variant identity; currently valid on kill objectives only.
	private static final String TARGET_VARIANT = "variant";
	private static final String DISPLAY_TARGET_IDS = "display-target-ids";
	private static final String KIND = "kind";
	private static final String REQUIRED = "required";
	private static final String ENABLED = JSONTypeAPIManager.FIELD_ENABLED;
	private static final String CONFIG_ROOT_FOLDER = "madoku-craft-quests";

	private static final List<CategoryFolder> CATEGORY_FOLDERS = List.of(
		new CategoryFolder(QuestCategoryIds.CORE, "madoku-main-quests"),
		new CategoryFolder(QuestCategoryIds.COMPAT, "madoku-compat-quests"),
		new CategoryFolder(QuestCategoryIds.FARMING, "madoku-farming-quests"),
		new CategoryFolder(QuestCategoryIds.MOBS, "madoku-mobs-quests")
	);
	private static final Map<String, QuestDefinition> BUILT_IN_DEFINITIONS = indexBuiltInDefinitions();
	private static final Map<String, QuestDefinition> BUILT_IN_DEFINITIONS_BY_ID = indexBuiltInDefinitionsById();
	private static List<QuestDefinition> savedClientDefinitions = List.of();
	private static boolean clientSynchronized;

	private QuestConfigManager() {
	}

	private static Map<String, QuestDefinition> indexBuiltInDefinitions() {
		Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
		for (QuestDefinition definition : QuestRegistry.builtInDefinitions()) {
			definitions.put(builtInLookupKey(definition.categoryId(), definition.id()), definition);
		}
		return Map.copyOf(definitions);
	}

	private static String builtInLookupKey(String categoryId, String questId) {
		return categoryId + ":" + questId;
	}

	private static Map<String, QuestDefinition> indexBuiltInDefinitionsById() {
		Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
		for (QuestDefinition definition : BUILT_IN_DEFINITIONS.values()) {
			definitions.put(definition.id(), definition);
		}
		return Map.copyOf(definitions);
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
		if (QuestCategoryIds.CORE.equals(categoryId)) return Map.ofEntries(
			Map.entry(CoreQuestRegistrar.LUMBERJACK, buildDefaultFile(categoryId, CoreQuestRegistrar.LUMBERJACK)),
			Map.entry(CoreQuestRegistrar.STONEWORKS, buildDefaultFile(categoryId, CoreQuestRegistrar.STONEWORKS)),
			Map.entry(CoreQuestRegistrar.DIRT_COLLECTOR, buildDefaultFile(categoryId, CoreQuestRegistrar.DIRT_COLLECTOR)),
			Map.entry(CoreQuestRegistrar.COLLECTING_FLINT, buildDefaultFile(categoryId, CoreQuestRegistrar.COLLECTING_FLINT)),
			Map.entry(CoreQuestRegistrar.COAL_MINER, buildDefaultFile(categoryId, CoreQuestRegistrar.COAL_MINER)),
			Map.entry(CoreQuestRegistrar.DIAMONDS, buildDefaultFile(categoryId, CoreQuestRegistrar.DIAMONDS)),
			Map.entry(CoreQuestRegistrar.OBSIDIAN, buildDefaultFile(categoryId, CoreQuestRegistrar.OBSIDIAN)),
			Map.entry(CoreQuestRegistrar.FLAMING_HOT, buildDefaultFile(categoryId, CoreQuestRegistrar.FLAMING_HOT)),
			Map.entry(CoreQuestRegistrar.GLASS_MAKER, buildDefaultFile(categoryId, CoreQuestRegistrar.GLASS_MAKER)),
			Map.entry(CoreQuestRegistrar.SHINY_GOLD, buildDefaultFile(categoryId, CoreQuestRegistrar.SHINY_GOLD)),
			Map.entry(CoreQuestRegistrar.STURDY_IRON, buildDefaultFile(categoryId, CoreQuestRegistrar.STURDY_IRON)),
			Map.entry(CoreQuestRegistrar.COPPER_SPECIALIST, buildDefaultFile(categoryId, CoreQuestRegistrar.COPPER_SPECIALIST)),
			Map.entry(CoreQuestRegistrar.ORE_SMELTER, buildDefaultFile(categoryId, CoreQuestRegistrar.ORE_SMELTER)),
			Map.entry(CoreQuestRegistrar.STONE_VARIETY, buildDefaultFile(categoryId, CoreQuestRegistrar.STONE_VARIETY)),
			Map.entry(CoreQuestRegistrar.THESE_ARE_STONES, buildDefaultFile(categoryId, CoreQuestRegistrar.THESE_ARE_STONES)),
			Map.entry(CoreQuestRegistrar.MASONRY, buildDefaultFile(categoryId, CoreQuestRegistrar.MASONRY)),
			Map.entry(CoreQuestRegistrar.LET_THERE_BE_LIGHT, buildDefaultFile(categoryId, CoreQuestRegistrar.LET_THERE_BE_LIGHT)),
			Map.entry(CoreQuestRegistrar.INFINITE_DIRT, buildDefaultFile(categoryId, CoreQuestRegistrar.INFINITE_DIRT)),
			Map.entry(CoreQuestRegistrar.CHAINMAIL_ARMOR, buildDefaultFile(categoryId, CoreQuestRegistrar.CHAINMAIL_ARMOR)),
			Map.entry(CoreQuestRegistrar.ENCHANTING_BOTTLES, buildDefaultFile(categoryId, CoreQuestRegistrar.ENCHANTING_BOTTLES)),
			Map.entry(CoreQuestRegistrar.MAKING_FIRE, buildDefaultFile(categoryId, CoreQuestRegistrar.MAKING_FIRE))
		);
		if (QuestCategoryIds.FARMING.equals(categoryId)) return Map.ofEntries(
			Map.entry(FarmingQuestRegistrar.GROWING_POTATOES, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_POTATOES)),
			Map.entry(FarmingQuestRegistrar.GROWING_WHEAT, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_WHEAT)),
			Map.entry(FarmingQuestRegistrar.GROWING_SWEET_BERRIES, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_SWEET_BERRIES)),
			Map.entry(FarmingQuestRegistrar.GROWING_PUMPKINS, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_PUMPKINS)),
			Map.entry(FarmingQuestRegistrar.GROWING_MELONS, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_MELONS)),
			Map.entry(FarmingQuestRegistrar.PUMPKIN_PIES, buildDefaultFile(categoryId, FarmingQuestRegistrar.PUMPKIN_PIES)),
			Map.entry(FarmingQuestRegistrar.GROWING_BEETROOT, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_BEETROOT)),
			Map.entry(FarmingQuestRegistrar.BOUNTIFUL_HARVESTS, buildDefaultFile(categoryId, FarmingQuestRegistrar.BOUNTIFUL_HARVESTS)),
			Map.entry(FarmingQuestRegistrar.GROWING_CARROTS, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_CARROTS)),
			Map.entry(FarmingQuestRegistrar.GOLDEN_CARROTS, buildDefaultFile(categoryId, FarmingQuestRegistrar.GOLDEN_CARROTS)),
			Map.entry(FarmingQuestRegistrar.ARE_THOSE_STEMS, buildDefaultFile(categoryId, FarmingQuestRegistrar.ARE_THOSE_STEMS)),
			Map.entry(FarmingQuestRegistrar.THESE_ARE_CROPS, buildDefaultFile(categoryId, FarmingQuestRegistrar.THESE_ARE_CROPS)),
			Map.entry(FarmingQuestRegistrar.MAKING_BREAD, buildDefaultFile(categoryId, FarmingQuestRegistrar.MAKING_BREAD)),
			Map.entry(FarmingQuestRegistrar.BAKING_COOKIES, buildDefaultFile(categoryId, FarmingQuestRegistrar.BAKING_COOKIES)),
			Map.entry(FarmingQuestRegistrar.BAKING_POTATOES, buildDefaultFile(categoryId, FarmingQuestRegistrar.BAKING_POTATOES)),
			Map.entry(FarmingQuestRegistrar.GROW_FASTER, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROW_FASTER)),
			Map.entry(FarmingQuestRegistrar.GROWING_SUGAR_CANE, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_SUGAR_CANE)),
			Map.entry(FarmingQuestRegistrar.GROWING_GLOW_BERRIES, buildDefaultFile(categoryId, FarmingQuestRegistrar.GROWING_GLOW_BERRIES))
		);
		if (QuestCategoryIds.COMPAT.equals(categoryId)) return Map.ofEntries(
			Map.entry(FarmingQuestRegistrar.CUTTING_MELONS, buildDefaultFile(categoryId, FarmingQuestRegistrar.CUTTING_MELONS)),
			Map.entry(CompatQuestRegistrar.MAKING_GLOWING_PIES, buildDefaultFile(categoryId, CompatQuestRegistrar.MAKING_GLOWING_PIES)),
			Map.entry(CompatQuestRegistrar.COOKING_BEETROOT_SOUP, buildDefaultFile(categoryId, CompatQuestRegistrar.COOKING_BEETROOT_SOUP)),
			Map.entry(CompatQuestRegistrar.BAKING_SWEET_BERRY_COOKIES, buildDefaultFile(categoryId, CompatQuestRegistrar.BAKING_SWEET_BERRY_COOKIES)),
			Map.entry(CompatQuestRegistrar.FISHING_FOR_COOKED_TROPICAL_FISH, buildDefaultFile(categoryId, CompatQuestRegistrar.FISHING_FOR_COOKED_TROPICAL_FISH))
		);
		if (QuestCategoryIds.MOBS.equals(categoryId)) return Map.ofEntries(
			Map.entry(MobsQuestRegistrar.KILLING_SKELETONS, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_SKELETONS)),
			Map.entry(MobsQuestRegistrar.KILLING_STRAYS, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_STRAYS)),
			Map.entry(MobsQuestRegistrar.TYPES_OF_SKELETONS, buildDefaultFile(categoryId, MobsQuestRegistrar.TYPES_OF_SKELETONS)),
			Map.entry(MobsQuestRegistrar.KILLING_ZOMBIES, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_ZOMBIES)),
			Map.entry(MobsQuestRegistrar.KILLING_CREEPERS, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_CREEPERS)),
			Map.entry(MobsQuestRegistrar.KILLING_SPIDERS, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_SPIDERS)),
			Map.entry(MobsQuestRegistrar.KILLING_ENDERMEN, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_ENDERMEN)),
			Map.entry(MobsQuestRegistrar.KILLING_DROWNED, buildDefaultFile(categoryId, MobsQuestRegistrar.KILLING_DROWNED)),
			Map.entry(MobsQuestRegistrar.THATS_A_STRAY, buildDefaultFile(categoryId, MobsQuestRegistrar.THATS_A_STRAY)),
			Map.entry(MobsQuestRegistrar.STRANGE_SKELETON, buildDefaultFile(categoryId, MobsQuestRegistrar.STRANGE_SKELETON)),
			Map.entry(MobsQuestRegistrar.THATS_A_ZOMBIE, buildDefaultFile(categoryId, MobsQuestRegistrar.THATS_A_ZOMBIE)),
			Map.entry(MobsQuestRegistrar.CREEPER_ON_FIRE, buildDefaultFile(categoryId, MobsQuestRegistrar.CREEPER_ON_FIRE)),
			Map.entry(MobsQuestRegistrar.SPIDER_FAMILY, buildDefaultFile(categoryId, MobsQuestRegistrar.SPIDER_FAMILY)),
			Map.entry(MobsQuestRegistrar.STRANGE_ENDERMAN, buildDefaultFile(categoryId, MobsQuestRegistrar.STRANGE_ENDERMAN)),
			Map.entry(MobsQuestRegistrar.ELITE_MOB_KILLER, buildDefaultFile(categoryId, MobsQuestRegistrar.ELITE_MOB_KILLER))
		);
		return Map.of();
	}

	private static JsonObject buildDefaultFile(String categoryId, String fileKey) {
		QuestDefinition builtIn = BUILT_IN_DEFINITIONS.get(builtInLookupKey(categoryId, normalizeId(fileKey)));
		if (builtIn != null) return toJson(builtIn);

		String normalizedId = normalizeId(fileKey);
		return questFile(
			normalizedId,
			"menu.madoku-craft.quests." + normalizedId,
			"menu.madoku-craft.quests." + normalizedId + ".description",
			categoryId,
			QuestRarity.COMMON.id(),
			List.of(QuestObjective.collects(QuestTarget.id("minecraft:air"), 1)),
			Set.of()
		);
	}

	private static JsonObject toJson(QuestDefinition definition) {
		return questFile(
			definition.id(),
			definition.titleKey(),
			definition.descriptionKey(),
			definition.categoryId(),
			definition.rarity().id(),
			definition.objectives(),
			definition.requiredModules()
		);
	}

	private static JsonObject questFile(
		String id,
		String titleKey,
		String descriptionKey,
		String categoryId,
		String rarity,
		List<?> objectives,
		Set<Identifier> requiredModules
	) {
		JsonObject root = new JsonObject();
		JsonObject quest = new JsonObject();
		quest.addProperty(ID, id);
		quest.addProperty(TITLE_KEY, titleKey);
		quest.addProperty(DESCRIPTION_KEY, descriptionKey);
		quest.addProperty(CATEGORY, categoryId);
		quest.addProperty(RARITY, rarity);
		JsonArray requiredModuleArray = new JsonArray();
		for (Identifier moduleId : requiredModules) requiredModuleArray.add(moduleId.toString());
		quest.add(REQUIRED_MODULES, requiredModuleArray);
		JsonArray objectiveArray = new JsonArray();
		for (Object value : objectives) {
			JsonObject objective = new JsonObject();
			if (value instanceof QuestObjective questObjective) {
				objective.addProperty(SUBCATEGORY, questObjective.subcategory().id());
				if (!questObjective.descriptionKey().isBlank()) objective.addProperty(OBJECTIVE_DESCRIPTION_KEY, questObjective.descriptionKey());
				objective.add(TARGET, targetJson(questObjective.target()));
				if (!questObjective.displayTargetIds().equals(questObjective.target().ids())) {
					JsonArray displayTargetIds = new JsonArray();
					for (String targetId : questObjective.displayTargetIds()) displayTargetIds.add(targetId);
					objective.add(DISPLAY_TARGET_IDS, displayTargetIds);
				}
				objective.addProperty(REQUIRED, questObjective.requiredCount());
			}
			objectiveArray.add(objective);
		}
		quest.add(OBJECTIVES, objectiveArray);
		root.add(QUEST_GROUP, quest);
		return root;
	}

	private static JsonObject targetJson(QuestTarget target) {
		JsonObject json = new JsonObject();
		String kindId = target.kind() == QuestTarget.Kind.ANY_OF
			? "any-of"
			: target.kind().name().toLowerCase(Locale.ROOT);
		json.addProperty(KIND, kindId);
		if (target.kind() == QuestTarget.Kind.ANY_OF) {
			JsonArray ids = new JsonArray();
			for (String id : target.ids()) ids.add(id);
			json.add(TARGET_IDS, ids);
		} else {
			json.addProperty(ID, target.id());
		}
		if (!target.variantId().isBlank()) json.addProperty(TARGET_VARIANT, target.variantId());
		return json;
	}

	private static QuestDefinition parse(String fileKey, String categoryId, JsonObject sourceRoot) {
		JsonObject quest = objectField(sourceRoot, QUEST_GROUP);
		String id = normalizeId(readString(quest, ID, ""));
		String titleKey = readString(quest, TITLE_KEY, "");
		String descriptionKey = readString(quest, DESCRIPTION_KEY, "");
		String configuredCategory = normalizeId(readString(quest, CATEGORY, categoryId));
		QuestRarity rarity = QuestRarity.fromId(readString(quest, RARITY, QuestRarity.COMMON.id()));
		if (id.isBlank() || titleKey.isBlank() || descriptionKey.isBlank() || !categoryId.equals(configuredCategory)) {
			LOGGER.warn("Skipping invalid quest file '{}' in '{}'.", fileKey, categoryId);
			return null;
		}
		QuestDefinition builtIn = BUILT_IN_DEFINITIONS_BY_ID.get(id);
		if (builtIn != null && !builtIn.categoryId().equals(categoryId)) {
			LOGGER.warn("Skipping quest '{}' because it belongs to category '{}', not '{}'.", id, builtIn.categoryId(), categoryId);
			return null;
		}
		if (rarity == null) {
			LOGGER.warn("Skipping quest '{}' because rarity is invalid.", id);
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
		Set<Identifier> requiredModules = new LinkedHashSet<>();
		for (String moduleId : readStrings(quest, REQUIRED_MODULES)) {
			Identifier parsedModuleId = Identifier.tryParse(moduleId);
			if (parsedModuleId == null) {
				LOGGER.warn("Skipping quest '{}' because required module ID '{}' is invalid.", id, moduleId);
				return null;
			}
			requiredModules.add(parsedModuleId);
		}
		if (builtIn != null) requiredModules.addAll(builtIn.requiredModules());
		try {
			return new QuestDefinition(id, titleKey, descriptionKey, categoryId, rarity, objectives, requiredModules);
		} catch (IllegalArgumentException exception) {
			LOGGER.warn("Skipping invalid quest definition '{}'.", id, exception);
			return null;
		}
	}

	private static QuestObjective parseObjective(JsonElement element) {
		if (element == null || !element.isJsonObject()) return null;
		JsonObject source = element.getAsJsonObject();
		QuestSubcategory subcategory = QuestSubcategory.fromId(readString(source, SUBCATEGORY, ""));
		String descriptionKey = readString(source, OBJECTIVE_DESCRIPTION_KEY, "");
		JsonObject target = objectField(source, TARGET);
		QuestTarget.Kind kind = QuestTarget.Kind.fromId(readString(target, KIND, ""));
		String targetId = readString(target, ID, "");
		String variantId = readString(target, TARGET_VARIANT, "");
		List<String> targetIds = readStrings(target, TARGET_IDS);
		List<String> displayTargetIds = readStrings(source, DISPLAY_TARGET_IDS);
		int required = readInt(source, REQUIRED, 0);
		if (subcategory == null || kind == null || (!variantId.isBlank() && subcategory != QuestSubcategory.KILL)
			|| (kind != QuestTarget.Kind.ANY_OF && targetId.isBlank())
			|| (kind == QuestTarget.Kind.ANY_OF && targetIds.isEmpty()) || required < 1) return null;
		try {
			QuestTarget questTarget = kind == QuestTarget.Kind.ANY_OF
				? new QuestTarget(kind, null, targetIds, variantId)
				: new QuestTarget(kind, targetId, List.of(targetId), variantId);
			return new QuestObjective(subcategory, questTarget, required, descriptionKey, displayTargetIds);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static List<String> readStrings(JsonObject source, String key) {
		JsonElement element = source == null ? null : source.get(key);
		if (element == null || !element.isJsonArray()) return List.of();
		List<String> values = new ArrayList<>();
		for (JsonElement value : element.getAsJsonArray()) {
			if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				String string = value.getAsString().trim();
				if (!string.isBlank()) values.add(string);
			}
		}
		return List.copyOf(values);
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

}
