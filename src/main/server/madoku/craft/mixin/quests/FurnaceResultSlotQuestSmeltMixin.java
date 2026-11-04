package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts output taken by a player from furnace-type blocks. */
@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotQuestSmeltMixin {
	@Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
	private void madokuCraft$trackSmeltedOutput(Player player, ItemStack stack, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer) || stack == null || stack.isEmpty()) return;
		FurnaceResultSlot resultSlot = (FurnaceResultSlot) (Object) this;
		Container container = resultSlot.container;
		if (!(container instanceof AbstractFurnaceBlockEntity)) return;
		QuestsStateBridge.onItemSmelted(serverPlayer, stack.getItem(), stack.getCount());
	}
}
