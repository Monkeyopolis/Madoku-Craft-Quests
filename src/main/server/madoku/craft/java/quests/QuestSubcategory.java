package madoku.craft.java.quests;

import java.util.Locale;

/** Standard quest objective types. Additional objective types can be added here later. */
public enum QuestSubcategory {
	BREAK("break", 1.0D),
	COLLECT("collect", 1.0D),
	CRAFTING("crafting", 1.25D),
	ENCHANT("enchant", 1.25D),
	SMELT("smelt", 1.5D),
	STRIP("strip", 1.25D),
	USE("use", 1.25D);

	private final String id;
	private final double rewardMultiplier;

	QuestSubcategory(String id, double rewardMultiplier) {
		this.id = id;
		this.rewardMultiplier = rewardMultiplier;
	}

	public String id() {
		return id;
	}

	public double rewardMultiplier() {
		return rewardMultiplier;
	}

	public static QuestSubcategory fromId(String id) {
		if (id == null) return null;
		String normalized = id.trim().toLowerCase(Locale.ROOT);
		for (QuestSubcategory subcategory : values()) {
			if (subcategory.id.equals(normalized)) return subcategory;
		}
		return null;
	}
}
