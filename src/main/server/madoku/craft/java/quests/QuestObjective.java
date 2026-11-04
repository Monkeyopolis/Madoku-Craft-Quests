package madoku.craft.java.quests;

/** One independently tracked requirement belonging to a quest. */
public record QuestObjective(
	QuestSubcategory subcategory,
	QuestTarget target,
	int requiredCount,
	String descriptionKey
) {
	public QuestObjective(QuestSubcategory subcategory, QuestTarget target, int requiredCount) {
		this(subcategory, target, requiredCount, "");
	}

	public QuestObjective {
		if (subcategory == null) throw new IllegalArgumentException("Quest objective subcategory must not be null.");
		if (target == null) throw new IllegalArgumentException("Quest objective target must not be null.");
		if (requiredCount < 1) throw new IllegalArgumentException("Quest objective required count must be positive.");
		descriptionKey = descriptionKey == null ? "" : descriptionKey.trim();
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
}
