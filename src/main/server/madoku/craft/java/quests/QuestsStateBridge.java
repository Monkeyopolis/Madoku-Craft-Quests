package madoku.craft.java.quests;

import net.minecraft.server.level.ServerPlayer;

/** Narrow bridge used by the inventory mixin without exposing the state manager. */
public final class QuestsStateBridge {
	private QuestsStateBridge() {
	}

	public static void onInventoryChanged(ServerPlayer player) {
		QuestsStateManager.onInventoryChanged(player);
	}
}
