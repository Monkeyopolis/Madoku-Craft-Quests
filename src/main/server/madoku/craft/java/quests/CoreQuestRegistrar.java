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
	public static final String THESE_ARE_STONES = "these-are-stones";
	public static final String MASONRY = "masonry";
	public static final String LET_THERE_BE_LIGHT = "let-there-be-light";
	public static final String INFINITE_DIRT = "infinite-dirt";
	public static final String CHAINMAIL_ARMOR = "chainmail-armor";
	public static final String ENCHANTING_BOTTLES = "enchanting-bottles";
	public static final String MAKING_FIRE = "making-fire";

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
						96,
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
						128,
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
						192,
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
						32,
						"menu.madoku-craft.quests.collecting-flint.objective.collect"
					)
				)
			),
			new QuestDefinition(
				COAL_MINER,
				"menu.madoku-craft.quests.coal-miner",
				"menu.madoku-craft.quests.coal-miner.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
						64,
						"menu.madoku-craft.quests.coal-miner.objective.break"
					)
				)
			),
			new QuestDefinition(
				DIAMONDS,
				"menu.madoku-craft.quests.diamonds",
				"menu.madoku-craft.quests.diamonds.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
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
				QuestRarity.COMMON,
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
				QuestRarity.COMMON,
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
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:gold_ore", "minecraft:deepslate_gold_ore"),
						16,
						"menu.madoku-craft.quests.shiny-gold.objective.break"
					)
				)
			),
			new QuestDefinition(
				STURDY_IRON,
				"menu.madoku-craft.quests.sturdy-iron",
				"menu.madoku-craft.quests.sturdy-iron.description",
				QuestCategoryIds.CORE,
				QuestRarity.COMMON,
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
				QuestRarity.COMMON,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
						96,
						"menu.madoku-craft.quests.copper-specialist.objective.break"
					)
				)
			),
			new QuestDefinition(
				STONE_VARIETY,
				"menu.madoku-craft.quests.stone-variety",
				"menu.madoku-craft.quests.stone-variety.description",
				QuestCategoryIds.CORE,
				QuestRarity.EPIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.id("minecraft:diorite"),
						48
					),
					QuestObjective.breaks(
						QuestTarget.id("minecraft:granite"),
						48
					),
					QuestObjective.breaks(
						QuestTarget.id("minecraft:andesite"),
						48
					)
				)
			),
			new QuestDefinition(
				THESE_ARE_STONES,
				"menu.madoku-craft.quests.these-are-stones",
				"menu.madoku-craft.quests.these-are-stones.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.breaks(
						QuestTarget.id("minecraft:deepslate"),
						96
					),
					QuestObjective.breaks(
						QuestTarget.id("minecraft:tuff"),
						32
					)
				)
			),
			new QuestDefinition(
				MASONRY,
				"menu.madoku-craft.quests.masonry",
				"menu.madoku-craft.quests.masonry.description",
				QuestCategoryIds.CORE,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.collects(
						QuestTarget.id("minecraft:clay_ball"),
						64
					),
					QuestObjective.smelts(
						QuestTarget.id("minecraft:brick"),
						64
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:bricks"),
						64
					)
				)
			),
			new QuestDefinition(
				LET_THERE_BE_LIGHT,
				"menu.madoku-craft.quests.let-there-be-light",
				"menu.madoku-craft.quests.let-there-be-light.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.crafts(
						QuestTarget.id("minecraft:stick"),
						32
					),
					QuestObjective.smelts(
						QuestTarget.id("minecraft:charcoal"),
						32
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:torch"),
						128
					)
				)
			),
			new QuestDefinition(
				INFINITE_DIRT,
				"menu.madoku-craft.quests.infinite-dirt",
				"menu.madoku-craft.quests.infinite-dirt.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.breaks(
						QuestTarget.id("minecraft:dirt"),
						64
					),
					QuestObjective.breaks(
						QuestTarget.id("minecraft:gravel"),
						64
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:coarse_dirt"),
						128
					)
				)
			),
			new QuestDefinition(
				CHAINMAIL_ARMOR,
				"menu.madoku-craft.quests.chainmail-armor",
				"menu.madoku-craft.quests.chainmail-armor.description",
				QuestCategoryIds.CORE,
				QuestRarity.RARE,
				List.of(
					QuestObjective.crafts(
						QuestTarget.id("minecraft:iron_nugget"),
						32
					),
					QuestObjective.crafts(
						QuestTarget.anyOf(
							"minecraft:chainmail_helmet",
							"minecraft:chainmail_chestplate",
							"minecraft:chainmail_leggings",
							"minecraft:chainmail_boots"
						),
						4
					)
				)
			),
			new QuestDefinition(
				ENCHANTING_BOTTLES,
				"menu.madoku-craft.quests.enchanting-bottles",
				"menu.madoku-craft.quests.enchanting-bottles.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.crafts(
						QuestTarget.id("minecraft:glass_bottle"),
						6
					),
					QuestObjective.enchants(
						QuestTarget.id("minecraft:experience_bottle"),
						6
					)
				)
			),
			new QuestDefinition(
				GLASS_MAKER,
				"menu.madoku-craft.quests.glass-maker",
				"menu.madoku-craft.quests.glass-maker.description",
				QuestCategoryIds.CORE,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf("minecraft:sand", "minecraft:red_sand"),
						64,
						"menu.madoku-craft.quests.glass-maker.objective.break"
					),
					QuestObjective.smelts(
						QuestTarget.id("minecraft:glass"),
						64,
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
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.breaks(
						QuestTarget.anyOf(
							"minecraft:iron_ore",
							"minecraft:deepslate_iron_ore",
							"minecraft:gold_ore",
							"minecraft:deepslate_gold_ore"
						),
						64,
						"menu.madoku-craft.quests.ore-smelter.objective.break"
					),
					QuestObjective.smelts(
						QuestTarget.anyOf(
							"minecraft:iron_ingot",
							"minecraft:gold_ingot"
						),
						64,
						"menu.madoku-craft.quests.ore-smelter.objective.smelt"
					)
				)
			),
			new QuestDefinition(
				MAKING_FIRE,
				"menu.madoku-craft.quests.making-fire",
				"menu.madoku-craft.quests.making-fire.description",
				QuestCategoryIds.CORE,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.breaks(
						QuestTarget.tag("minecraft:logs"),
						64,
						"menu.madoku-craft.quests.making-fire.objective.break"
					),
					QuestObjective.strips(
						QuestTarget.tag("minecraft:logs"),
						64,
						"menu.madoku-craft.quests.making-fire.objective.strip"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:campfire"),
						16,
						"menu.madoku-craft.quests.making-fire.objective.crafting"
					)
				)
			)
		);
	}
}
