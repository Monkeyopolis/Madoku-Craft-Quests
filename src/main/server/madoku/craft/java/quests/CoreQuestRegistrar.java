package madoku.craft.java.quests;

import java.util.List;

/** Registers the built-in Main Quests definitions. */
public final class CoreQuestRegistrar implements QuestCategoryRegistrar {
	public static final String LUMBERJACK = "lumberjack";
	public static final String STONEWORKS = "stoneworks";
	public static final String DIRT_COLLECTOR = "dirt-collector";
	public static final String COLLECTING_FLINT = "collecting-flint";
	public static final String COAL_MINER = "coal-miner";
	public static final String DIAMONDS = "diamonds";
	public static final String OBSIDIAN = "obsidian";
	public static final String FLAMING_HOT = "flaming-hot";
	public static final String GLASS_MAKER = "glass-maker";
	public static final String SHINY_GOLD = "shiny-gold";
	public static final String STURDY_IRON = "sturdy-iron";
	public static final String COPPER_SPECIALIST = "copper-specialist";
	public static final String ORE_SMELTER = "ore-smelter";
	public static final String STONE_VARIETY = "stone-variety";
	public static final String PROCESSING_LOGS = "processing-logs";

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
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.tag("minecraft:logs"),
						48,
						"menu.madoku-craft.quests.lumberjack.objective.break"
					)
				)
			),
			new QuestDefinition(
				STONEWORKS,
				"menu.madoku-craft.quests.stoneworks",
				"menu.madoku-craft.quests.stoneworks.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:stone", "minecraft:cobblestone"),
						64,
						"menu.madoku-craft.quests.stoneworks.objective.break"
					)
				)
			),
			new QuestDefinition(
				DIRT_COLLECTOR,
				"menu.madoku-craft.quests.dirt-collector",
				"menu.madoku-craft.quests.dirt-collector.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf(
							"minecraft:podzol",
							"minecraft:dirt",
							"minecraft:grass_block",
							"minecraft:mycelium",
							"minecraft:dirt_path"
						),
						72,
						"menu.madoku-craft.quests.dirt-collector.objective.break"
					)
				)
			),
			new QuestDefinition(
				COLLECTING_FLINT,
				"menu.madoku-craft.quests.collecting-flint",
				"menu.madoku-craft.quests.collecting-flint.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.collects(
						QuestTarget.id("minecraft:flint"),
						12,
						"menu.madoku-craft.quests.collecting-flint.objective.collect"
					)
				)
			),
			new QuestDefinition(
				COAL_MINER,
				"menu.madoku-craft.quests.coal-miner",
				"menu.madoku-craft.quests.coal-miner.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
						24,
						"menu.madoku-craft.quests.coal-miner.objective.break"
					)
				)
			),
			new QuestDefinition(
				DIAMONDS,
				"menu.madoku-craft.quests.diamonds",
				"menu.madoku-craft.quests.diamonds.description",
				QuestCategoryIds.CORE,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
						2,
						"menu.madoku-craft.quests.diamonds.objective.break"
					)
				)
			),
			new QuestDefinition(
				OBSIDIAN,
				"menu.madoku-craft.quests.obsidian",
				"menu.madoku-craft.quests.obsidian.description",
				QuestCategoryIds.CORE,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.id("minecraft:obsidian"),
						4,
						"menu.madoku-craft.quests.obsidian.objective.break"
					)
				)
			),
			new QuestDefinition(
				FLAMING_HOT,
				"menu.madoku-craft.quests.flaming-hot",
				"menu.madoku-craft.quests.flaming-hot.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.uses(
						QuestTarget.id("minecraft:lava_bucket"),
						1,
						"menu.madoku-craft.quests.flaming-hot.objective.use"
					)
				)
			),
			new QuestDefinition(
				SHINY_GOLD,
				"menu.madoku-craft.quests.shiny-gold",
				"menu.madoku-craft.quests.shiny-gold.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:gold_ore", "minecraft:deepslate_gold_ore"),
						8,
						"menu.madoku-craft.quests.shiny-gold.objective.break"
					)
				)
			),
			new QuestDefinition(
				STURDY_IRON,
				"menu.madoku-craft.quests.sturdy-iron",
				"menu.madoku-craft.quests.sturdy-iron.description",
				QuestCategoryIds.CORE,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
						16,
						"menu.madoku-craft.quests.sturdy-iron.objective.break"
					)
				)
			),
			new QuestDefinition(
				COPPER_SPECIALIST,
				"menu.madoku-craft.quests.copper-specialist",
				"menu.madoku-craft.quests.copper-specialist.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
						48,
						"menu.madoku-craft.quests.copper-specialist.objective.break"
					)
				)
			),
			new QuestDefinition(
				STONE_VARIETY,
				"menu.madoku-craft.quests.stone-variety",
				"menu.madoku-craft.quests.stone-variety.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf(
							"minecraft:granite",
							"minecraft:diorite",
							"minecraft:andesite"
						),
						40,
						"menu.madoku-craft.quests.stone-variety.objective.break"
					)
				)
			),
			new QuestDefinition(
				GLASS_MAKER,
				"menu.madoku-craft.quests.glass-maker",
				"menu.madoku-craft.quests.glass-maker.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:sand", "minecraft:red_sand"),
						24,
						"menu.madoku-craft.quests.glass-maker.objective.break"
					),
					QuestObjective.smelts(
						QuestTarget.id("minecraft:glass"),
						24,
						"menu.madoku-craft.quests.glass-maker.objective.smelt",
						List.of("minecraft:sand", "minecraft:red_sand")
					)
				)
			),
			new QuestDefinition(
				ORE_SMELTER,
				"menu.madoku-craft.quests.ore-smelter",
				"menu.madoku-craft.quests.ore-smelter.description",
				QuestCategoryIds.CORE,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf(
							"minecraft:copper_ore",
							"minecraft:deepslate_copper_ore",
							"minecraft:iron_ore",
							"minecraft:deepslate_iron_ore",
							"minecraft:gold_ore",
							"minecraft:deepslate_gold_ore"
						),
						32,
						"menu.madoku-craft.quests.ore-smelter.objective.break"
					),
					QuestObjective.smelts(
						QuestTarget.anyOf(
							"minecraft:copper_ingot",
							"minecraft:iron_ingot",
							"minecraft:gold_ingot"
						),
						32,
						"menu.madoku-craft.quests.ore-smelter.objective.smelt"
					)
				)
			),
			new QuestDefinition(
				PROCESSING_LOGS,
				"menu.madoku-craft.quests.processing-logs",
				"menu.madoku-craft.quests.processing-logs.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.tag("minecraft:logs"),
						24,
						"menu.madoku-craft.quests.processing-logs.objective.break"
					),
					QuestObjective.strips(
						QuestTarget.tag("minecraft:logs"),
						24,
						"menu.madoku-craft.quests.processing-logs.objective.strip"
					)
				)
			)
		);
	}
}
