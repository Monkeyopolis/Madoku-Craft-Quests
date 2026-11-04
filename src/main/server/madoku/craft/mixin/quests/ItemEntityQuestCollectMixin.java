package madoku.craft.mixin.quests;

import madoku.craft.java.core.item.ItemEntityDropAPIManager;
import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Tracks item collection only after an item has successfully entered a player inventory. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityQuestCollectMixin {
	@Inject(
		method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;onItemPickup(Lnet/minecraft/world/entity/item/ItemEntity;)V",
			shift = At.Shift.AFTER
		),
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void madokuCraft$trackQuestCollection(
		Player player,
		CallbackInfo ci,
		ItemStack pickedUpStack,
		Item pickedUpItem,
		int pickedUpAmount
	) {
		ItemEntity itemEntity = (ItemEntity) (Object) this;
		if (player instanceof ServerPlayer serverPlayer) {
			int excludedAmount = ItemEntityDropAPIManager.consumePlayerAttributedAmount(itemEntity, pickedUpAmount);
			int collectedAmount = pickedUpAmount - excludedAmount;
			if (collectedAmount > 0) {
				QuestsStateBridge.onItemCollected(serverPlayer, pickedUpItem, collectedAmount);
			}
		}
	}
}
