package madoku.craft.mixin.quests;

import madoku.craft.java.core.enchant.EnchantConfigAPIManager;
import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts vanilla enchantment-table operations when the Core custom table is disabled. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuQuestEnchantMixin {
	@Unique
	private Item madokuCraft$vanillaInputItem;

	@Inject(method = "clickMenuButton", at = @At("HEAD"))
	private void madokuCraft$captureVanillaInput(Player player, int button, CallbackInfoReturnable<Boolean> cir) {
		ItemStack input = ((EnchantmentMenu) (Object) this).getSlot(0).getItem();
		madokuCraft$vanillaInputItem = input == null || input.isEmpty() ? null : input.getItem();
	}

	@Inject(method = "clickMenuButton", at = @At("RETURN"))
	private void madokuCraft$trackVanillaEnchant(Player player, int button, CallbackInfoReturnable<Boolean> cir) {
		Item output = madokuCraft$vanillaInputItem;
		madokuCraft$vanillaInputItem = null;
		if (!(player instanceof ServerPlayer serverPlayer)
			|| EnchantConfigAPIManager.isEnchantmentTableEnabled()
			|| !cir.getReturnValueZ()
			|| output == null) return;
		QuestsStateBridge.onItemEnchanted(serverPlayer, output, 1);
	}
}
