package madoku.craft.java.quests;

import java.util.Locale;

/** Standard quest objective types. Additional objective types can be added here later. */
public enum QuestSubcategory {
	BREAK("break"),
	COLLECT("collect");

	private final String id;

	QuestSubcategory(String id) {
		this.id = id;
	}

	public String id() {
		return id;
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
