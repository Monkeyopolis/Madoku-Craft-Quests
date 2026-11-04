package madoku.craft.java.quests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Central registry for standard quest definitions and weighted assignment. */
public final class QuestRegistry {
	public static final String CORE_CATEGORY = QuestCategoryIds.CORE;
	public static final String FARMING_CATEGORY = QuestCategoryIds.FARMING;
	public static final String MOBS_CATEGORY = QuestCategoryIds.MOBS;
	public static final String PETS_CATEGORY = QuestCategoryIds.PETS;
	public static final String LUMBERJACK = CoreQuestRegistrar.LUMBERJACK;
	public static final String STONEWORKS = CoreQuestRegistrar.STONEWORKS;
	public static final String DIRT_COLLECTOR = CoreQuestRegistrar.DIRT_COLLECTOR;
	public static final String COLLECTING_FLINT = CoreQuestRegistrar.COLLECTING_FLINT;
	public static final String COAL_MINER = CoreQuestRegistrar.COAL_MINER;
	public static final String DIAMONDS = CoreQuestRegistrar.DIAMONDS;
	public static final String OBSIDIAN = CoreQuestRegistrar.OBSIDIAN;
	public static final String FLAMING_HOT = CoreQuestRegistrar.FLAMING_HOT;
	public static final String GLASS_MAKER = CoreQuestRegistrar.GLASS_MAKER;
	public static final String SHINY_GOLD = CoreQuestRegistrar.SHINY_GOLD;
	public static final String STURDY_IRON = CoreQuestRegistrar.STURDY_IRON;
	public static final String COPPER_SPECIALIST = CoreQuestRegistrar.COPPER_SPECIALIST;
	public static final String ORE_SMELTER = CoreQuestRegistrar.ORE_SMELTER;
	public static final String STONE_VARIETY = CoreQuestRegistrar.STONE_VARIETY;
	public static final String PROCESSING_LOGS = CoreQuestRegistrar.PROCESSING_LOGS;

	private static final Map<String, QuestDefinition> DEFINITIONS = new LinkedHashMap<>();
	private static final List<QuestCategoryRegistrar> BUILT_IN_REGISTRARS = List.of(
		new CoreQuestRegistrar(),
		new FarmingQuestRegistrar(),
		new MobsQuestRegistrar(),
		new PetsQuestRegistrar()
	);

	static {
		for (QuestCategoryRegistrar registrar : BUILT_IN_REGISTRARS) registerCategory(registrar);
	}

	private QuestRegistry() {
	}

