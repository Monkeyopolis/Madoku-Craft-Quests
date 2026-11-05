package madoku.craft.java.quests;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;

/** Narrow bridge used by the inventory mixin without exposing the state manager. */
public final class QuestsStateBridge {
	private static final ThreadLocal<Boolean> SHIFT_RESULT_TRANSFER = new ThreadLocal<>();

	private QuestsStateBridge() {
	}

	public static void beginShiftResultTransfer() {
		SHIFT_RESULT_TRANSFER.set(Boolean.TRUE);
	}

	public static boolean isShiftResultTransferActive() {
		return Boolean.TRUE.equals(SHIFT_RESULT_TRANSFER.get());
	}

	public static void endShiftResultTransfer() {
		SHIFT_RESULT_TRANSFER.remove();
	}

	public static void onItemCollected(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemCollected(player, item, amount);
	}

	public static void onItemCrafted(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemCrafted(player, item, amount);
	}

	public static void onItemEnchanted(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemEnchanted(player, item, amount);
	}

	public static void onItemSmelted(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemSmelted(player, item, amount);
	}

	public static void onItemUsed(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemUsed(player, item, amount);
	}

	public static void onBlockStripped(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		QuestsStateManager.onBlockStripped(player, level, pos, state);
	}
}
