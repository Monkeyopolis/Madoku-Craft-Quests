package madoku.craft.java.quests;

import java.util.List;
import java.util.Locale;

/** Immutable definition for a quest that can be assigned to a category. */
public record QuestDefinition(
	String id,
	String titleKey,
	String descriptionKey,
	String categoryId,
	List<QuestObjective> objectives
) {
	public QuestDefinition {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Quest id must not be blank.");
		if (titleKey == null || titleKey.isBlank()) throw new IllegalArgumentException("Quest title key must not be blank.");
		if (descriptionKey == null || descriptionKey.isBlank()) throw new IllegalArgumentException("Quest description key must not be blank.");
		if (categoryId == null || categoryId.isBlank()) throw new IllegalArgumentException("Quest category id must not be blank.");
		if (objectives == null || objectives.isEmpty()) throw new IllegalArgumentException("Quest must have at least one objective.");
		id = id.trim().toLowerCase(Locale.ROOT);
		categoryId = categoryId.trim().toLowerCase(Locale.ROOT);
		objectives = List.copyOf(objectives);
	}
}
