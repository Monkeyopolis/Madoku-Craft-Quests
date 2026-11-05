package madoku.craft.java.quests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One independently tracked requirement belonging to a quest. */
public record QuestObjective(
	QuestSubcategory subcategory,
	QuestTarget target,
	int requiredCount,
	String descriptionKey,
	List<String> displayTargetIds
) {
	public QuestObjective(QuestSubcategory subcategory, QuestTarget target, int requiredCount) {
		this(subcategory, target, requiredCount, "", List.of());
	}

	public QuestObjective(QuestSubcategory subcategory, QuestTarget target, int requiredCount, String descriptionKey) {
		this(subcategory, target, requiredCount, descriptionKey, List.of());
	}

	public QuestObjective {
		if (subcategory == null) throw new IllegalArgumentException("Quest objective subcategory must not be null.");
		if (target == null) throw new IllegalArgumentException("Quest objective target must not be null.");
		if (requiredCount < 1) throw new IllegalArgumentException("Quest objective required count must be positive.");
		descriptionKey = descriptionKey == null ? "" : descriptionKey.trim();
		List<String> normalizedDisplayTargetIds = new ArrayList<>();
		if (displayTargetIds != null) {
			for (String value : displayTargetIds) {
				if (value != null && !value.isBlank()) normalizedDisplayTargetIds.add(value.trim().toLowerCase(Locale.ROOT));
			}
		}
		if (normalizedDisplayTargetIds.isEmpty()) normalizedDisplayTargetIds.addAll(target.ids());
		displayTargetIds = List.copyOf(normalizedDisplayTargetIds);
	}

	public static QuestObjective breaks(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.BREAK, target, requiredCount);
	}

	public static QuestObjective breaks(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.BREAK, target, requiredCount, descriptionKey);
	}

	public static QuestObjective collects(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.COLLECT, target, requiredCount);
	}

	public static QuestObjective collects(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.COLLECT, target, requiredCount, descriptionKey);
	}

	public static QuestObjective crafts(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.CRAFTING, target, requiredCount);
	}

	public static QuestObjective crafts(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.CRAFTING, target, requiredCount, descriptionKey);
	}

	public static QuestObjective enchants(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.ENCHANT, target, requiredCount);
	}

	public static QuestObjective enchants(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.ENCHANT, target, requiredCount, descriptionKey);
	}

	public static QuestObjective smelts(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.SMELT, target, requiredCount);
	}

	public static QuestObjective smelts(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.SMELT, target, requiredCount, descriptionKey);
	}

	public static QuestObjective smelts(
		QuestTarget target,
		int requiredCount,
		String descriptionKey,
		List<String> displayTargetIds
	) {
		return new QuestObjective(QuestSubcategory.SMELT, target, requiredCount, descriptionKey, displayTargetIds);
	}

	public static QuestObjective strips(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.STRIP, target, requiredCount);
	}

	public static QuestObjective strips(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.STRIP, target, requiredCount, descriptionKey);
	}

	public static QuestObjective uses(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.USE, target, requiredCount);
	}

	public static QuestObjective uses(QuestTarget target, int requiredCount, String descriptionKey) {
		return new QuestObjective(QuestSubcategory.USE, target, requiredCount, descriptionKey);
	}
}
