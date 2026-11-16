package madoku.craft.java.quests;

import madoku.craft.java.core.module.MadokuModuleIds;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.Identifier;

/** Registration point for quests that bridge multiple optional modules. */
public final class CompatQuestRegistrar implements QuestCategoryRegistrar {
	private static final Set<Identifier> FARMING_AND_ITEMS = Set.of(
		MadokuModuleIds.FARMING,
		MadokuModuleIds.ITEMS
	);
	private static final Set<Identifier> ITEMS_ONLY = Set.of(MadokuModuleIds.ITEMS);

	public static final String MAKING_GLOWING_PIES = "making-glowing-pies";
	public static final String COOKING_BEETROOT_SOUP = "cooking-beetroot-soup";
	public static final String BAKING_SWEET_BERRY_COOKIES = "baking-sweet-berry-cookies";
	public static final String FISHING_FOR_COOKED_TROPICAL_FISH = "fishing-for-cooked-tropical-fish";

	@Override
	public String categoryId() {
		return QuestCategoryIds.COMPAT;
	}

	@Override
	public List<QuestDefinition> definitions() {
		return List.of(
			new QuestDefinition(
				FarmingQuestRegistrar.CUTTING_MELONS,
				"menu.madoku-craft.quests.cutting-melons",
				"menu.madoku-craft.quests.cutting-melons.description",
				QuestCategoryIds.COMPAT,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:melon"),
						16,
						"menu.madoku-craft.quests.cutting-melons.objective.harvest"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:bowl"),
						16,
						"menu.madoku-craft.quests.cutting-melons.objective.bowls"
					),
					QuestObjective.crafts(
						QuestTarget.id("madoku-craft:bowl-of-melon"),
						16,
						"menu.madoku-craft.quests.cutting-melons.objective.bowl-of-melon"
					)
				),
				FARMING_AND_ITEMS
			),
			new QuestDefinition(
				MAKING_GLOWING_PIES,
				"menu.madoku-craft.quests.making-glowing-pies",
				"menu.madoku-craft.quests.making-glowing-pies.description",
				QuestCategoryIds.COMPAT,
				QuestRarity.MYTHIC,
				List.of(
					QuestObjective.collects(
						QuestTarget.id("minecraft:sugar_cane"),
						24,
						"menu.madoku-craft.quests.making-glowing-pies.objective.collect-sugar-cane"
					),
					QuestObjective.harvests(
						QuestTarget.id("minecraft:wheat"),
						16,
						"menu.madoku-craft.quests.making-glowing-pies.objective.harvest-wheat"
					),
					QuestObjective.crafts(
						QuestTarget.id("madoku-craft:glowberry-pie"),
						12,
						"menu.madoku-craft.quests.making-glowing-pies.objective.crafting"
					)
				),
				FARMING_AND_ITEMS
			),
			new QuestDefinition(
				COOKING_BEETROOT_SOUP,
				"menu.madoku-craft.quests.cooking-beetroot-soup",
				"menu.madoku-craft.quests.cooking-beetroot-soup.description",
				QuestCategoryIds.COMPAT,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:beetroot"),
						16,
						"menu.madoku-craft.quests.cooking-beetroot-soup.objective.harvest"
					),
					QuestObjective.crafts(
						QuestTarget.id("minecraft:beetroot_soup"),
						16,
						"menu.madoku-craft.quests.cooking-beetroot-soup.objective.crafting"
					)
				),
				FARMING_AND_ITEMS
			),
			new QuestDefinition(
				BAKING_SWEET_BERRY_COOKIES,
				"menu.madoku-craft.quests.baking-sweet-berry-cookies",
				"menu.madoku-craft.quests.baking-sweet-berry-cookies.description",
				QuestCategoryIds.COMPAT,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.harvests(
						QuestTarget.id("minecraft:wheat"),
						16,
						"menu.madoku-craft.quests.baking-sweet-berry-cookies.objective.harvest-wheat"
					),
					QuestObjective.harvests(
						QuestTarget.id("minecraft:sweet_berries"),
						16,
						"menu.madoku-craft.quests.baking-sweet-berry-cookies.objective.harvest-sweet-berries"
					),
					QuestObjective.crafts(
						QuestTarget.id("madoku-craft:sweet-berry-cookie"),
						16,
						"menu.madoku-craft.quests.baking-sweet-berry-cookies.objective.crafting"
					)
				),
				FARMING_AND_ITEMS
			),
			new QuestDefinition(
				FISHING_FOR_COOKED_TROPICAL_FISH,
				"menu.madoku-craft.quests.fishing-for-cooked-tropical-fish",
				"menu.madoku-craft.quests.fishing-for-cooked-tropical-fish.description",
				QuestCategoryIds.COMPAT,
				QuestRarity.LEGENDARY,
				List.of(
					QuestObjective.collects(
						QuestTarget.anyOf("minecraft:charcoal", "minecraft:coal"),
						8,
						"menu.madoku-craft.quests.fishing-for-cooked-tropical-fish.objective.collect-fuel"
					),
					QuestObjective.collects(
						QuestTarget.id("minecraft:tropical_fish"),
						8,
						"menu.madoku-craft.quests.fishing-for-cooked-tropical-fish.objective.collect-fish"
					),
					QuestObjective.smelts(
						QuestTarget.id("madoku-craft:cooked-tropical-fish"),
						8,
						"menu.madoku-craft.quests.fishing-for-cooked-tropical-fish.objective.smelt-fish"
					)
				),
				ITEMS_ONLY
			)
		);
	}
}
