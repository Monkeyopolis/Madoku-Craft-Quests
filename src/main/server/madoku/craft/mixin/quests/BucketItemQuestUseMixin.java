package madoku.craft.mixin.quests;

import madoku.craft.java.core.data.ChunkDataAPIManager;
import madoku.craft.java.quests.QuestsStateBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Tracks successful natural lava pickups made with a bucket. */
@Mixin(BucketItem.class)
public abstract class BucketItemQuestUseMixin {
	@Redirect(
		method = "use",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack madokuCraft$trackLavaPickup(
		BucketPickup bucketPickup,
		LivingEntity entity,
		LevelAccessor level,
		BlockPos pos,
		BlockState state
	) {
		ItemStack result = bucketPickup.pickupBlock(entity, level, pos, state);
		ServerLevel serverLevel = level instanceof ServerLevel value ? value : null;
		ServerPlayer serverPlayer = entity instanceof ServerPlayer value ? value : null;
		boolean isLava = state != null && state.getFluidState().is(FluidTags.LAVA);
		boolean isSource = state != null && state.getFluidState().isSource();
		boolean isPlayerPlaced = serverLevel != null && ChunkDataAPIManager.isPlayerPlacedBlock(serverLevel, pos);
		if (result.isEmpty()
			|| serverLevel == null
			|| serverPlayer == null
			|| serverPlayer.isCreative()
			|| !isLava
			|| !isSource) {
			return result;
		}

		if (isPlayerPlaced) {
			ChunkDataAPIManager.removePlayerPlacedBlock(serverLevel, pos);
			return result;
		}

		QuestsStateBridge.onItemUsed(serverPlayer, result.getItem(), result.getCount());
		return result;
	}
}
