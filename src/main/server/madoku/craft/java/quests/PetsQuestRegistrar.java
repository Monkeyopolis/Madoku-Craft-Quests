package madoku.craft.java.quests;

import java.util.List;

/** Registration point for Pet Quests definitions. */
public final class PetsQuestRegistrar implements QuestCategoryRegistrar {
	@Override
	public String categoryId() {
		return QuestCategoryIds.PETS;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of();
	}
}