	public static synchronized void register(QuestDefinition definition) {
		if (definition == null) throw new IllegalArgumentException("Quest definition must not be null.");
		if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
			throw new IllegalArgumentException("Quest definition is already registered: " + definition.id());
		}
	}

	public static synchronized void registerCategory(QuestCategoryRegistrar registrar) {
		if (registrar == null) throw new IllegalArgumentException("Quest category registrar must not be null.");
		if (registrar.categoryId() == null || registrar.categoryId().isBlank()) {
			throw new IllegalArgumentException("Quest category registrar id must not be blank.");
		}
		List<QuestDefinition> definitions = registrar.definitions();
		if (definitions == null) throw new IllegalArgumentException("Quest category definitions must not be null.");
		for (QuestDefinition definition : definitions) {
			if (!registrar.categoryId().equals(definition.categoryId())) {
				throw new IllegalArgumentException(
					"Quest definition category does not match registrar: " + definition.id()
				);
			}
			register(definition);
		}
	}

	public static synchronized List<QuestDefinition> builtInDefinitions() {
		List<QuestDefinition> definitions = new ArrayList<>();
		for (QuestCategoryRegistrar registrar : BUILT_IN_REGISTRARS) {
			List<QuestDefinition> registered = registrar.definitions();
			if (registered != null) definitions.addAll(registered);
		}
		return List.copyOf(definitions);
	}

	public static synchronized List<QuestDefinition> allDefinitions() {
		return List.copyOf(DEFINITIONS.values());
	}

	public static synchronized void replaceDefinitions(List<QuestDefinition> definitions) {
		Map<String, QuestDefinition> replacement = new LinkedHashMap<>();
		if (definitions != null) {
			for (QuestDefinition definition : definitions) {
				if (definition == null) throw new IllegalArgumentException("Quest definition must not be null.");
				if (replacement.putIfAbsent(definition.id(), definition) != null) {
					throw new IllegalArgumentException("Quest definition is already registered: " + definition.id());
				}
			}
		}
		DEFINITIONS.clear();
		DEFINITIONS.putAll(replacement);
	}

	public static QuestDefinition get(String id) {
		return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase(Locale.ROOT));
	}

	public static QuestDefinition firstMainQuest() {
		QuestDefinition lumberjack = get(LUMBERJACK);
		if (lumberjack != null
			&& CORE_CATEGORY.equals(lumberjack.categoryId())
			&& lumberjack.rarity() == QuestRarity.COMMON) {
			return lumberjack;
		}
		return forCategory(CORE_CATEGORY).stream()
			.filter(definition -> definition.rarity() == QuestRarity.COMMON)
			.findFirst()
			.orElse(null);
	}

	public static List<QuestDefinition> forCategory(String categoryId) {
		if (categoryId == null || categoryId.isBlank()) return List.of();
		List<QuestDefinition> definitions = new ArrayList<>();
		for (QuestDefinition definition : DEFINITIONS.values()) {
			if (categoryId.equals(definition.categoryId())) definitions.add(definition);
		}
		return List.copyOf(definitions);
	}

	public static boolean hasCategory(String categoryId) {
		return !forCategory(categoryId).isEmpty();
	}

	public static boolean hasAvailable(String categoryId, Set<String> excludedIds) {
		return forCategory(categoryId).stream().anyMatch(definition -> excludedIds == null || !excludedIds.contains(definition.id()));
	}

	public static QuestAssignment choose(Random random, String categoryId, Set<String> excludedIds) {
		List<QuestDefinition> definitions = forCategory(categoryId).stream()
			.filter(definition -> excludedIds == null || !excludedIds.contains(definition.id()))
			.toList();
		if (definitions.isEmpty()) return null;

		List<QuestRarity> availableRarities = new ArrayList<>();
		for (QuestRarity rarity : QuestRarity.values()) {
			if (definitions.stream().anyMatch(definition -> definition.rarity() == rarity)) {
				availableRarities.add(rarity);
			}
		}
		QuestRarity selectedRarity = chooseRarity(random, availableRarities);
		List<QuestDifficulty> availableDifficulties = List.of(QuestDifficulty.values());
		QuestDifficulty selectedDifficulty = chooseDifficulty(random, availableDifficulties);
		List<QuestDefinition> matchingDefinitions = definitions.stream()
			.filter(definition -> definition.rarity() == selectedRarity)
			.toList();
		return new QuestAssignment(
			matchingDefinitions.get(random.nextInt(matchingDefinitions.size())),
			selectedDifficulty
		);
	}

	private static QuestRarity chooseRarity(Random random, List<QuestRarity> availableRarities) {
		int totalWeight = availableRarities.stream().mapToInt(QuestRarity::selectionWeight).sum();
		if (totalWeight <= 0) return availableRarities.get(0);
		int selectedWeight = random.nextInt(totalWeight);
		for (QuestRarity rarity : availableRarities) {
			selectedWeight -= rarity.selectionWeight();
			if (selectedWeight < 0) return rarity;
		}
		return availableRarities.get(availableRarities.size() - 1);
	}

	private static QuestDifficulty chooseDifficulty(Random random, List<QuestDifficulty> availableDifficulties) {
		int totalWeight = availableDifficulties.stream().mapToInt(QuestDifficulty::weight).sum();
		if (totalWeight <= 0) return QuestDifficulty.EASY;
		int selectedWeight = random.nextInt(totalWeight);
		for (QuestDifficulty difficulty : availableDifficulties) {
			selectedWeight -= difficulty.weight();
			if (selectedWeight < 0) return difficulty;
		}
		return availableDifficulties.get(availableDifficulties.size() - 1);
	}
}
