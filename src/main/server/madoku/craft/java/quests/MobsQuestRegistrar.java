package madoku.craft.java.quests;

import java.util.List;

/** Registration point for Mob Quests definitions. */
public final class MobsQuestRegistrar implements QuestCategoryRegistrar {
	@Override
	public String categoryId() {
		return QuestCategoryIds.MOBS;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of();
	}
}
