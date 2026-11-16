package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tracks successful standard item uses so use quests do not need item-specific mixins. */
@Mixin(ItemStack.class)
public abstract class ItemStackQuestUseMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void madokuCraft$trackBlockUse(
		UseOnContext context,
		CallbackInfoReturnable<InteractionResult> callback
	) {
		if (context == null || callback.getReturnValue() == null || !callback.getReturnValue().consumesAction()) return;
		if (!(context.getLevel() instanceof ServerLevel)
			|| !(context.getPlayer() instanceof ServerPlayer serverPlayer)) return;

		QuestsStateBridge.onItemUsed(serverPlayer, context.getItemInHand().getItem(), 1);
	}

	@Inject(method = "use", at = @At("RETURN"))
	private void madokuCraft$trackHandUse(
		Level level,
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callback
	) {
		ItemStack usedStack = (ItemStack) (Object) this;
		if (!(level instanceof ServerLevel)
			|| !(player instanceof ServerPlayer serverPlayer)
			|| callback.getReturnValue() == null
			|| !callback.getReturnValue().consumesAction()) return;

		QuestsStateBridge.onItemUsed(serverPlayer, usedStack.getItem(), 1);
	}
}
