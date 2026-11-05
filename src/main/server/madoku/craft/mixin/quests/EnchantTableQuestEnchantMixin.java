package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts each successful custom enchantment operation, including bulk shift-click operations. */
@Mixin(targets = "madoku.craft.java.core.enchant.EnchantTableAPIManager")
public abstract class EnchantTableQuestEnchantMixin {
	@Unique
	private static final ThreadLocal<Item> MADOKU_CRAFT$OUTPUT_ITEM = new ThreadLocal<>();

	@Inject(
		method = "craftOne(Lnet/minecraft/world/inventory/EnchantmentMenu;Lnet/minecraft/world/entity/player/Player;IZ)Z",
		at = @At("HEAD")
	)
	private static void madokuCraft$captureEnchantOutput(
		EnchantmentMenu menu,
		Player player,
		int option,
		boolean bulk,
		CallbackInfoReturnable<Boolean> cir
	) {
		MADOKU_CRAFT$OUTPUT_ITEM.set(madokuCraft$outputItem(menu.getSlot(0).getItem()));
	}

	@Inject(
		method = "craftOne(Lnet/minecraft/world/inventory/EnchantmentMenu;Lnet/minecraft/world/entity/player/Player;IZ)Z",
		at = @At("RETURN")
	)
	private static void madokuCraft$trackEnchant(
		EnchantmentMenu menu,
		Player player,
		int option,
		boolean bulk,
		CallbackInfoReturnable<Boolean> cir
	) {
		Item output = MADOKU_CRAFT$OUTPUT_ITEM.get();
		MADOKU_CRAFT$OUTPUT_ITEM.remove();
		if (!(player instanceof ServerPlayer serverPlayer) || !cir.getReturnValueZ() || output == null) return;
		QuestsStateBridge.onItemEnchanted(serverPlayer, output, 1);
	}

	@Unique
	private static Item madokuCraft$outputItem(ItemStack input) {
		if (input == null || input.isEmpty()) return null;
		if (input.is(Items.BOOK) || input.is(Items.ENCHANTED_BOOK)) return Items.ENCHANTED_BOOK;
		if (input.is(Items.GLASS_BOTTLE)) return Items.EXPERIENCE_BOTTLE;
		return input.getItem();
	}
}
