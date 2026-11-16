package madoku.craft.java.quests;

import java.util.List;

/** Registration point for Mob Quests definitions. */
public final class MobsQuestRegistrar implements QuestCategoryRegistrar {
	public static final String KILLING_SKELETONS = "killing-skeletons";
	public static final String KILLING_STRAYS = "killing-strays";
	public static final String TYPES_OF_SKELETONS = "types-of-skeletons";
	public static final String KILLING_ZOMBIES = "killing-zombies";
	public static final String KILLING_CREEPERS = "killing-creepers";
	public static final String KILLING_SPIDERS = "killing-spiders";
	public static final String KILLING_ENDERMEN = "killing-endermen";
	public static final String KILLING_DROWNED = "killing-drowned";
	public static final String THATS_A_STRAY = "thats-a-stray";
	public static final String STRANGE_SKELETON = "strange-skeleton";
	public static final String THATS_A_ZOMBIE = "thats-a-zombie";
	public static final String CREEPER_ON_FIRE = "creeper-on-fire";
	public static final String SPIDER_FAMILY = "spider-family";
	public static final String STRANGE_ENDERMAN = "strange-enderman";
	public static final String ELITE_MOB_KILLER = "elite-mob-killer";

	@Override
	public String categoryId() {
		return QuestCategoryIds.MOBS;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of(
			new QuestDefinition(
				KILLING_SKELETONS,
				"menu.madoku-craft.quests.killing-skeletons",
				"menu.madoku-craft.quests.killing-skeletons.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:skeleton"),
						4,
						"menu.madoku-craft.quests.killing-skeletons.objective.kill"
					)
				)
			),
			new QuestDefinition(
				KILLING_STRAYS,
				"menu.madoku-craft.quests.killing-strays",
				"menu.madoku-craft.quests.killing-strays.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:stray"),
					4,
					"menu.madoku-craft.quests.killing-strays.objective.kill"
				))
			),
			new QuestDefinition(
				TYPES_OF_SKELETONS,
				"menu.madoku-craft.quests.types-of-skeletons",
				"menu.madoku-craft.quests.types-of-skeletons.description",
				QuestCategoryIds.MOBS,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:stray"),
						4,
						"menu.madoku-craft.quests.types-of-skeletons.objective.kill-strays"
					),
					QuestObjective.kills(
						QuestTarget.id("minecraft:bogged"),
						4,
						"menu.madoku-craft.quests.types-of-skeletons.objective.kill-bogged"
					),
					QuestObjective.kills(
						QuestTarget.id("minecraft:parched"),
						4,
						"menu.madoku-craft.quests.types-of-skeletons.objective.kill-parched"
					)
				)
			),
			new QuestDefinition(
				KILLING_ZOMBIES,
				"menu.madoku-craft.quests.killing-zombies",
				"menu.madoku-craft.quests.killing-zombies.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:zombie"),
					4,
					"menu.madoku-craft.quests.killing-zombies.objective.kill"
				))
			),
			new QuestDefinition(
				KILLING_CREEPERS,
				"menu.madoku-craft.quests.killing-creepers",
				"menu.madoku-craft.quests.killing-creepers.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:creeper"),
					4,
					"menu.madoku-craft.quests.killing-creepers.objective.kill"
				))
			),
			new QuestDefinition(
				KILLING_SPIDERS,
				"menu.madoku-craft.quests.killing-spiders",
				"menu.madoku-craft.quests.killing-spiders.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:spider"),
					4,
					"menu.madoku-craft.quests.killing-spiders.objective.kill"
				))
			),
			new QuestDefinition(
				KILLING_ENDERMEN,
				"menu.madoku-craft.quests.killing-endermen",
				"menu.madoku-craft.quests.killing-endermen.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:enderman"),
					2,
					"menu.madoku-craft.quests.killing-endermen.objective.kill"
				))
			),
			new QuestDefinition(
				KILLING_DROWNED,
				"menu.madoku-craft.quests.killing-drowned",
				"menu.madoku-craft.quests.killing-drowned.description",
				QuestCategoryIds.MOBS,
				QuestRarity.COMMON,
				List.of(QuestObjective.kills(
					QuestTarget.id("minecraft:drowned"),
					4,
					"menu.madoku-craft.quests.killing-drowned.objective.kill"
				))
			),
			new QuestDefinition(
				THATS_A_STRAY,
				"menu.madoku-craft.quests.thats-a-stray",
				"menu.madoku-craft.quests.thats-a-stray.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:stray"),
						4,
						"menu.madoku-craft.quests.thats-a-stray.objective.kill-strays"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:stray",
							"madoku-craft:mob/minecraft/stray/frosted-stray"
						),
						1,
						"menu.madoku-craft.quests.thats-a-stray.objective.kill-frozen-stray"
					)
				)
			),
			new QuestDefinition(
				STRANGE_SKELETON,
				"menu.madoku-craft.quests.strange-skeleton",
				"menu.madoku-craft.quests.strange-skeleton.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:skeleton"),
						4,
						"menu.madoku-craft.quests.strange-skeleton.objective.kill-skeletons"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:skeleton",
							"madoku-craft:mob/minecraft/skeleton/red-skeleton"
						),
						1,
						"menu.madoku-craft.quests.strange-skeleton.objective.kill-red-skeleton"
					)
				)
			),
			new QuestDefinition(
				THATS_A_ZOMBIE,
				"menu.madoku-craft.quests.thats-a-zombie",
				"menu.madoku-craft.quests.thats-a-zombie.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:zombie"),
						4,
						"menu.madoku-craft.quests.thats-a-zombie.objective.kill-zombies"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:zombie",
							"madoku-craft:mob/minecraft/zombie/leader-zombie"
						),
						1,
						"menu.madoku-craft.quests.thats-a-zombie.objective.kill-leader-zombie"
					)
				)
			),
			new QuestDefinition(
				CREEPER_ON_FIRE,
				"menu.madoku-craft.quests.creeper-on-fire",
				"menu.madoku-craft.quests.creeper-on-fire.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:creeper"),
						4,
						"menu.madoku-craft.quests.creeper-on-fire.objective.kill-creepers"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:creeper",
							"madoku-craft:mob/minecraft/creeper/fire-creeper"
						),
						1,
						"menu.madoku-craft.quests.creeper-on-fire.objective.kill-fire-creeper"
					)
				)
			),
			new QuestDefinition(
				SPIDER_FAMILY,
				"menu.madoku-craft.quests.spider-family",
				"menu.madoku-craft.quests.spider-family.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:spider"),
						4,
						"menu.madoku-craft.quests.spider-family.objective.kill-spiders"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:spider",
							"madoku-craft:mob/minecraft/spider/albino-spider"
						),
						1,
						"menu.madoku-craft.quests.spider-family.objective.kill-albino-spider"
					)
				)
			),
			new QuestDefinition(
				ELITE_MOB_KILLER,
				"menu.madoku-craft.quests.elite-mob-killer",
				"menu.madoku-craft.quests.elite-mob-killer.description",
				QuestCategoryIds.MOBS,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:zombie",
							"madoku-craft:mob/minecraft/zombie/leader-zombie"
						),
						1,
						"menu.madoku-craft.quests.elite-mob-killer.objective.kill-leader-zombie"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:creeper",
							"madoku-craft:mob/minecraft/creeper/fire-creeper"
						),
						1,
						"menu.madoku-craft.quests.elite-mob-killer.objective.kill-fire-creeper"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:spider",
							"madoku-craft:mob/minecraft/spider/albino-spider"
						),
						1,
						"menu.madoku-craft.quests.elite-mob-killer.objective.kill-albino-spider"
					)
				)
			),
			new QuestDefinition(
				STRANGE_ENDERMAN,
				"menu.madoku-craft.quests.strange-enderman",
				"menu.madoku-craft.quests.strange-enderman.description",
				QuestCategoryIds.MOBS,
				QuestRarity.RARE,
				List.of(
					QuestObjective.kills(
						QuestTarget.id("minecraft:enderman"),
						4,
						"menu.madoku-craft.quests.strange-enderman.objective.kill-endermen"
					),
					QuestObjective.kills(
						QuestTarget.entityVariant(
							"minecraft:enderman",
							"madoku-craft:mob/minecraft/enderman/corrupted-enderman"
						),
						1,
						"menu.madoku-craft.quests.strange-enderman.objective.kill-corrupted-enderman"
					)
				)
			)
		);
	}
}
