package madoku.craft.java.quests;

import java.util.List;

/** Registration point for Farming Quests definitions. */
public final class FarmingQuestRegistrar implements QuestCategoryRegistrar {
	@Override
	public String categoryId() {
		return QuestCategoryIds.FARMING;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of();
	}
}
