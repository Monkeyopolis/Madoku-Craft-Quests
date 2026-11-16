package madoku.craft.java.quests;

import java.util.List;

/** Registration point for Farming Quests definitions. */
public final class FarmingQuestRegistrar implements QuestCategoryRegistrar {
	public static final String GROWING_POTATOES = "growing-potatoes";
	public static final String GROWING_WHEAT = "growing-wheat";
	public static final String GROWING_SWEET_BERRIES = "growing-sweet-berries";
	public static final String GROWING_PUMPKINS = "growing-pumpkins";
	public static final String GROWING_MELONS = "growing-melons";
	public static final String CUTTING_MELONS = "cutting-melons";
	public static final String PUMPKIN_PIES = "pumpkin-pies";
	public static final String GROWING_BEETROOT = "growing-beetroot";
	public static final String BOUNTIFUL_HARVESTS = "bountiful-harvests";
	public static final String GROWING_CARROTS = "growing-carrots";
	public static final String GOLDEN_CARROTS = "golden-carrots";
	public static final String ARE_THOSE_STEMS = "are-those-stems";
	public static final String THESE_ARE_CROPS = "these-are-crops";
	public static final String MAKING_BREAD = "making-bread";
	public static final String BAKING_COOKIES = "baking-cookies";
	public static final String BAKING_POTATOES = "baking-potatoes";
	public static final String GROW_FASTER = "grow-faster";
	public static final String GROWING_SUGAR_CANE = "growing-sugar-cane";
	public static final String GROWING_GLOW_BERRIES = "growing-glow-berries";

