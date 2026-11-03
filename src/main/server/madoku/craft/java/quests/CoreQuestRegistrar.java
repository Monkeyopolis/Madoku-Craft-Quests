package madoku.craft.java.quests;

import java.util.List;

/** Registers the built-in Main Quests definitions. */
public final class CoreQuestRegistrar implements QuestCategoryRegistrar {
	public static final String LUMBERJACK = "lumberjack";

	@Override
	public String categoryId() {
		return QuestCategoryIds.CORE;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of(new QuestDefinition(
			LUMBERJACK,
			"menu.madoku-craft.quests.lumberjack",
			"menu.madoku-craft.quests.lumberjack.description",
			QuestCategoryIds.CORE,
			List.of(
				QuestObjective.breaks(QuestTarget.tag("minecraft:logs"), 48),
				QuestObjective.collects(QuestTarget.tag("minecraft:logs"), 48)
			)
		));
	}
}
