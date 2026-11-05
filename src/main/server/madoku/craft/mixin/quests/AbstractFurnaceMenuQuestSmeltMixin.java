package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts furnace, smoker, and blast-furnace output taken through shift-click transfers. */
@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuQuestSmeltMixin {
	@Unique
	private ItemStack madokuCraft$shiftSmeltedOutput = ItemStack.EMPTY;

	@Inject(method = "quickMoveStack", at = @At("HEAD"))
	private void madokuCraft$captureShiftSmelt(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		madokuCraft$shiftSmeltedOutput = ItemStack.EMPTY;
		if (!(player instanceof ServerPlayer)) return;
		Slot slot = ((AbstractContainerMenu) (Object) this).getSlot(slotIndex);
		if (!(slot instanceof FurnaceResultSlot) || !slot.hasItem()) return;
		madokuCraft$shiftSmeltedOutput = slot.getItem().copy();
		QuestsStateBridge.beginShiftResultTransfer();
	}

	@Inject(method = "quickMoveStack", at = @At("RETURN"))
	private void madokuCraft$trackShiftSmelt(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack output = madokuCraft$shiftSmeltedOutput;
		madokuCraft$shiftSmeltedOutput = ItemStack.EMPTY;
		if (!(player instanceof ServerPlayer serverPlayer)) return;
		try {
			if (!output.isEmpty() && cir.getReturnValue() != null && !cir.getReturnValue().isEmpty()) {
				QuestsStateBridge.onItemSmelted(serverPlayer, output.getItem(), output.getCount());
			}
		} finally {
			QuestsStateBridge.endShiftResultTransfer();
		}
	}
}
