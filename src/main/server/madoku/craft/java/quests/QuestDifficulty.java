package madoku.craft.java.quests;

/** Standard difficulty tiers and their relative selection weights. */
public enum QuestDifficulty {
	EASY("easy", 60, 1, "I"),
	NORMAL("normal", 30, 2, "II"),
	HARD("hard", 10, 4, "III");

	private final String id;
	private final int weight;
	private final int objectiveMultiplier;
	private final String tierLabel;

	QuestDifficulty(String id, int weight, int objectiveMultiplier, String tierLabel) {
		this.id = id;
		this.weight = weight;
		this.objectiveMultiplier = objectiveMultiplier;
		this.tierLabel = tierLabel;
	}

	public String id() {
		return id;
	}

	public int weight() {
		return weight;
	}

	public int objectiveMultiplier() {
		return objectiveMultiplier;
	}

	public String tierLabel() {
		return tierLabel;
	}

	public int scaledCount(int baseCount) {
		long scaled = (long) Math.max(0, baseCount) * objectiveMultiplier;
		return (int) Math.min(Integer.MAX_VALUE, scaled);
	}

	public static QuestDifficulty fromId(String id) {
		if (id == null) return null;
		for (QuestDifficulty difficulty : values()) {
			if (difficulty.id.equals(id.trim().toLowerCase(java.util.Locale.ROOT))) return difficulty;
		}
		return null;
	}
}
