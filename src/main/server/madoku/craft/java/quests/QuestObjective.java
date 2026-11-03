package madoku.craft.java.quests;

/** One independently tracked requirement belonging to a quest. */
public record QuestObjective(QuestSubcategory subcategory, QuestTarget target, int requiredCount) {
	public QuestObjective {
		if (subcategory == null) throw new IllegalArgumentException("Quest objective subcategory must not be null.");
		if (target == null) throw new IllegalArgumentException("Quest objective target must not be null.");
		if (requiredCount < 1) throw new IllegalArgumentException("Quest objective required count must be positive.");
	}

	public static QuestObjective breaks(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.BREAK, target, requiredCount);
	}

	public static QuestObjective collects(QuestTarget target, int requiredCount) {
		return new QuestObjective(QuestSubcategory.COLLECT, target, requiredCount);
	}
}
