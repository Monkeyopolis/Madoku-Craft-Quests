package madoku.craft.java.quests;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/** Narrow bridge used by the inventory mixin without exposing the state manager. */
public final class QuestsStateBridge {
	private QuestsStateBridge() {
	}

	public static void onItemCollected(ServerPlayer player, Item item, int amount) {
		QuestsStateManager.onItemCollected(player, item, amount);
	}
}
