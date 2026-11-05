package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts items crafted through the player inventory or crafting table result slot. */
@Mixin(ResultSlot.class)
public abstract class ResultSlotQuestCraftMixin {
	@Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
	private void madokuCraft$trackCraftedOutput(Player player, ItemStack stack, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer)
			|| QuestsStateBridge.isShiftResultTransferActive()
			|| !(serverPlayer.containerMenu instanceof AbstractCraftingMenu)
			|| stack == null
			|| stack.isEmpty()) return;
		QuestsStateBridge.onItemCrafted(serverPlayer, stack.getItem(), stack.getCount());
	}
}
