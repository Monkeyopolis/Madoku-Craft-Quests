package madoku.craft.mixin.quests;

import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tracks successful axe conversions from logs to stripped logs for quests. */
@Mixin(ItemStack.class)
public abstract class ItemStackQuestStripMixin {
	@Unique
	private static final TagKey<Block> MADOKU_LOGS = TagKey.create(
		Registries.BLOCK,
		Identifier.fromNamespaceAndPath("minecraft", "logs")
	);
	@Unique
	private BlockState madokuCraft$beforeStripState;

	@Inject(method = "useOn", at = @At("HEAD"))
	private void madokuCraft$captureStripSource(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		madokuCraft$beforeStripState = null;
		if (context == null || !(context.getLevel() instanceof ServerLevel)) return;
		madokuCraft$beforeStripState = context.getLevel().getBlockState(context.getClickedPos());
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void madokuCraft$trackStrip(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState before = madokuCraft$beforeStripState;
		madokuCraft$beforeStripState = null;
		if (before == null || context == null || cir.getReturnValue() == null || !cir.getReturnValue().consumesAction()) return;
		if (!(context.getLevel() instanceof ServerLevel level)
			|| !(context.getPlayer() instanceof ServerPlayer serverPlayer)
			|| serverPlayer.isCreative()) return;

		BlockPos pos = context.getClickedPos();
		BlockState after = level.getBlockState(pos);
		if (before.equals(after) || !before.is(MADOKU_LOGS) || !madokuCraft$isStrippedLog(after)) return;
		QuestsStateBridge.onBlockStripped(serverPlayer, level, pos, before);
	}

	@Unique
	private static boolean madokuCraft$isStrippedLog(BlockState state) {
		Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return blockId != null && blockId.getPath().startsWith("stripped_");
	}
}
