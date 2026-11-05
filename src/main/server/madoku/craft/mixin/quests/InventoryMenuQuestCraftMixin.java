package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts player-inventory crafting performed through shift-click transfers. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuQuestCraftMixin {
	@Unique
	private ItemStack madokuCraft$shiftCraftedOutput = ItemStack.EMPTY;

	@Inject(method = "quickMoveStack", at = @At("HEAD"))
	private void madokuCraft$captureShiftCraft(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		madokuCraft$shiftCraftedOutput = ItemStack.EMPTY;
		if (!(player instanceof ServerPlayer)) return;
		Slot slot = ((AbstractContainerMenu) (Object) this).getSlot(slotIndex);
		if (!(slot instanceof ResultSlot) || !slot.hasItem()) return;
		madokuCraft$shiftCraftedOutput = slot.getItem().copy();
		QuestsStateBridge.beginShiftResultTransfer();
	}

	@Inject(method = "quickMoveStack", at = @At("RETURN"))
	private void madokuCraft$trackShiftCraft(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack output = madokuCraft$shiftCraftedOutput;
		madokuCraft$shiftCraftedOutput = ItemStack.EMPTY;
		if (!(player instanceof ServerPlayer serverPlayer)) return;
		try {
			if (!output.isEmpty() && cir.getReturnValue() != null && !cir.getReturnValue().isEmpty()) {
				QuestsStateBridge.onItemCrafted(serverPlayer, output.getItem(), output.getCount());
			}
		} finally {
			QuestsStateBridge.endShiftResultTransfer();
		}
	}
}
