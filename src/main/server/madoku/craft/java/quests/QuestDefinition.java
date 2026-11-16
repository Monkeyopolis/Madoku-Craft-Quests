package madoku.craft.java.quests;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.resources.Identifier;

/** Immutable definition for a quest that can be assigned to a category. */
public record QuestDefinition(
	String id,
	String titleKey,
	String descriptionKey,
	String categoryId,
	QuestRarity rarity,
	List<QuestObjective> objectives,
	Set<Identifier> requiredModules
) {
	private static final int MAX_OBJECTIVES = 3;

	public QuestDefinition(
		String id,
		String titleKey,
		String descriptionKey,
		String categoryId,
		List<QuestObjective> objectives
	) {
		this(id, titleKey, descriptionKey, categoryId, QuestRarity.COMMON, objectives, Set.of());
	}

	public QuestDefinition(
		String id,
		String titleKey,
		String descriptionKey,
		String categoryId,
		QuestRarity rarity,
		List<QuestObjective> objectives
	) {
		this(id, titleKey, descriptionKey, categoryId, rarity, objectives, Set.of());
	}

	public QuestDefinition {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Quest id must not be blank.");
		if (titleKey == null || titleKey.isBlank()) throw new IllegalArgumentException("Quest title key must not be blank.");
		if (descriptionKey == null || descriptionKey.isBlank()) throw new IllegalArgumentException("Quest description key must not be blank.");
		if (categoryId == null || categoryId.isBlank()) throw new IllegalArgumentException("Quest category id must not be blank.");
		if (rarity == null) throw new IllegalArgumentException("Quest rarity must not be null.");
		if (objectives == null || objectives.isEmpty()) throw new IllegalArgumentException("Quest must have at least one objective.");
		if (objectives.size() > MAX_OBJECTIVES) throw new IllegalArgumentException("Quest may have at most " + MAX_OBJECTIVES + " objectives.");
		if (requiredModules == null) throw new IllegalArgumentException("Quest required modules must not be null.");
		id = id.trim().toLowerCase(Locale.ROOT);
		categoryId = categoryId.trim().toLowerCase(Locale.ROOT);
		objectives = List.copyOf(objectives);
		requiredModules = Set.copyOf(requiredModules);
	}
}
