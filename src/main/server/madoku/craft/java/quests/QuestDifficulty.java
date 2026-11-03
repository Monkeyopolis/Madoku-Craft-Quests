package madoku.craft.java.quests;

/** Standard difficulty tiers and their relative selection weights. */
public enum QuestDifficulty {
	EASY("easy", 60, 1, 80, 212, 4, "I"),
	NORMAL("normal", 30, 2, 160, 424, 8, "II"),
	HARD("hard", 10, 4, 320, 848, 20, "III");

	private final String id;
	private final int weight;
	private final int objectiveMultiplier;
	private final int experienceReward;
	private final int levelsXpReward;
	private final int essenceReward;
	private final String tierLabel;

	QuestDifficulty(
		String id,
		int weight,
		int objectiveMultiplier,
		int experienceReward,
		int levelsXpReward,
		int essenceReward,
		String tierLabel
	) {
		this.id = id;
		this.weight = weight;
		this.objectiveMultiplier = objectiveMultiplier;
		this.experienceReward = experienceReward;
		this.levelsXpReward = levelsXpReward;
		this.essenceReward = essenceReward;
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

	public int experienceReward() {
		return experienceReward;
	}

	public int levelsXpReward() {
		return levelsXpReward;
	}

	public int essenceReward() {
		return essenceReward;
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
