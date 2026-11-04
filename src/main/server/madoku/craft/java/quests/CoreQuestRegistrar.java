package madoku.craft.java.quests;

import java.util.List;

/** Registers the built-in Main Quests definitions. */
public final class CoreQuestRegistrar implements QuestCategoryRegistrar {
	public static final String LUMBERJACK = "lumberjack";
	public static final String STONEWORKS = "stoneworks";
	public static final String COAL_MINER = "coal-miner";

	@Override
	public String categoryId() {
		return QuestCategoryIds.CORE;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of(
			new QuestDefinition(
				LUMBERJACK,
				"menu.madoku-craft.quests.lumberjack",
				"menu.madoku-craft.quests.lumberjack.description",
				QuestCategoryIds.CORE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.tag("minecraft:logs"),
						48,
						"menu.madoku-craft.quests.lumberjack.objective.break"
					),
					QuestObjective.collects(
						QuestTarget.tag("minecraft:logs"),
						48,
						"menu.madoku-craft.quests.lumberjack.objective.collect"
					)
				)
			),
			new QuestDefinition(
				STONEWORKS,
				"menu.madoku-craft.quests.stoneworks",
				"menu.madoku-craft.quests.stoneworks.description",
				QuestCategoryIds.CORE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:stone", "minecraft:cobblestone"),
						56,
						"menu.madoku-craft.quests.stoneworks.objective.break"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:cobblestone"),
						56,
						"menu.madoku-craft.quests.stoneworks.objective.collect"
					)
				)
			),
			new QuestDefinition(
				COAL_MINER,
				"menu.madoku-craft.quests.coal-miner",
				"menu.madoku-craft.quests.coal-miner.description",
				QuestCategoryIds.CORE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
						24,
						"menu.madoku-craft.quests.coal-miner.objective.break"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:coal"),
						24,
						"menu.madoku-craft.quests.coal-miner.objective.collect"
					)
				)
			)
		);
	}
}