	@Override
	public String categoryId() {
		return QuestCategoryIds.FARMING;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of(
			new QuestDefinition(
				GROWING_POTATOES,
				"menu.madoku-craft.quests.growing-potatoes",
				"menu.madoku-craft.quests.growing-potatoes.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:potato"),
						40,
						"menu.madoku-craft.quests.growing-potatoes.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				GROWING_WHEAT,
				"menu.madoku-craft.quests.growing-wheat",
				"menu.madoku-craft.quests.growing-wheat.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:wheat"),
						32,
						"menu.madoku-craft.quests.growing-wheat.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				GROWING_CARROTS,
				"menu.madoku-craft.quests.growing-carrots",
				"menu.madoku-craft.quests.growing-carrots.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:carrot"),
						40,
						"menu.madoku-craft.quests.growing-carrots.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				GOLDEN_CARROTS,
				"menu.madoku-craft.quests.golden-carrots",
				"menu.madoku-craft.quests.golden-carrots.description",
				QuestCategoryIds.FARMING,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:carrot"),
						32,
						"menu.madoku-craft.quests.golden-carrots.objective.harvest"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:golden_carrot"),
						8,
						"menu.madoku-craft.quests.golden-carrots.objective.crafting"
					)
				)
			),
			new QuestDefinition(
				GROWING_SWEET_BERRIES,
				"menu.madoku-craft.quests.growing-sweet-berries",
				"menu.madoku-craft.quests.growing-sweet-berries.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:sweet_berries"),
						32,
						"menu.madoku-craft.quests.growing-sweet-berries.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				ARE_THOSE_STEMS,
				"menu.madoku-craft.quests.are-those-stems",
				"menu.madoku-craft.quests.are-those-stems.description",
				QuestCategoryIds.FARMING,
				QuestRarity.RARE,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:pumpkin"),
						12,
						"menu.madoku-craft.quests.are-those-stems.objective.harvest-pumpkins"
					),
					QuestObjective.harvests(
						QuestTarget.id("minecraft:melon"),
						12,
						"menu.madoku-craft.quests.are-those-stems.objective.harvest-melons"
					)
				)
			),
			new QuestDefinition(
				THESE_ARE_CROPS,
				"menu.madoku-craft.quests.these-are-crops",
				"menu.madoku-craft.quests.these-are-crops.description",
				QuestCategoryIds.FARMING,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:sweet_berries"),
						24,
						"menu.madoku-craft.quests.these-are-crops.objective.harvest-sweet-berries"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:sugar_cane"),
						48,
						"menu.madoku-craft.quests.these-are-crops.objective.collect-sugar-cane"
					)
				)
			),
			new QuestDefinition(
				GROWING_PUMPKINS,
				"menu.madoku-craft.quests.growing-pumpkins",
				"menu.madoku-craft.quests.growing-pumpkins.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:pumpkin"),
						16,
						"menu.madoku-craft.quests.growing-pumpkins.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				GROWING_MELONS,
				"menu.madoku-craft.quests.growing-melons",
				"menu.madoku-craft.quests.growing-melons.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:melon"),
						24,
						"menu.madoku-craft.quests.growing-melons.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				BOUNTIFUL_HARVESTS,
				"menu.madoku-craft.quests.bountiful-harvests",
				"menu.madoku-craft.quests.bountiful-harvests.description",
				QuestCategoryIds.FARMING,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:beetroot"),
						24,
						"menu.madoku-craft.quests.bountiful-harvests.objective.harvest-beetroot"
					),
					QuestObjective.harvests(
						QuestTarget.id("minecraft:carrot"),
						24,
						"menu.madoku-craft.quests.bountiful-harvests.objective.harvest-carrots"
					),
					QuestObjective.harvests(
						QuestTarget.id("minecraft:potato"),
						24,
						"menu.madoku-craft.quests.bountiful-harvests.objective.harvest-potatoes"
					)
				)
			),
			new QuestDefinition(
				PUMPKIN_PIES,
				"menu.madoku-craft.quests.pumpkin-pies",
				"menu.madoku-craft.quests.pumpkin-pies.description",
				QuestCategoryIds.FARMING,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:pumpkin"),
						12,
						"menu.madoku-craft.quests.pumpkin-pies.objective.harvest"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:sugar_cane"),
						24,
						"menu.madoku-craft.quests.pumpkin-pies.objective.collect"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:pumpkin_pie"),
						12,
						"menu.madoku-craft.quests.pumpkin-pies.objective.crafting"
					)
				)
			),
			new QuestDefinition(
				GROWING_BEETROOT,
				"menu.madoku-craft.quests.growing-beetroot",
				"menu.madoku-craft.quests.growing-beetroot.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:beetroot"),
						40,
						"menu.madoku-craft.quests.growing-beetroot.objective.harvest"
					)
				)
			),
			new QuestDefinition(
				MAKING_BREAD,
				"menu.madoku-craft.quests.making-bread",
				"menu.madoku-craft.quests.making-bread.description",
				QuestCategoryIds.FARMING,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:wheat_seeds"),
						32,
						"menu.madoku-craft.quests.making-bread.objective.harvest"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:bread"),
						32,
						"menu.madoku-craft.quests.making-bread.objective.crafting"
					)
				)
			),
			new QuestDefinition(
				BAKING_COOKIES,
				"menu.madoku-craft.quests.baking-cookies",
				"menu.madoku-craft.quests.baking-cookies.description",
				QuestCategoryIds.FARMING,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:wheat"),
						16,
						"menu.madoku-craft.quests.baking-cookies.objective.harvest-wheat"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:cocoa_beans"),
						32,
						"menu.madoku-craft.quests.baking-cookies.objective.collect-cocoa-beans"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:cookie"),
						24,
						"menu.madoku-craft.quests.baking-cookies.objective.crafting"
					)
				)
			),
			new QuestDefinition(
				BAKING_POTATOES,
				"menu.madoku-craft.quests.baking-potatoes",
				"menu.madoku-craft.quests.baking-potatoes.description",
				QuestCategoryIds.FARMING,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:potato"),
						24,
						"menu.madoku-craft.quests.baking-potatoes.objective.harvest"
					),
					QuestObjective.smelts(
						QuestTarget.id("minecraft:baked_potato"),
						24,
						"menu.madoku-craft.quests.baking-potatoes.objective.smelt"
					)
				)
			),
			new QuestDefinition(
				GROW_FASTER,
				"menu.madoku-craft.quests.grow-faster",
				"menu.madoku-craft.quests.grow-faster.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.uses(
						QuestTarget.id("minecraft:bone_meal"),
						32,
						"menu.madoku-craft.quests.grow-faster.objective.use"
					)
				)
			),
			new QuestDefinition(
				GROWING_SUGAR_CANE,
				"menu.madoku-craft.quests.growing-sugar-cane",
				"menu.madoku-craft.quests.growing-sugar-cane.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.collects(
						QuestTarget.id("minecraft:sugar_cane"),
						48,
						"menu.madoku-craft.quests.growing-sugar-cane.objective.collect"
					)
				)
			),
			new QuestDefinition(
				GROWING_GLOW_BERRIES,
				"menu.madoku-craft.quests.growing-glow-berries",
				"menu.madoku-craft.quests.growing-glow-berries.description",
				QuestCategoryIds.FARMING,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.collects(
						QuestTarget.id("minecraft:glow_berries"),
						32,
						"menu.madoku-craft.quests.growing-glow-berries.objective.collect"
					)
				)
			)
		);
	}
}
