package madoku.craft.java.quests;

import java.util.Locale;

/** A registry identifier or tag used by a quest objective. */
public record QuestTarget(Kind kind, String id) {
	public QuestTarget {
		if (kind == null) throw new IllegalArgumentException("Quest target kind must not be null.");
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Quest target id must not be blank.");
		id = id.trim().toLowerCase(Locale.ROOT);
	}

	public static QuestTarget id(String id) {
		return new QuestTarget(Kind.ID, id);
	}

	public static QuestTarget tag(String id) {
		return new QuestTarget(Kind.TAG, id);
	}

	public enum Kind {
		ID,
		TAG;

		public static Kind fromId(String id) {
			if (id == null) return null;
			return switch (id.trim().toLowerCase(Locale.ROOT)) {
				case "id" -> ID;
				case "tag" -> TAG;
				default -> null;
			};
		}
	}
}
