package madoku.craft.java.quests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** A registry identifier or tag used by a quest objective. */
public record QuestTarget(Kind kind, String id, List<String> ids) {
	public QuestTarget {
		if (kind == null) throw new IllegalArgumentException("Quest target kind must not be null.");
		List<String> normalizedIds = new ArrayList<>();
		if (ids != null) {
			for (String value : ids) {
				if (value != null && !value.isBlank()) {
					normalizedIds.add(value.trim().toLowerCase(Locale.ROOT));
				}
			}
		}
		if (normalizedIds.isEmpty() && id != null && !id.isBlank()) {
			normalizedIds.add(id.trim().toLowerCase(Locale.ROOT));
		}
		if (normalizedIds.isEmpty()) throw new IllegalArgumentException("Quest target id must not be blank.");
		id = normalizedIds.get(0);
		ids = List.copyOf(normalizedIds);
	}

	public QuestTarget(Kind kind, String id) {
		this(kind, id, List.of(id));
	}

	public static QuestTarget id(String id) {
		return new QuestTarget(Kind.ID, id);
	}

	public static QuestTarget tag(String id) {
		return new QuestTarget(Kind.TAG, id);
	}

	public static QuestTarget anyOf(String... ids) {
		return new QuestTarget(Kind.ANY_OF, null, ids == null ? List.of() : Arrays.asList(ids));
	}

	public static QuestTarget anyOf(List<String> ids) {
		return new QuestTarget(Kind.ANY_OF, null, ids);
	}

	public enum Kind {
		ID,
		TAG,
		ANY_OF;

		public static Kind fromId(String id) {
			if (id == null) return null;
			return switch (id.trim().toLowerCase(Locale.ROOT)) {
				case "id" -> ID;
				case "tag" -> TAG;
				case "any-of", "any_of" -> ANY_OF;
				default -> null;
			};
		}
	}
}
