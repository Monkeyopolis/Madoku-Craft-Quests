package madoku.craft.java.quests;

import java.util.Locale;

/** Standard quest rarities and their additive reward multipliers. */
public enum QuestRarity {
	COMMON("common", 30, 1.0D),
	RARE("rare", 25, 1.25D),
	EPIC("epic", 20, 1.5D),
	LEGENDARY("legendary", 15, 1.75D),
	MYTHIC("mythic", 10, 2.0D);

	private final String id;
	private final int selectionWeight;
	private final double rewardMultiplier;

	QuestRarity(String id, int selectionWeight, double rewardMultiplier) {
		this.id = id;
		this.selectionWeight = selectionWeight;
		this.rewardMultiplier = rewardMultiplier;
	}

	public String id() {
		return id;
	}

	public int selectionWeight() {
		return selectionWeight;
	}

	public double rewardMultiplier() {
		return rewardMultiplier;
	}

	public static QuestRarity fromId(String id) {
		if (id == null) return null;
		String normalized = id.trim().toLowerCase(Locale.ROOT);
		for (QuestRarity rarity : values()) {
			if (rarity.id.equals(normalized)) return rarity;
		}
		return null;
	}
}
